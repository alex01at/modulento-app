package at.modulento.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.modulento.app.data.ModulentoRepository
import at.modulento.app.data.ServerUrl
import at.modulento.app.data.api.ApiException
import at.modulento.app.data.api.Meta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MetaState {
    data object Loading : MetaState
    data class Loaded(val meta: Meta) : MetaState
    data class Failed(val error: UiError) : MetaState
}

data class AppState(
    val serverUrl: String?,
    val loggedIn: Boolean,
    val meta: MetaState = MetaState.Loading,
    /** Set when the server rejected the stored token; the login screen explains it once. */
    val sessionExpired: Boolean = false,
)

/**
 * App-wide session state: which server, logged in or not, and the cached
 * `/api/v1/meta` answer that drives navigation, dashboard and theming.
 *
 * [clearWebSession] drops the WebView cookies, so that the website session
 * ends together with the API session.
 */
class AppViewModel(
    private val repository: ModulentoRepository,
    private val clearWebSession: () -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppState(serverUrl = repository.serverUrl, loggedIn = repository.isLoggedIn),
    )
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var metaJob: Job? = null

    init {
        if (repository.isLoggedIn) refreshMeta()
    }

    /** Returns false (and changes nothing) if [input] is not a usable address. */
    fun saveServerUrl(input: String): Boolean {
        val url = ServerUrl.normalize(input) ?: return false
        if (url == repository.serverUrl) return true
        endSession()
        repository.setServerUrl(url)
        _state.value = AppState(serverUrl = url, loggedIn = false)
        return true
    }

    fun onLoggedIn() {
        _state.update { it.copy(loggedIn = true, sessionExpired = false) }
        refreshMeta()
    }

    fun refreshMeta() {
        metaJob?.cancel()
        _state.update { it.copy(meta = MetaState.Loading) }
        metaJob = viewModelScope.launch {
            try {
                val meta = repository.loadMeta()
                _state.update { it.copy(meta = MetaState.Loaded(meta)) }
            } catch (e: ApiException) {
                if (e.isAuthFailure) {
                    onSessionRejected()
                } else {
                    _state.update { it.copy(meta = MetaState.Failed(UiError.from(e))) }
                }
            }
        }
    }

    fun logout() {
        endSession()
        _state.update { AppState(serverUrl = it.serverUrl, loggedIn = false) }
    }

    /** The server no longer accepts the token (revoked, expired, account removed). */
    fun onSessionRejected() {
        if (!_state.value.loggedIn) return
        endSession(revokeOnServer = false)
        _state.update { AppState(serverUrl = it.serverUrl, loggedIn = false, sessionExpired = true) }
    }

    fun onSessionExpiredShown() {
        _state.update { it.copy(sessionExpired = false) }
    }

    private fun endSession(revokeOnServer: Boolean = true) {
        metaJob?.cancel()
        val closed = repository.closeSession()
        clearWebSession()
        if (revokeOnServer && closed != null) {
            viewModelScope.launch { repository.revoke(closed) }
        }
    }
}
