package at.modulento.app.data

import at.modulento.app.FakeSessionStore
import at.modulento.app.Samples
import at.modulento.app.data.api.ApiClient
import at.modulento.app.data.api.ApiException
import at.modulento.app.ui.UiError
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private val server = MockWebServer()
    private lateinit var store: FakeSessionStore
    private lateinit var repository: ModulentoRepository

    @Before
    fun setUp() {
        server.start()
        store = FakeSessionStore(serverUrl = server.url("/").toString().trimEnd('/'))
        repository = ModulentoRepository(store, ApiClient())
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .body(body)
                .build(),
        )
    }

    private suspend fun failure(block: suspend () -> Unit): ApiException {
        try {
            block()
        } catch (e: ApiException) {
            return e
        }
        fail("expected an ApiException")
        throw AssertionError()
    }

    @Test
    fun `login posts the form fields and stores the token unchanged`() = runTest {
        enqueue(200, Samples.LOGIN)

        val account = repository.login(" admin@example.test ", "pass word&+1", "Pixel 9")

        assertEquals("admin@example.test", account.email)
        assertNull(account.displayName)
        assertEquals("sel.secret", store.token)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/login", request.url.encodedPath)
        assertNull(request.headers["Authorization"])
        assertEquals(
            "email=admin%40example.test&password=pass+word%26%2B1&device_name=Pixel+9",
            request.body?.utf8(),
        )
    }

    @Test
    fun `login errors are told apart by code`() = runTest {
        enqueue(401, Samples.error("invalid_credentials"))
        enqueue(429, Samples.error("too_many_attempts"))
        enqueue(403, Samples.error("email_unverified"))

        val errors = List(3) { failure { repository.login("a@example.test", "x", null) } }

        assertEquals(
            listOf(UiError.InvalidCredentials, UiError.TooManyAttempts, UiError.EmailUnverified),
            errors.map(UiError::from),
        )
        assertEquals(listOf(401, 429, 403), errors.map { (it as ApiException.Http).status })
        assertTrue(errors.none { it.isAuthFailure && (it as ApiException.Http).status != 401 })
        assertNull(store.token)
    }

    @Test
    fun `meta sends the bearer token and parses the admin answer`() = runTest {
        store.token = "sel.secret"
        enqueue(200, Samples.META_ADMIN)

        val meta = repository.loadMeta()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/meta", request.url.encodedPath)
        assertEquals("Bearer sel.secret", request.headers["Authorization"])
        assertTrue(meta.hasAdminMenu)
        assertEquals("/admin/pages", meta.adminMenu.single().items.single().path)
        assertEquals(3, meta.notices.single().count)
        assertEquals("Modulento", meta.siteName)
        assertEquals("#d4245a", meta.accent)
        assertEquals("1.2.3", meta.coreVersion)
    }

    @Test
    fun `a customer account has no admin menu`() = runTest {
        store.token = "sel.secret"
        enqueue(200, Samples.META_CUSTOMER)

        val meta = repository.loadMeta()

        assertFalse(meta.hasAdminMenu)
        assertEquals("Kim", meta.account.displayName)
    }

    @Test
    fun `a rejected token is an auth failure whether sent as 401 or 403`() = runTest {
        store.token = "sel.secret"
        enqueue(403, Samples.error("unauthenticated", "Kein Zugriff"))
        enqueue(401, Samples.error("unauthenticated"))

        assertTrue(failure { repository.loadMeta() }.isAuthFailure)
        assertTrue(failure { repository.loadMeta() }.isAuthFailure)
    }

    @Test
    fun `handoff posts the path and returns the url`() = runTest {
        store.token = "sel.secret"
        enqueue(200, Samples.HANDOFF)

        val url = repository.webviewHandoff("/admin/shop/settings")

        assertEquals("http://127.0.0.1:8098/webview/session?code=abc", url)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/webview-handoff", request.url.encodedPath)
        assertEquals("Bearer sel.secret", request.headers["Authorization"])
        assertEquals("path=%2Fadmin%2Fshop%2Fsettings", request.body?.utf8())
    }

    @Test
    fun `handoff 503 is reported as unavailable and keeps the session`() = runTest {
        store.token = "sel.secret"
        enqueue(503, Samples.error("unavailable"))

        val error = failure { repository.webviewHandoff("/account") }

        assertEquals(UiError.HandoffUnavailable, UiError.from(error))
        assertFalse(error.isAuthFailure)
        assertEquals("sel.secret", store.token)
    }

    @Test
    fun `handoff refuses a url that is not http`() = runTest {
        store.token = "sel.secret"
        enqueue(200, """{"data":{"code":"abc","url":"javascript:alert(1)","expires_in":60}}""")

        assertTrue(failure { repository.webviewHandoff("/account") } is ApiException.InvalidResponse)
    }

    @Test
    fun `logout revokes on the server and forgets the token even if that fails`() = runTest {
        store.token = "sel.secret"
        enqueue(400, Samples.error("invalid_token"))

        val closed = repository.closeSession()
        assertNull(store.token)
        assertFalse(repository.isLoggedIn)
        repository.revoke(closed!!)

        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.url.encodedPath)
        assertEquals("Bearer sel.secret", request.headers["Authorization"])
    }

    @Test
    fun `changing the server drops the token`() {
        store.token = "sel.secret"

        repository.setServerUrl(store.serverUrl!!)
        assertEquals("sel.secret", store.token)

        repository.setServerUrl("https://other.example")
        assertNull(store.token)
    }

    @Test
    fun `a server url with a sub-directory keeps its path`() = runTest {
        store.serverUrl = store.serverUrl + "/shop"
        store.token = "sel.secret"
        enqueue(200, Samples.META_CUSTOMER)

        repository.loadMeta()

        assertEquals("/shop/api/v1/meta", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `non-json and unexpected answers do not crash`() = runTest {
        store.token = "sel.secret"
        enqueue(200, "<html>Not an API</html>")
        enqueue(500, "<html>Internal Server Error</html>")
        enqueue(404, "<html>Not Found</html>")
        enqueue(200, "{}")

        assertEquals(UiError.InvalidResponse, UiError.from(failure { repository.loadMeta() }))
        val serverError = failure { repository.loadMeta() } as ApiException.Http
        assertEquals("http_500", serverError.code)
        assertEquals(UiError.Server, UiError.from(serverError))
        assertEquals(UiError.InvalidResponse, UiError.from(failure { repository.loadMeta() }))
        assertEquals(UiError.InvalidResponse, UiError.from(failure { repository.loadMeta() }))
    }

    @Test
    fun `a dropped connection is a network error`() = runTest {
        store.token = "sel.secret"
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        assertEquals(UiError.Network, UiError.from(failure { repository.loadMeta() }))
    }
}
