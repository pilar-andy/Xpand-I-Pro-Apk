package com.xpandpro.app

import com.xpandpro.app.shizuku.ShizukuManager

/** Punto de acceso compartido a la conexión con Shizuku (la crea MainActivity). */
object AppServices {
    @Volatile var shizuku: ShizukuManager? = null
}
