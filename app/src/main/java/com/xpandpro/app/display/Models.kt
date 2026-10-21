package com.xpandpro.app.display

data class Resolution(val width: Int, val height: Int) {
    override fun toString() = "${width}x$height"

    companion object {
        fun parse(text: String?): Resolution? {
            if (text == null) return null
            val m = Regex("""^\s*(\d+)x(\d+)\s*$""").find(text) ?: return null
            return Resolution(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
    }
}

/** Estado leído de `wm size`. */
data class DisplayState(val physical: Resolution, val override: Resolution?) {
    val current: Resolution get() = override ?: physical

    /** True si hay una resolución distinta de la física activa. */
    val isOverridden: Boolean get() = override != null && override != physical
}

data class ShellResult(val exitCode: Int, val output: String) {
    val ok: Boolean get() = exitCode == 0
}

fun interface ShellRunner {
    fun run(command: String): ShellResult
}

/** Lo mínimo que hay que recordar para poder restaurar siempre. */
interface StateStore {
    var stretched: Boolean

    /** Override que existía ANTES de estirar ("" = ninguno). null = sin dato. */
    var originalOverride: String?
}

sealed class Outcome {
    data class Success(val state: DisplayState) : Outcome()
    data class Failure(val message: String) : Outcome()
}
