package com.xpandpro.app

import android.content.Context
import com.xpandpro.app.display.StateStore
import com.xpandpro.app.display.StretchPreset

class PrefsStateStore(context: Context) : StateStore {
    private val prefs = context.getSharedPreferences("xpand_state", Context.MODE_PRIVATE)

    // commit() (síncrono): el estado debe estar en disco antes de tocar la pantalla.
    override var stretched: Boolean
        get() = prefs.getBoolean("stretched", false)
        set(v) { prefs.edit().putBoolean("stretched", v).commit() }

    override var originalOverride: String?
        get() = prefs.getString("original_override", null)
        set(v) { prefs.edit().putString("original_override", v).commit() }

    var preset: StretchPreset
        get() = StretchPreset.fromName(prefs.getString("preset", null))
        set(v) { prefs.edit().putString("preset", v.name).apply() }
}
