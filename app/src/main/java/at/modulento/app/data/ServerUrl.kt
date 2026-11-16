package at.modulento.app.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ServerUrl {

    /**
     * Turns user input into the canonical form stored by the app: http(s),
     * no query/fragment, no trailing slash. A missing scheme means HTTPS.
     * Returns null if the input is not a usable web address.
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val url = withScheme.toHttpUrlOrNull() ?: return null
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
        return url.newBuilder()
            .query(null)
            .fragment(null)
            .build()
            .toString()
            .trimEnd('/')
    }

    /** Absolute URL of a local site path such as `/` or `/account`. */
    fun page(serverUrl: String, path: String): String =
        serverUrl.trimEnd('/') + "/" + path.trimStart('/')
}
