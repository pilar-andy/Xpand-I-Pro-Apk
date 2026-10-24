package com.xpandpro.app.display

/** Rectángulo en píxeles: izquierda, arriba, derecha, abajo. */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    override fun toString() = "$left,$top,$right,$bottom"
}

sealed class ProjectionPlan {
    /** layerStack: parte de la imagen que se toma. display: parte del panel donde se dibuja. */
    data class Ok(val layerStack: Box, val display: Box) : ProjectionPlan()
    data class Invalid(val reason: String) : ProjectionPlan()
}

object ProjectionPlanner {
    /**
     * Estirado "con recorte": se toma la franja central de la imagen
     * (lado largo / multiplicador) y se estira a todo el panel. Los personajes se ven
     * `multiplicador` veces más anchos y se pierden los bordes. No cambia `wm size`.
     *
     * @param physW0 ancho físico en orientación natural (el que da `wm size`)
     * @param physH0 alto físico en orientación natural
     * @param rotation 0..3 como `Display.getRotation()`
     * @param logicalW ancho lógico actual (ya girado)
     * @param logicalH alto lógico actual (ya girado)
     * @param permille multiplicador por mil (1300 = 1.30x)
     */
    fun stretch(
        physW0: Int, physH0: Int, rotation: Int,
        logicalW: Int, logicalH: Int, permille: Int,
    ): ProjectionPlan {
        val m = permille / 1000.0
        if (m < StretchPlanner.MIN_MULTIPLIER || m > StretchPlanner.MAX_MULTIPLIER) {
            return ProjectionPlan.Invalid("Multiplicador fuera de rango (1.05x a 1.70x).")
        }
        if (physW0 <= 0 || physH0 <= 0 || logicalW <= 0 || logicalH <= 0 || rotation !in 0..3) {
            return ProjectionPlan.Invalid("No se pudo leer la pantalla.")
        }
        val landscape = rotation % 2 == 1
        val physW = if (landscape) physH0 else physW0
        val physH = if (landscape) physW0 else physH0
        if (logicalW != physW || logicalH != physH) {
            return ProjectionPlan.Invalid("La pantalla tiene un tamaño personalizado. Restaura primero.")
        }

        val display = Box(0, 0, physW, physH)
        // Se estira a lo largo del lado largo (en horizontal, el ancho).
        return if (logicalW >= logicalH) {
            val crop = evenCrop(logicalW, m) ?: return invalidCrop()
            val left = (logicalW - crop) / 2
            ProjectionPlan.Ok(Box(left, 0, left + crop, logicalH), display)
        } else {
            val crop = evenCrop(logicalH, m) ?: return invalidCrop()
            val top = (logicalH - crop) / 2
            ProjectionPlan.Ok(Box(0, top, logicalW, top + crop), display)
        }
    }

    private fun evenCrop(length: Int, m: Double): Int? {
        var crop = Math.round(length / m).toInt()
        crop -= crop % 2
        return if (crop in 2 until length) crop else null
    }

    private fun invalidCrop() = ProjectionPlan.Invalid("Con este multiplicador la imagen no cambia.")
}
