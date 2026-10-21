package com.xpandpro.app

object Config {
    /** Paquete de Blood Strike (NetEase). Verificar con el juego instalado; cambiar aquí si difiere. */
    const val BLOOD_STRIKE_PACKAGE = "com.netease.newspike"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /** Multiplicador en centésimas: 101 = 1.01x ... 130 = 1.30x. */
    const val MIN_PERCENT = 101
    const val MAX_PERCENT = 130
    const val DEFAULT_PERCENT = 120

    /** Segundos para confirmar tras activar; si no se confirma, se restaura solo. */
    const val CONFIRM_SECONDS = 15
}
