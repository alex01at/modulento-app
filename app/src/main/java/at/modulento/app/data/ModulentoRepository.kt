package at.modulento.app.data

import at.modulento.app.data.api.Account
import at.modulento.app.data.api.ApiClient
import at.modulento.app.data.api.ApiException
import at.modulento.app.data.api.Meta

/** Single entry point for the ViewModels: session state plus the API calls. */
class ModulentoRepository(
    private val store: SessionStore,
    private val client: ApiClient = ApiClient(),
) {

    val serverUrl: String? get() = store.serverUrl
    val isLoggedIn: Boolean get() = store.serverUrl != null && store.token != null

    /** Switching servers invalidates the token: it belongs to the old server. */
    fun setServerUrl(url: String) {
        if (url != store.serverUrl) store.token = null
        store.serverUrl = url
    }

    suspend fun login(email: String, password: String, deviceName: String?): Account {
        val url = requireServerUrl()
        val data = client.call {
            client.api(url).login(email.trim(), password, deviceName?.trim()?.takeIf { it.isNotEmpty() })
        }
        if (data.token.isBlank()) throw ApiException.InvalidResponse()
        store.token = data.token
        return data.account
    }

    suspend fun loadMeta(): Meta {
        val (url, token) = requireSession()
        return client.call { client.api(url).meta(ApiClient.bearer(token)) }
    }

    /** Returns the one-time URL that signs the WebView in and forwards to [path]. */
    suspend fun webviewHandoff(path: String?): String {
        val (url, token) = requireSession()
        val data = client.call { client.api(url).webviewHandoff(ApiClient.bearer(token), path) }
        if (!data.url.startsWith("https://") && !data.url.startsWith("http://")) {
            throw ApiException.InvalidResponse()
        }
        return data.url
    }

    /**
     * Forgets the token locally, right away. The returned handle lets the caller
     * revoke it on the server afterwards without blocking the UI on the network.
     */
    fun closeSession(): ClosedSession? {
        val url = store.serverUrl
        val token = store.token
        store.token = null
        return if (url != null && token != null) ClosedSession(url, token) else null
    }

    /** Best effort: a failure here must never keep the user logged in locally. */
    suspend fun revoke(session: ClosedSession) {
        try {
            client.call { client.api(session.serverUrl).logout(ApiClient.bearer(session.token)) }
        } catch (_: ApiException) {
        }
    }

    private fun requireServerUrl(): String =
        store.serverUrl ?: throw ApiException.InvalidResponse(IllegalStateException("no server url"))

    private fun requireSession(): Pair<String, String> {
        val url = requireServerUrl()
        val token = store.token ?: throw ApiException.Http(401, "unauthenticated", "")
        return url to token
    }

    class ClosedSession internal constructor(val serverUrl: String, val token: String)
}
