package at.modulento.app.ui

import androidx.annotation.StringRes
import at.modulento.app.R
import at.modulento.app.data.api.ApiException

/** What went wrong, in terms the UI can explain. */
enum class UiError(@StringRes val text: Int) {
    InvalidCredentials(R.string.error_invalid_credentials),
    TooManyAttempts(R.string.error_too_many_attempts),
    EmailUnverified(R.string.error_email_unverified),
    HandoffUnavailable(R.string.error_handoff_unavailable),
    Network(R.string.error_network),
    InvalidResponse(R.string.error_invalid_response),
    Server(R.string.error_server);

    companion object {
        fun from(e: ApiException): UiError = when (e) {
            is ApiException.Network -> Network
            is ApiException.InvalidResponse -> InvalidResponse
            is ApiException.Http -> when {
                e.code == "invalid_credentials" -> InvalidCredentials
                e.code == "too_many_attempts" || e.status == 429 -> TooManyAttempts
                e.code == "email_unverified" -> EmailUnverified
                e.code == "unavailable" || e.status == 503 -> HandoffUnavailable
                // A 404 on an API path means: this is not a (current) Modulento server.
                e.status == 404 -> InvalidResponse
                else -> Server
            }
        }
    }
}
