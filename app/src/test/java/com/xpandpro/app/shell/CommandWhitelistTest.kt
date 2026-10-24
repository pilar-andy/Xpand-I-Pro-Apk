package com.xpandpro.app.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandWhitelistTest {
    @Test fun aceptaLosComandosPrevistos() {
        listOf(
            "wm size", "wm size reset", "wm size 1080x1846",
            "wm density", "wm density reset", "wm density 323",
            "wm scaling off", "wm scaling auto",
            "xpand-projection info", "xpand-projection stretch 1080 2340 1300",
        ).forEach { assertTrue(it, CommandWhitelist.isAllowed(it)) }
    }

    @Test fun rechazaInyeccionYVariantes() {
        listOf(
            "wm size 1080x1846; reboot",
            "wm size 1080x1846 && id",
            "wm size 1080x1846\nid",
            "wm density 323 | cat",
            "wm density 3",
            "wm density 12345",
            "wm scaling on",
            "wm scaling off; id",
            "wm overscan 0,0,0,0",
            "wm size \$(id)",
            "xpand-projection stretch 1080 2340 1300; id",
            "xpand-projection stretch 1080 2340",
            "xpand-projection reset",
            "id", "", "sh -c id",
        ).forEach { assertFalse(it, CommandWhitelist.isAllowed(it)) }
    }
}
