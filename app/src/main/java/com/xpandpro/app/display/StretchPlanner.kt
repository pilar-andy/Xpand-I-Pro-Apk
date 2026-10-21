package com.xpandpro.app.display

sealed class Plan {
    data class Ok(val target: Resolution) : Plan()
    data class Invalid(val reason: String) : Plan()
}

object StretchPlanner {
    const val MIN_MULTIPLIER = 1.05
    const val MAX_MULTIPLIER = 1.70

    /**
     * Estirado por multiplicador: mantiene el lado corto y REDUCE el lado largo
     * dividiéndolo entre el multiplicador (p. ej. 2400 con 1.30x → 1846). Si el
     * sistema rellena la pantalla, la imagen se ve ensanchada por ese factor.
     * Respeta la orientación "natural" de `wm size` y parte siempre de la
     * resolución física, nunca de una ya modificada.
     */
    fun plan(state: DisplayState, multiplier: Double): Plan {
        val p = state.physical
        val short = minOf(p.width, p.height)
        val long = maxOf(p.width, p.height)
        if (short <= 0) return Plan.Invalid("No se pudo leer el tamaño de la pantalla.")
        if (multiplier.isNaN() || multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
            return Plan.Invalid("El multiplicador debe estar entre 1.05x y 1.70x.")
        }

        var newLong = Math.round(long / multiplier).toInt()
        newLong -= newLong % 2 // medidas pares: más compatibles
        if (newLong >= long) {
            return Plan.Invalid("Con este multiplicador la pantalla no cambia. Sube el valor.")
        }
        if (newLong < short) {
            return Plan.Invalid("Este multiplicador es demasiado alto para tu pantalla. Bájalo.")
        }
        return Plan.Ok(withOrientation(p, short, newLong))
    }

    /**
     * Reduce el lado largo hasta una proporción fija (p. ej. 4:3). Se usa en la
     * prueba de compatibilidad.
     */
    fun planAspect(state: DisplayState, num: Int, den: Int): Plan {
        val p = state.physical
        val short = minOf(p.width, p.height)
        val long = maxOf(p.width, p.height)
        if (short <= 0) return Plan.Invalid("No se pudo leer el tamaño de la pantalla.")

        var newLong = (short.toLong() * num / den).toInt()
        newLong -= newLong % 2
        if (newLong >= long) {
            return Plan.Invalid("Tu pantalla ya es igual o más cuadrada que $num:$den; no hace falta estirar.")
        }
        return Plan.Ok(withOrientation(p, short, newLong))
    }

    private fun withOrientation(p: Resolution, short: Int, newLong: Int) =
        if (p.width <= p.height) Resolution(short, newLong) else Resolution(newLong, short)
}
