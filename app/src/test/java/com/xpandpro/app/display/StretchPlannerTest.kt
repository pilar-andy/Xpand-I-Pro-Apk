package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchPlannerTest {
    private fun state(w: Int, h: Int) = DisplayState(Resolution(w, h), null)

    /** Números reales de Cat Resolution Pro: 2688×1216 a 1.30x → 3494×1216. */
    @Test fun igualaACatResolutionPro() {
        val p = StretchPlanner.plan(state(1216, 2688), 1.30) as Plan.Ok
        assertEquals(Resolution(1216, 3494), p.target)
    }

    @Test fun telefono20por9a1_30() {
        val p = StretchPlanner.plan(state(1080, 2400), 1.30) as Plan.Ok
        assertEquals(Resolution(1080, 3120), p.target)
    }

    @Test fun telefono20por9a1_20() {
        val p = StretchPlanner.plan(state(1080, 2400), 1.20) as Plan.Ok
        assertEquals(Resolution(1080, 2880), p.target)
    }

    @Test fun mantieneElLadoCortoYAgrandaElLargo() {
        val p = StretchPlanner.plan(state(1080, 2400), 1.15) as Plan.Ok
        assertEquals(1080, p.target.width)
        assertTrue(p.target.height > 2400)
    }

    @Test fun tabletHorizontalMantieneOrientacion() {
        val p = StretchPlanner.plan(state(2560, 1600), 1.25) as Plan.Ok
        assertEquals(Resolution(3200, 1600), p.target)
    }

    @Test fun multiplicadorFueraDeRangoEsInvalido() {
        assertTrue(StretchPlanner.plan(state(1080, 2400), 1.00) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), 0.80) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), 1.31) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), Double.NaN) is Plan.Invalid)
    }

    @Test fun siempreParteDeLaResolucionFisica() {
        val modified = DisplayState(Resolution(1080, 2400), Resolution(1080, 3120))
        val p = StretchPlanner.plan(modified, 1.10) as Plan.Ok
        assertEquals(Resolution(1080, 2640), p.target)
    }

    @Test fun medidasSiempreParesYMayoresQueLaFisica() {
        for (percent in 101..130) {
            val p = StretchPlanner.plan(state(1079, 2401), percent / 100.0)
            if (p is Plan.Ok) {
                assertEquals(0, p.target.height % 2)
                assertTrue(p.target.height > 2401)
            }
        }
    }

    @Test fun textoHorizontal() {
        assertEquals("2688 × 1216", Resolution(1216, 2688).landscapeText())
    }
}
