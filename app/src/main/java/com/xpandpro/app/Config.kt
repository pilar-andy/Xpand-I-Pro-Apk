package com.xpandpro.app

object Config {
    /** Paquete de Blood Strike (NetEase). Verificar con el juego instalado; cambiar aquí si difiere. */
    const val BLOOD_STRIKE_PACKAGE = "com.netease.newspike"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /** Multiplicador en centésimas: 105 = 1.05x ... 170 = 1.70x. */
    const val MIN_PERCENT = 105
    const val MAX_PERCENT = 170
    const val DEFAULT_PERCENT = 130

    /** Segundos para confirmar tras activar; si no se confirma, se restaura solo. */
    const val CONFIRM_SECONDS = 15
}
