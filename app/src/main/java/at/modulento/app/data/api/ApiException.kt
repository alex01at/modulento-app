package at.modulento.app.data.api

/** The only exception type that leaves the API layer. */
sealed class ApiException(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** The server answered with an error envelope or a non-2xx status. */
    class Http(val status: Int, val code: String, message: String) : ApiException(message)

    /** The server could not be reached (DNS, connection, timeout, TLS, blocked cleartext). */
    class Network(cause: Throwable) : ApiException(cause.message, cause)

    /** The server answered, but not with something this app understands. */
    class InvalidResponse(cause: Throwable? = null) : ApiException(cause?.message, cause)

    /**
     * True if a Bearer-protected call was rejected because of the token itself.
     * The server answers such calls with 403 `unauthenticated` (not only 401),
     * and with 400 `invalid_token` on logout. Other 403s (e.g. `email_unverified`
     * on login) are not token problems and must not end the session.
     */
    val isAuthFailure: Boolean
        get() = this is Http &&
            (status == 401 || code == "unauthenticated" || code == "invalid_token")
}
