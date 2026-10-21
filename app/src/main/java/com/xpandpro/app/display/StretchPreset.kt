package com.xpandpro.app.display

/** Proporción lado largo : lado corto que se quiere obtener. */
enum class StretchPreset(val label: String, val num: Int, val den: Int) {
    R16_9("16:9", 16, 9),
    R16_10("16:10", 16, 10),
    R4_3("4:3", 4, 3),
    R5_4("5:4", 5, 4);

    companion object {
        val DEFAULT = R4_3
        fun fromName(name: String?): StretchPreset = values().firstOrNull { it.name == name } ?: DEFAULT
    }
}
