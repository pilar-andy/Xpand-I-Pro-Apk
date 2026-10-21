package com.xpandpro.app.display

/** Simula `wm size` de un teléfono para probar la lógica sin Android. */
class FakeDevice(
    private val physical: Resolution = Resolution(1080, 2400),
    var override: Resolution? = null,
    var rejectSet: Boolean = false,
    var failAllCommands: Boolean = false,
) : ShellRunner {
    val commands = mutableListOf<String>()

    override fun run(command: String): ShellResult {
        commands += command
        if (failAllCommands) return ShellResult(1, "error")
        return when {
            command == "wm size" -> {
                val o = override?.let { "\nOverride size: $it" } ?: ""
                ShellResult(0, "Physical size: $physical$o\n")
            }
            command == "wm size reset" -> { override = null; ShellResult(0, "") }
            command.startsWith("wm size ") -> {
                if (!rejectSet) override = Resolution.parse(command.removePrefix("wm size "))
                ShellResult(0, "")
            }
            else -> ShellResult(2, "unknown")
        }
    }
}

class FakeStore : StateStore {
    override var stretched = false
    override var originalOverride: String? = null
}
