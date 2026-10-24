package com.xpandpro.app.shell

/**
 * Lista blanca de comandos que el servicio privilegiado acepta.
 * Coincidencia COMPLETA: cualquier carácter extra (`;`, `&&`, saltos de línea...) la rechaza.
 */
object CommandWhitelist {
    private val ALLOWED = Regex(
        """wm size( reset| \d{2,5}x\d{2,5})?""" +
            """|wm density( reset| \d{2,4})?""" +
            """|wm scaling (off|auto)""" +
            """|xpand-projection (info|stretch \d{3,5} \d{3,5} \d{4})"""
    )

    fun isAllowed(command: String): Boolean = ALLOWED.matches(command)
}
