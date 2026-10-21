package com.xpandpro.app.shell

import kotlin.system.exitProcess

/**
 * Corre dentro del proceso "shell" que arranca Shizuku (privilegios de adb).
 * Solo acepta comandos `wm size ...`; cualquier otra cosa se rechaza.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String?): String {
        val cmd = command?.trim().orEmpty()
        if (!ALLOWED.matches(cmd)) return "-2\nComando no permitido"
        return try {
            val process = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val code = process.waitFor()
            "$code\n$output"
        } catch (t: Throwable) {
            "-1\n${t.message}"
        }
    }

    private companion object {
        val ALLOWED = Regex("""wm size( reset| \d{2,5}x\d{2,5})?""")
    }
}
