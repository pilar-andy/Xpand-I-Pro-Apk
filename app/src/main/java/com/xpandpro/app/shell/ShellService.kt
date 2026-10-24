package com.xpandpro.app.shell

import kotlin.system.exitProcess

/**
 * Corre dentro del proceso "shell" que arranca Shizuku (privilegios de adb).
 * Solo acepta comandos `wm size`, `wm density` y `wm scaling` con argumentos
 * exactos (coincidencia completa, sin `;` ni nada extra); lo demás se rechaza.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String?): String {
        val cmd = command?.trim().orEmpty()
        if (!CommandWhitelist.isAllowed(cmd)) return "-2\nComando no permitido"
        // Comandos propios: se resuelven aquí, sin pasar por `sh`.
        if (cmd.startsWith("xpand-projection")) return ProjectionTool.handle(cmd)
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
}
