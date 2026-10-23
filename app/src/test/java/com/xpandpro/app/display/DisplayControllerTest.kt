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
        assertEquals(Resolution(1080, 1846), dev.override)
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
        val dev = FakeDevice(override = Resolution(1080, 1846)); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.restore() is Outcome.Success)
        assertNull(dev.override)
        assertTrue(dev.commands.contains("wm size reset"))
    }

    @Test fun siTodoFallaRestaurarDevuelveError() {
        val dev = FakeDevice(failAllCommands = true); val store = FakeStore()
        assertTrue(DisplayController(dev, store).restore() is Outcome.Failure)
    }

    @Test fun pruebaDeCompatibilidadAplica4por3YSeRestaura() {
        val dev = FakeDevice(); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.applyPlan { StretchPlanner.planAspect(it, 4, 3) } is Outcome.Success)
        assertEquals(Resolution(1080, 1440), dev.override)
        assertTrue(c.restore() is Outcome.Success)
        assertNull(dev.override)
        assertFalse(store.stretched)
    }

    @Test fun densidadYEscaladoSeAplicanYSeRestauran() {
        val dev = FakeDevice(); val store = FakeStore()
        val c = DisplayController(dev, store)

        val out = c.apply(1.30, setOf(Tweak.DENSITY, Tweak.SCALING_OFF))
        assertTrue(out is Outcome.Success)
        assertEquals(Resolution(1080, 1846), dev.override)
        assertEquals(323, dev.densityOverride) // 420 * 1846 / 2400
        assertTrue(dev.scalingOff)

        assertTrue(c.restore() is Outcome.Success)
        assertNull(dev.override)
        assertNull(dev.densityOverride)
        assertFalse(dev.scalingOff)
        assertFalse(store.densityChanged)
        assertFalse(store.scalingChanged)
    }

    @Test fun restauraLaDensidadQueHabiaAntes() {
        val dev = FakeDevice(densityOverride = 380); val store = FakeStore()
        val c = DisplayController(dev, store)

        c.apply(1.30, setOf(Tweak.DENSITY))
        c.restore()
        assertEquals(380, dev.densityOverride)
    }

    @Test fun siElTelefonoRechazaLaDensidadSeRestauraTodo() {
        val dev = FakeDevice(rejectDensity = true); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.apply(1.30, setOf(Tweak.DENSITY)) is Outcome.Failure)
        assertNull(dev.override)
        assertNull(dev.densityOverride)
        assertFalse(store.stretched)
        assertFalse(store.densityChanged)
    }

    @Test fun siFallaElEscaladoSeRestauraTodo() {
        val dev = FakeDevice(rejectScaling = true); val store = FakeStore()
        val c = DisplayController(dev, store)

        assertTrue(c.apply(1.30, setOf(Tweak.SCALING_OFF)) is Outcome.Failure)
        assertNull(dev.override)
        assertFalse(dev.scalingOff)
        assertFalse(store.scalingChanged)
    }

    @Test fun sinAjustesExtraNoTocaDensidadNiEscalado() {
        val dev = FakeDevice(); val store = FakeStore()
        DisplayController(dev, store).apply(1.30)
        assertTrue(dev.commands.none { it.startsWith("wm density ") || it.startsWith("wm scaling") })
    }
}
