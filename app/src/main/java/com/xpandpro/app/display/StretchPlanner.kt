package com.xpandpro.app.display

sealed class Plan {
    data class Ok(val target: Resolution) : Plan()
    data class Invalid(val reason: String) : Plan()
}

object StretchPlanner {
    /**
     * Mantiene el lado corto de la pantalla física y reduce el lado largo
     * hasta la proporción pedida. Respeta la orientación "natural" que
     * reporta `wm size` (teléfonos en vertical, algunas tablets en horizontal).
     */
    fun plan(state: DisplayState, preset: StretchPreset): Plan {
        val p = state.physical
        val short = minOf(p.width, p.height)
        val long = maxOf(p.width, p.height)
        if (short <= 0) return Plan.Invalid("No se pudo leer el tamaño de la pantalla.")

        var newLong = (short.toLong() * preset.num / preset.den).toInt()
        newLong -= newLong % 2 // medidas pares: más compatibles
        if (newLong >= long) {
            return Plan.Invalid("Tu pantalla ya es igual o más cuadrada que ${preset.label}. Elige otro formato.")
        }
        val target = if (p.width <= p.height) Resolution(short, newLong) else Resolution(newLong, short)
        return Plan.Ok(target)
    }
}
