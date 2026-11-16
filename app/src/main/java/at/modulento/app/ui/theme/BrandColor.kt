package at.modulento.app.ui.theme

object BrandColor {

    /**
     * Parses `#RGB` or `#RRGGBB` into an opaque ARGB value. Anything else
     * (named colors, rgb(), empty) yields null so the caller can fall back.
     */
    fun parseHex(value: String?): Int? {
        val text = value?.trim() ?: return null
        if (!text.startsWith("#")) return null
        val hex = text.substring(1)
        if (hex.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) return null
        val rgb = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        return (0xFF000000 or rgb.toLong(16)).toInt()
    }
}
