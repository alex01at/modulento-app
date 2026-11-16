package at.modulento.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.modulento.app.data.ModulentoRepository
import at.modulento.app.data.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HandoffState {
    data object Loading : HandoffState
    data class Ready(val url: String) : HandoffState
    data class Failed(val error: UiError) : HandoffState
    /** The token was rejected; the caller ends the session. */
    data object SessionRejected : HandoffState
}

/** Requests a one-time WebView login for one local site [path]. */
class WebHandoffViewModel(
    private val repository: ModulentoRepository,
    private val path: String,
) : ViewModel() {

    private val _state = MutableStateFlow<HandoffState>(HandoffState.Loading)
    val state: StateFlow<HandoffState> = _state.asStateFlow()

    init {
        request()
    }

    fun request() {
        _state.value = HandoffState.Loading
        viewModelScope.launch {
            _state.value = try {
                HandoffState.Ready(repository.webviewHandoff(path))
            } catch (e: ApiException) {
                if (e.isAuthFailure) HandoffState.SessionRejected else HandoffState.Failed(UiError.from(e))
            }
        }
    }
}
