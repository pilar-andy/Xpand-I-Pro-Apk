package com.xpandpro.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionPlannerTest {
    @Test fun horizontalA1_30RecortaLosLadosYEstiraAlPanel() {
        // 1080x2340 natural, girado a horizontal (rotación 1): lienzo 2340x1080.
        val p = ProjectionPlanner.stretch(1080, 2340, 1, 2340, 1080, 1300) as ProjectionPlan.Ok
        assertEquals(Box(270, 0, 2070, 1080), p.layerStack)
        assertEquals(Box(0, 0, 2340, 1080), p.display)
    }

    @Test fun rotacion3TambienEsHorizontal() {
        val p = ProjectionPlanner.stretch(1080, 2340, 3, 2340, 1080, 1300) as ProjectionPlan.Ok
        assertEquals(Box(270, 0, 2070, 1080), p.layerStack)
    }

    @Test fun verticalEstiraElLadoLargoQueEsElAlto() {
        val p = ProjectionPlanner.stretch(1080, 2340, 0, 1080, 2340, 1300) as ProjectionPlan.Ok
        assertEquals(Box(0, 270, 1080, 2070), p.layerStack)
        assertEquals(Box(0, 0, 1080, 2340), p.display)
    }

    @Test fun elRecorteSiempreEsParYCentrado() {
        for (permille in 1050..1700 step 10) {
            val p = ProjectionPlanner.stretch(1080, 2340, 1, 2340, 1080, permille) as ProjectionPlan.Ok
            assertEquals(0, p.layerStack.width % 2)
            assertEquals(p.layerStack.left, 2340 - p.layerStack.right)
            assertTrue(p.layerStack.left >= 0 && p.layerStack.right <= 2340)
        }
    }

    @Test fun multiplicadorFueraDeRangoEsInvalido() {
        listOf(1000, 1049, 1701, 2000).forEach {
            assertTrue(it.toString(), ProjectionPlanner.stretch(1080, 2340, 1, 2340, 1080, it) is ProjectionPlan.Invalid)
        }
    }

    @Test fun conTamanoPersonalizadoEsInvalido() {
        // El lienzo ya no coincide con el panel (hay un `wm size` activo).
        assertTrue(ProjectionPlanner.stretch(1080, 2340, 1, 1846, 1080, 1300) is ProjectionPlan.Invalid)
    }

    @Test fun datosInvalidosSonInvalidos() {
        assertTrue(ProjectionPlanner.stretch(0, 2340, 1, 2340, 1080, 1300) is ProjectionPlan.Invalid)
        assertTrue(ProjectionPlanner.stretch(1080, 2340, 7, 2340, 1080, 1300) is ProjectionPlan.Invalid)
    }

    @Test fun boxTextoParaDiagnostico() {
        assertEquals("1,2,3,4", Box(1, 2, 3, 4).toString())
    }
}
