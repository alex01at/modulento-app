package at.modulento.app.ui

import at.modulento.app.ui.theme.BrandColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandColorTest {

    @Test
    fun `parses six and three digit hex colors`() {
        assertEquals(0xFFD4245A.toInt(), BrandColor.parseHex("#d4245a"))
        assertEquals(0xFFD4245A.toInt(), BrandColor.parseHex(" #D4245A "))
        assertEquals(0xFFFFAA00.toInt(), BrandColor.parseHex("#fa0"))
    }

    @Test
    fun `anything else falls back`() {
        assertNull(BrandColor.parseHex(null))
        assertNull(BrandColor.parseHex(""))
        assertNull(BrandColor.parseHex("d4245a"))
        assertNull(BrandColor.parseHex("#d4245"))
        assertNull(BrandColor.parseHex("#gggggg"))
        assertNull(BrandColor.parseHex("#+1234"))
        assertNull(BrandColor.parseHex("red"))
        assertNull(BrandColor.parseHex("rgb(1,2,3)"))
    }
}
