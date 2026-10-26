package com.perimenopauseintelligence.app.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var welcomeSeen: Boolean
        get() = prefs.getBoolean("welcome_seen", false)
        set(v) = prefs.edit().putBoolean("welcome_seen", v).apply()

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_enabled", false)
        set(v) = prefs.edit().putBoolean("reminder_enabled", v).apply()

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(v) = prefs.edit().putInt("reminder_hour", v).apply()

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(v) = prefs.edit().putInt("reminder_minute", v).apply()

    fun clear() = prefs.edit().clear().apply()
}
