package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayControllerTest {

    @Test fun activarYRestaurar() {
        val dev = FakeDevice(); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.apply(1.30) is Outcome.Success)
        assertEquals(Resolution(1080, 3120), dev.override)
        assertTrue(store.stretched)

        assertTrue(c.restore() is Outcome.Success)
        assertNull(dev.override)
        assertFalse(store.stretched)
    }

    @Test fun restauraElOverrideQueHabiaAntes() {
        val dev = FakeDevice(override = Resolution(1080, 2000)); val store = FakeStore()
        val c = DisplayController(dev, store)

        c.apply(1.30)
        c.restore()
        assertEquals(Resolution(1080, 2000), dev.override)
    }

    @Test fun activarDosVecesNoPisaElOriginal() {
        val dev = FakeDevice(); val store = FakeStore()
        val c = DisplayController(dev, store)

        c.apply(1.30)
        c.apply(1.20)
        c.restore()
        assertNull(dev.override)
    }

    @Test fun siElTelefonoRechazaElCambioSeRestaura() {
        val dev = FakeDevice(rejectSet = true); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.apply(1.30) is Outcome.Failure)
        assertNull(dev.override)
        assertFalse(store.stretched)
    }

    @Test fun restaurarSinDatosGuardadosUsaReset() {
        val dev = FakeDevice(override = Resolution(1080, 3120)); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.restore() is Outcome.Success)
        assertNull(dev.override)
        assertTrue(dev.commands.contains("wm size reset"))
    }

    @Test fun siTodoFallaRestaurarDevuelveError() {
        val dev = FakeDevice(failAllCommands = true); val store = FakeStore()
        assertTrue(DisplayController(dev, store).restore() is Outcome.Failure)
    }
}
