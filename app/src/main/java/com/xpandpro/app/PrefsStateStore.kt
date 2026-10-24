package com.xpandpro.app

import android.content.Context
import com.xpandpro.app.display.StateStore
import com.xpandpro.app.display.Tweak

/** Resultado de la prueba de compatibilidad (la hace cada usuario en su teléfono). */
enum class StretchSupport { UNKNOWN, YES, NO }

class PrefsStateStore(context: Context) : StateStore {
    private val prefs = context.getSharedPreferences("xpand_state", Context.MODE_PRIVATE)

    // commit() (síncrono): el estado debe estar en disco antes de tocar la pantalla.
    override var stretched: Boolean
        get() = prefs.getBoolean("stretched", false)
        set(v) { prefs.edit().putBoolean("stretched", v).commit() }

    override var originalOverride: String?
        get() = prefs.getString("original_override", null)
        set(v) { prefs.edit().putString("original_override", v).commit() }

    override var originalDensity: String?
        get() = prefs.getString("original_density", null)
        set(v) { prefs.edit().putString("original_density", v).commit() }

    override var densityChanged: Boolean
        get() = prefs.getBoolean("density_changed", false)
        set(v) { prefs.edit().putBoolean("density_changed", v).commit() }

    override var scalingChanged: Boolean
        get() = prefs.getBoolean("scaling_changed", false)
        set(v) { prefs.edit().putBoolean("scaling_changed", v).commit() }

    override var projectionChanged: Boolean
        get() = prefs.getBoolean("projection_changed", false)
        set(v) { prefs.edit().putBoolean("projection_changed", v).commit() }

    /** Ajustes extra con los que la prueba de compatibilidad vio que SÍ estira. */
    var stretchTweaks: Set<Tweak>
        get() = (prefs.getString("stretch_tweaks", "") ?: "").split(",")
            .mapNotNull { n -> Tweak.values().firstOrNull { it.name == n } }.toSet()
        set(v) { prefs.edit().putString("stretch_tweaks", v.joinToString(",") { it.name }).apply() }

    /** ¿Este teléfono estira la imagen (YES) o solo deja barras negras (NO)? */
    var stretchSupport: StretchSupport
        get() = runCatching { StretchSupport.valueOf(prefs.getString("stretch_support", null) ?: "") }
            .getOrDefault(StretchSupport.UNKNOWN)
        set(v) { prefs.edit().putString("stretch_support", v.name).apply() }

    /** Resultado de la prueba de proyección directa. */
    var projectionSupport: StretchSupport
        get() = runCatching { StretchSupport.valueOf(prefs.getString("projection_support", null) ?: "") }
            .getOrDefault(StretchSupport.UNKNOWN)
        set(v) { prefs.edit().putString("projection_support", v.name).apply() }

    /** Multiplicador elegido, en centésimas (120 = 1.20x). */
    var multiplierPercent: Int
        get() = prefs.getInt("multiplier_percent", Config.DEFAULT_PERCENT)
            .coerceIn(Config.MIN_PERCENT, Config.MAX_PERCENT)
        set(v) { prefs.edit().putInt("multiplier_percent", v).apply() }
}
