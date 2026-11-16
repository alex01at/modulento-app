package at.modulento.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `keeps a clean url and strips the trailing slash`() {
        assertEquals("https://example.at", ServerUrl.normalize("https://example.at/"))
        assertEquals("http://10.0.2.2:8098", ServerUrl.normalize("  http://10.0.2.2:8098  "))
    }

    @Test
    fun `a missing scheme means https`() {
        assertEquals("https://www.example.at", ServerUrl.normalize("www.example.at"))
    }

    @Test
    fun `keeps a sub-directory and drops query and fragment`() {
        assertEquals("https://example.at/shop", ServerUrl.normalize("https://example.at/shop/?a=1#top"))
    }

    @Test
    fun `rejects input that is not a web address`() {
        assertNull(ServerUrl.normalize(""))
        assertNull(ServerUrl.normalize("   "))
        assertNull(ServerUrl.normalize("ftp://example.at"))
        assertNull(ServerUrl.normalize("example at"))
        assertNull(ServerUrl.normalize("https://user:secret@example.at"))
    }

    @Test
    fun `builds page urls without doubled slashes`() {
        assertEquals("https://example.at/", ServerUrl.page("https://example.at", "/"))
        assertEquals("https://example.at/shop/account", ServerUrl.page("https://example.at/shop", "/account"))
    }
}
