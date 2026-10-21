package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchPlannerTest {
    private fun state(w: Int, h: Int) = DisplayState(Resolution(w, h), null)

    @Test fun telefono20por9a4por3() {
        val p = StretchPlanner.plan(state(1080, 2400), StretchPreset.R4_3) as Plan.Ok
        assertEquals(Resolution(1080, 1440), p.target)
    }

    @Test fun telefono20por9a16por10() {
        val p = StretchPlanner.plan(state(1080, 2400), StretchPreset.R16_10) as Plan.Ok
        assertEquals(Resolution(1080, 1728), p.target)
    }

    @Test fun tabletHorizontalMantieneOrientacion() {
        val p = StretchPlanner.plan(state(2560, 1600), StretchPreset.R4_3) as Plan.Ok
        assertEquals(Resolution(2132, 1600), p.target)
    }

    @Test fun pantallaYaMasCuadradaQueElFormato() {
        val p = StretchPlanner.plan(state(1080, 1440), StretchPreset.R16_9)
        assertTrue(p is Plan.Invalid)
    }

    @Test fun medidasSiempreParesYMenoresQueLaFisica() {
        for (preset in StretchPreset.values()) {
            val p = StretchPlanner.plan(state(1079, 2401), preset)
            if (p is Plan.Ok) {
                assertEquals(0, p.target.height % 2)
                assertTrue(p.target.height < 2401)
            }
        }
    }
}
