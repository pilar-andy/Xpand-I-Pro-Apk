package com.xpandpro.app.display

/**
 * Lógica central: activar y restaurar el estirado.
 *
 * Reglas de fiabilidad:
 *  1. Antes de tocar nada se guarda el estado original (y se marca "estirado").
 *  2. Después de cada cambio se vuelve a leer `wm size` para comprobar que se aplicó.
 *  3. Si algo falla al activar, se restaura de inmediato.
 *  4. Restaurar siempre tiene salida: si no hay datos guardados o falla, `wm size reset`.
 *  5. Los ajustes extra (densidad, escalado) se anotan ANTES de aplicarse y se
 *     deshacen siempre al restaurar (`wm density`, `wm scaling auto`).
 */
class DisplayController(
    private val shell: ShellRunner,
    private val store: StateStore,
) {
    fun readState(): DisplayState? {
        val r = shell.run("wm size")
        return if (r.ok) WmParser.parseSize(r.output) else null
    }

    fun readDensity(): DensityState? {
        val r = shell.run("wm density")
        return if (r.ok) WmParser.parseDensity(r.output) else null
    }

    fun apply(multiplier: Double, tweaks: Set<Tweak> = emptySet()): Outcome =
        applyPlan(tweaks) { StretchPlanner.plan(it, multiplier) }

    fun applyPlan(tweaks: Set<Tweak> = emptySet(), planner: (DisplayState) -> Plan): Outcome {
        val before = readState()
            ?: return Outcome.Failure("No se pudo leer la pantalla. ¿Shizuku sigue activo?")

        val target = when (val plan = planner(before)) {
            is Plan.Ok -> plan.target
            is Plan.Invalid -> return Outcome.Failure(plan.reason)
        }

        // Guardar el original solo la primera vez (no pisarlo con un valor ya estirado).
        if (!store.stretched) {
            store.originalOverride = if (before.isOverridden) before.override.toString() else ""
        }
        store.stretched = true

        val r = shell.run("wm size $target")
        val after = if (r.ok) readState() else null
        if (after == null || after.current != target) {
            restore()
            return Outcome.Failure("El teléfono no aceptó la resolución $target. Se restauró la pantalla.")
        }

        if (tweaks.isNotEmpty()) {
            val problem = applyTweaks(before, target, tweaks)
            if (problem != null) {
                restore()
                return Outcome.Failure("$problem Se restauró la pantalla.")
            }
        }
        return Outcome.Success(readState() ?: after)
    }

    /**
     * EXPERIMENTAL: proyección directa. No cambia `wm size`; ensancha la imagen en el compositor.
     * Solo se aplica si la pantalla está sin tamaño personalizado.
     */
    fun applyProjection(multiplier: Double): Outcome {
        val before = readState()
            ?: return Outcome.Failure("No se pudo leer la pantalla. ¿Shizuku sigue activo?")
        if (before.isOverridden) {
            return Outcome.Failure("Hay un tamaño personalizado activo. Restaura la pantalla primero.")
        }
        val permille = Math.round(multiplier * 1000.0).toInt()
        val p = before.physical

        store.projectionChanged = true // se anota ANTES de aplicar
        val r = shell.run("xpand-projection stretch ${p.width} ${p.height} $permille")
        if (!r.ok) {
            restore()
            return Outcome.Failure("No se pudo aplicar la proyección (${r.output.trim()}). Se restauró la pantalla.")
        }
        return Outcome.Success(before)
    }

    private fun applyTweaks(before: DisplayState, target: Resolution, tweaks: Set<Tweak>): String? {
        if (Tweak.DENSITY in tweaks) {
            val d = readDensity() ?: return "No se pudo leer la densidad."
            if (!store.densityChanged) store.originalDensity = if (d.isOverridden) d.current.toString() else ""
            store.densityChanged = true

            val longBefore = maxOf(before.physical.width, before.physical.height)
            val longAfter = maxOf(target.width, target.height)
            val newDensity = Math.round(d.physical.toDouble() * longAfter / longBefore).toInt().coerceIn(72, 1000)

            val r = shell.run("wm density $newDensity")
            val now = if (r.ok) readDensity() else null
            if (now == null || now.current != newDensity) return "El teléfono no aceptó la densidad $newDensity."
        }
        if (Tweak.SCALING_OFF in tweaks) {
            store.scalingChanged = true
            if (!shell.run("wm scaling off").ok) return "El teléfono no aceptó desactivar el escalado."
        }
        return null
    }

    fun restore(): Outcome {
        val extrasOk = restoreExtras()

        val original = Resolution.parse(store.originalOverride)
        val firstTry = if (store.stretched && original != null) "wm size $original" else "wm size reset"

        var state = tryCommand(firstTry)
        var restoredOk = isRestored(state, if (firstTry == "wm size reset") null else original)
        if (!restoredOk) {
            state = tryCommand("wm size reset")
            restoredOk = isRestored(state, null)
        }
        if (!restoredOk || state == null) {
            return Outcome.Failure("No se pudo restaurar. Usa Ajustes > Pantalla o reinicia el teléfono.")
        }
        store.stretched = false
        store.originalOverride = null
        if (!extrasOk) {
            return Outcome.Failure("Se restauró el tamaño, pero no la densidad o el escalado. Pulsa RESTAURAR otra vez.")
        }
        return Outcome.Success(state)
    }

    /** Devuelve true si no había nada que deshacer o todo se deshizo bien. */
    private fun restoreExtras(): Boolean {
        var ok = true
        if (store.projectionChanged) {
            // Rebote: cambiar `wm size` un instante obliga al sistema a volver a aplicar SU proyección,
            // sin depender de nuestras medidas. Luego `wm size reset` lo deja como estaba.
            val p = readState()?.physical
            if (p != null) {
                val bump = if (p.height >= p.width) "${p.width}x${maxOf(p.height - 2, 2)}" else "${maxOf(p.width - 2, 2)}x${p.height}"
                val a = shell.run("wm size $bump")
                val b = shell.run("wm size reset")
                if (a.ok && b.ok) store.projectionChanged = false else ok = false
            } else {
                ok = false
            }
        }
        if (store.scalingChanged) {
            if (shell.run("wm scaling auto").ok) store.scalingChanged = false else ok = false
        }
        if (store.densityChanged) {
            val original = store.originalDensity?.toIntOrNull()
            val first = if (original != null) "wm density $original" else "wm density reset"
            var done = shell.run(first).ok && densityRestored(original)
            if (!done) done = shell.run("wm density reset").ok && densityRestored(null)
            if (done) {
                store.densityChanged = false
                store.originalDensity = null
            } else {
                ok = false
            }
        }
        return ok
    }

    private fun densityRestored(original: Int?): Boolean {
        val d = readDensity() ?: return false
        return if (original != null) d.current == original else !d.isOverridden
    }

    private fun tryCommand(cmd: String): DisplayState? {
        val r = shell.run(cmd)
        return if (r.ok) readState() else null
    }

    private fun isRestored(state: DisplayState?, original: Resolution?): Boolean {
        if (state == null) return false
        return if (original != null) state.current == original else !state.isOverridden
    }
}
