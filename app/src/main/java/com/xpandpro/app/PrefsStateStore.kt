package com.xpandpro.app

import android.content.Context
import com.xpandpro.app.display.StateStore

class PrefsStateStore(context: Context) : StateStore {
    private val prefs = context.getSharedPreferences("xpand_state", Context.MODE_PRIVATE)

    // commit() (síncrono): el estado debe estar en disco antes de tocar la pantalla.
    override var stretched: Boolean
        get() = prefs.getBoolean("stretched", false)
        set(v) { prefs.edit().putBoolean("stretched", v).commit() }

    override var originalOverride: String?
        get() = prefs.getString("original_override", null)
        set(v) { prefs.edit().putString("original_override", v).commit() }

    /** Multiplicador elegido, en centésimas (120 = 1.20x). */
    var multiplierPercent: Int
        get() = prefs.getInt("multiplier_percent", Config.DEFAULT_PERCENT)
            .coerceIn(Config.MIN_PERCENT, Config.MAX_PERCENT)
        set(v) { prefs.edit().putInt("multiplier_percent", v).apply() }
}
