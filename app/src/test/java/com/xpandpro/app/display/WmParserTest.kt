package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WmParserTest {
    @Test fun sinOverride() {
        val s = WmParser.parseSize("Physical size: 1080x2400\n")!!
        assertEquals(Resolution(1080, 2400), s.physical)
        assertNull(s.override)
        assertFalse(s.isOverridden)
    }

    @Test fun conOverride() {
        val s = WmParser.parseSize("Physical size: 1080x2400\nOverride size: 1080x1440\n")!!
        assertEquals(Resolution(1080, 1440), s.current)
        assertTrue(s.isOverridden)
    }

    @Test fun salidaInvalida() {
        assertNull(WmParser.parseSize("cmd: not found"))
    }

    @Test fun densidadSinOverride() {
        val d = WmParser.parseDensity("Physical density: 420\n")!!
        assertEquals(420, d.current)
        assertFalse(d.isOverridden)
    }

    @Test fun densidadConOverride() {
        val d = WmParser.parseDensity("Physical density: 420\nOverride density: 323\n")!!
        assertEquals(323, d.current)
        assertTrue(d.isOverridden)
    }

    @Test fun densidadInvalida() {
        assertNull(WmParser.parseDensity("nope"))
    }
}
