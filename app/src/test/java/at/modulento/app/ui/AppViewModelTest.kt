package at.modulento.app.ui

import at.modulento.app.FakeSessionStore
import at.modulento.app.Samples
import at.modulento.app.data.ModulentoRepository
import at.modulento.app.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val server = MockWebServer()
    private lateinit var store: FakeSessionStore
    private var webSessionsCleared = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server.start()
        store = FakeSessionStore(serverUrl = server.url("/").toString().trimEnd('/'), token = "sel.secret")
    }

    @After
    fun tearDown() {
        server.close()
        Dispatchers.resetMain()
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun viewModel() =
        AppViewModel(ModulentoRepository(store, ApiClient())) { webSessionsCleared++ }

    private fun AppViewModel.awaitState(predicate: (AppState) -> Boolean): AppState = runBlocking {
        withTimeout(5_000) { state.first(predicate) }
    }

    @Test
    fun `a stored session loads meta on start`() {
        enqueue(200, Samples.META_ADMIN)

        val state = viewModel().awaitState { it.meta is MetaState.Loaded }

        assertTrue(state.loggedIn)
        assertTrue((state.meta as MetaState.Loaded).meta.hasAdminMenu)
    }

    @Test
    fun `a rejected token ends the session and explains it on the login screen`() {
        enqueue(403, Samples.error("unauthenticated"))

        val state = viewModel().awaitState { !it.loggedIn }

        assertTrue(state.sessionExpired)
        assertNull(store.token)
        assertEquals(1, webSessionsCleared)
        assertEquals("no logout call for a token the server already rejected", 1, server.requestCount)
    }

    @Test
    fun `a server that is merely unreachable keeps the session for a retry`() {
        enqueue(500, "oops")
        enqueue(200, Samples.META_CUSTOMER)

        val viewModel = viewModel()
        val failed = viewModel.awaitState { it.meta is MetaState.Failed }
        assertTrue(failed.loggedIn)
        assertEquals("sel.secret", store.token)

        viewModel.refreshMeta()
        val loaded = viewModel.awaitState { it.meta is MetaState.Loaded }
        assertFalse((loaded.meta as MetaState.Loaded).meta.hasAdminMenu)
    }

    @Test
    fun `logout is immediate locally and revokes the token on the server`() {
        enqueue(200, Samples.META_ADMIN)
        enqueue(200, Samples.OK)
        val viewModel = viewModel()
        viewModel.awaitState { it.meta is MetaState.Loaded }
        server.takeRequest()

        viewModel.logout()

        assertFalse(viewModel.state.value.loggedIn)
        assertFalse(viewModel.state.value.sessionExpired)
        assertNull(store.token)
        assertEquals(1, webSessionsCleared)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.url.encodedPath)
        assertEquals("Bearer sel.secret", request.headers["Authorization"])
    }

    @Test
    fun `an invalid server address changes nothing, a new one logs out`() {
        enqueue(200, Samples.META_ADMIN)
        enqueue(200, Samples.OK)
        val viewModel = viewModel()
        viewModel.awaitState { it.meta is MetaState.Loaded }

        assertFalse(viewModel.saveServerUrl("not a url"))
        assertTrue(viewModel.state.value.loggedIn)

        assertTrue(viewModel.saveServerUrl("other.example"))
        assertEquals("https://other.example", viewModel.state.value.serverUrl)
        assertEquals("https://other.example", store.serverUrl)
        assertFalse(viewModel.state.value.loggedIn)
        assertNull(store.token)
    }
}
