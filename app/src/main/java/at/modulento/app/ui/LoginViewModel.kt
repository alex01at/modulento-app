package at.modulento.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.modulento.app.data.ModulentoRepository
import at.modulento.app.data.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val busy: Boolean = false,
    val error: UiError? = null,
    val done: Boolean = false,
)

class LoginViewModel(private val repository: ModulentoRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun submit(email: String, password: String, deviceName: String) {
        if (_state.value.busy) return
        _state.value = LoginState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                repository.login(email, password, deviceName)
                LoginState(done = true)
            } catch (e: ApiException) {
                LoginState(error = UiError.from(e))
            }
        }
    }
}
