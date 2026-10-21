package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchPlannerTest {
    private fun state(w: Int, h: Int) = DisplayState(Resolution(w, h), null)

    // ---- estirado por multiplicador (reduce el lado largo) ----

    @Test fun telefono20por9a1_30() {
        val p = StretchPlanner.plan(state(1080, 2400), 1.30) as Plan.Ok
        assertEquals(Resolution(1080, 1846), p.target)
    }

    @Test fun telefono20por9a1_50() {
        val p = StretchPlanner.plan(state(1080, 2400), 1.50) as Plan.Ok
        assertEquals(Resolution(1080, 1600), p.target)
    }

    @Test fun mantieneElLadoCortoYReduceElLargo() {
        val p = StretchPlanner.plan(state(1080, 2340), 1.25) as Plan.Ok
        assertEquals(1080, p.target.width)
        assertTrue(p.target.height < 2340)
    }

    @Test fun tabletHorizontalMantieneOrientacion() {
        val p = StretchPlanner.plan(state(2560, 1600), 1.25) as Plan.Ok
        assertEquals(Resolution(2048, 1600), p.target)
    }

    @Test fun multiplicadorFueraDeRangoEsInvalido() {
        assertTrue(StretchPlanner.plan(state(1080, 2400), 1.00) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), 0.80) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), 1.71) is Plan.Invalid)
        assertTrue(StretchPlanner.plan(state(1080, 2400), Double.NaN) is Plan.Invalid)
    }

    @Test fun noDejaElLadoLargoMasCortoQueElCorto() {
        // 1080x1200 con 1.30x daría 922 de largo (< 1080): se rechaza.
        assertTrue(StretchPlanner.plan(state(1080, 1200), 1.30) is Plan.Invalid)
    }

    @Test fun siempreParteDeLaResolucionFisica() {
        val modified = DisplayState(Resolution(1080, 2400), Resolution(1080, 1600))
        val p = StretchPlanner.plan(modified, 1.20) as Plan.Ok
        assertEquals(Resolution(1080, 2000), p.target)
    }

    @Test fun medidasSiempreParesYMenoresQueLaFisica() {
        for (percent in 105..170) {
            val p = StretchPlanner.plan(state(1079, 2401), percent / 100.0)
            assertTrue(p is Plan.Ok)
            p as Plan.Ok
            assertEquals(0, p.target.height % 2)
            assertTrue(p.target.height < 2401)
            assertTrue(p.target.height >= 1079)
        }
    }

    // ---- proporción fija (prueba de compatibilidad) ----

    @Test fun telefono20por9a4por3() {
        val p = StretchPlanner.planAspect(state(1080, 2400), 4, 3) as Plan.Ok
        assertEquals(Resolution(1080, 1440), p.target)
    }

    @Test fun telefono2340a4por3() {
        val p = StretchPlanner.planAspect(state(1080, 2340), 4, 3) as Plan.Ok
        assertEquals(Resolution(1080, 1440), p.target)
    }

    @Test fun tabletHorizontal4por3() {
        val p = StretchPlanner.planAspect(state(2560, 1600), 4, 3) as Plan.Ok
        assertEquals(Resolution(2132, 1600), p.target)
    }

    @Test fun pantallaYaMasCuadradaQueElFormato() {
        assertTrue(StretchPlanner.planAspect(state(1080, 1440), 16, 9) is Plan.Invalid)
    }

    @Test fun textoHorizontal() {
        assertEquals("2400 × 1080", Resolution(1080, 2400).landscapeText())
    }
}
