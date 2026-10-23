package com.xpandpro.app.shell

/**
 * Lista blanca de comandos que el servicio privilegiado acepta.
 * Coincidencia COMPLETA: cualquier carácter extra (`;`, `&&`, saltos de línea...) la rechaza.
 */
object CommandWhitelist {
    private val ALLOWED = Regex(
        """wm size( reset| \d{2,5}x\d{2,5})?""" +
            """|wm density( reset| \d{2,4})?""" +
            """|wm scaling (off|auto)"""
    )

    fun isAllowed(command: String): Boolean = ALLOWED.matches(command)
}
