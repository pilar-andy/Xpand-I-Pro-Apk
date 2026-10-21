package com.xpandpro.app.display

sealed class Plan {
    data class Ok(val target: Resolution) : Plan()
    data class Invalid(val reason: String) : Plan()
}

object StretchPlanner {
    const val MIN_MULTIPLIER = 1.01
    const val MAX_MULTIPLIER = 1.30

    /**
     * Mantiene el lado corto de la pantalla física y AGRANDA el lado largo
     * multiplicándolo (p. ej. 2688×1216 con 1.30x → 3494×1216). Respeta la
     * orientación "natural" que reporta `wm size` (teléfonos en vertical,
     * algunas tablets en horizontal). Siempre parte de la resolución física,
     * nunca de una ya modificada.
     */
    fun plan(state: DisplayState, multiplier: Double): Plan {
        val p = state.physical
        val short = minOf(p.width, p.height)
        val long = maxOf(p.width, p.height)
        if (short <= 0) return Plan.Invalid("No se pudo leer el tamaño de la pantalla.")
        if (multiplier.isNaN() || multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
            return Plan.Invalid("El multiplicador debe estar entre 1.01x y 1.30x.")
        }

        var newLong = Math.round(long * multiplier).toInt()
        newLong -= newLong % 2 // medidas pares: más compatibles
        if (newLong <= long) {
            return Plan.Invalid("Con este multiplicador la pantalla no cambia. Sube el valor.")
        }
        val target = if (p.width <= p.height) Resolution(short, newLong) else Resolution(newLong, short)
        return Plan.Ok(target)
    }
}
