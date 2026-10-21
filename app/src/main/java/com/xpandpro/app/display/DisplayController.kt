package com.xpandpro.app.display

/**
 * Lógica central: activar y restaurar el estirado.
 *
 * Reglas de fiabilidad:
 *  1. Antes de tocar nada se guarda el estado original (y se marca "estirado").
 *  2. Después de cada cambio se vuelve a leer `wm size` para comprobar que se aplicó.
 *  3. Si algo falla al activar, se restaura de inmediato.
 *  4. Restaurar siempre tiene salida: si no hay datos guardados o falla, `wm size reset`.
 */
class DisplayController(
    private val shell: ShellRunner,
    private val store: StateStore,
) {
    fun readState(): DisplayState? {
        val r = shell.run("wm size")
        return if (r.ok) WmParser.parseSize(r.output) else null
    }

    fun apply(multiplier: Double): Outcome {
        val before = readState()
            ?: return Outcome.Failure("No se pudo leer la pantalla. ¿Shizuku sigue activo?")

        val target = when (val plan = StretchPlanner.plan(before, multiplier)) {
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
        return Outcome.Success(after)
    }

    fun restore(): Outcome {
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
        return Outcome.Success(state)
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
