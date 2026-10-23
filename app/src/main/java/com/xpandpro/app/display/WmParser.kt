package com.xpandpro.app.display

object WmParser {
    private val physical = Regex("""Physical size:\s*(\d+)x(\d+)""")
    private val override = Regex("""Override size:\s*(\d+)x(\d+)""")

    private val physicalDensity = Regex("""Physical density:\s*(\d+)""")
    private val overrideDensity = Regex("""Override density:\s*(\d+)""")

    fun parseDensity(output: String): DensityState? {
        val p = physicalDensity.find(output) ?: return null
        val o = overrideDensity.find(output)
        return DensityState(p.groupValues[1].toInt(), o?.groupValues?.get(1)?.toInt())
    }

    fun parseSize(output: String): DisplayState? {
        val p = physical.find(output) ?: return null
        val o = override.find(output)
        return DisplayState(
            Resolution(p.groupValues[1].toInt(), p.groupValues[2].toInt()),
            o?.let { Resolution(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        )
    }
}
