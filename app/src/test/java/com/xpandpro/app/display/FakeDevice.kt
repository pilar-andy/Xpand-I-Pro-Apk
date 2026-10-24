package com.xpandpro.app.display

/** Simula `wm size`, `wm density` y `wm scaling` de un teléfono para probar la lógica sin Android. */
class FakeDevice(
    private val physical: Resolution = Resolution(1080, 2400),
    var override: Resolution? = null,
    var rejectSet: Boolean = false,
    var failAllCommands: Boolean = false,
    private val physicalDensity: Int = 420,
    var densityOverride: Int? = null,
    var rejectDensity: Boolean = false,
    var scalingOff: Boolean = false,
    var rejectScaling: Boolean = false,
    var rejectProjection: Boolean = false,
) : ShellRunner {
    val commands = mutableListOf<String>()

    /** Simula que la imagen está ensanchada por la proyección directa. */
    var projectionActive: Boolean = false

    override fun run(command: String): ShellResult {
        commands += command
        if (failAllCommands) return ShellResult(1, "error")
        return when {
            command == "wm size" -> {
                val o = override?.let { "\nOverride size: $it" } ?: ""
                ShellResult(0, "Physical size: $physical$o\n")
            }
            // Cambiar el tamaño hace que el sistema vuelva a aplicar su propia proyección.
            command == "wm size reset" -> { override = null; projectionActive = false; ShellResult(0, "") }
            command.startsWith("wm size ") -> {
                if (!rejectSet) override = Resolution.parse(command.removePrefix("wm size "))
                projectionActive = false
                ShellResult(0, "")
            }
            command.startsWith("xpand-projection stretch ") -> {
                if (rejectProjection) ShellResult(1, "ERR NoSuchMethodException")
                else { projectionActive = true; ShellResult(0, "ok") }
            }
            command == "wm density" -> {
                val o = densityOverride?.let { "\nOverride density: $it" } ?: ""
                ShellResult(0, "Physical density: $physicalDensity$o\n")
            }
            command == "wm density reset" -> { densityOverride = null; ShellResult(0, "") }
            command.startsWith("wm density ") -> {
                if (!rejectDensity) densityOverride = command.removePrefix("wm density ").toInt()
                ShellResult(0, "")
            }
            command == "wm scaling off" -> {
                if (rejectScaling) ShellResult(1, "error") else { scalingOff = true; ShellResult(0, "") }
            }
            command == "wm scaling auto" -> { scalingOff = false; ShellResult(0, "") }
            else -> ShellResult(2, "unknown")
        }
    }
}

class FakeStore : StateStore {
    override var stretched = false
    override var originalOverride: String? = null
    override var originalDensity: String? = null
    override var densityChanged = false
    override var scalingChanged = false
    override var projectionChanged = false
}
