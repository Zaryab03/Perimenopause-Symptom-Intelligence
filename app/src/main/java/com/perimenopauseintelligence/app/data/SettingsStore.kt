package com.perimenopauseintelligence.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PeriodStatus(val label: String, val hint: String) {
    REGULAR("Yes, fairly regular", "Your cycle is mostly predictable"),
    IRREGULAR("Yes, but irregular", "Timing, length or flow is changing"),
    NONE("No periods", "Stopped, or never applied to me"),
    SKIP("Prefer not to say", "We'll still show the optional question"),
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "").orEmpty())
    val userName: StateFlow<String> = _userName
    fun setUserName(v: String) { val n = v.trim().take(40); prefs.edit().putString("user_name", n).apply(); _userName.value = n }

    private val _periodStatus = MutableStateFlow(
        runCatching { PeriodStatus.valueOf(prefs.getString("period_status", "SKIP").orEmpty()) }.getOrDefault(PeriodStatus.SKIP),
    )
    val periodStatus: StateFlow<PeriodStatus> = _periodStatus
    fun setPeriodStatus(v: PeriodStatus) { prefs.edit().putString("period_status", v.name).apply(); _periodStatus.value = v }

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone
    fun setOnboardingDone(v: Boolean) { prefs.edit().putBoolean("onboarding_done", v).apply(); _onboardingDone.value = v }

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_enabled", false)
        set(v) = prefs.edit().putBoolean("reminder_enabled", v).apply()

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(v) = prefs.edit().putInt("reminder_hour", v).apply()

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(v) = prefs.edit().putInt("reminder_minute", v).apply()

    private val _pinEnabled = MutableStateFlow(prefs.contains("pin_hash"))
    val pinEnabled: StateFlow<Boolean> = _pinEnabled

    fun setPin(pin: String) {
        val salt = PinCodec.newSalt()
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(PinCodec.hash(pin, salt), Base64.NO_WRAP))
            .apply()
        _pinEnabled.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString("pin_salt", null) ?: return false
        val hash = prefs.getString("pin_hash", null) ?: return false
        return PinCodec.matches(pin, Base64.decode(salt, Base64.NO_WRAP), Base64.decode(hash, Base64.NO_WRAP))
    }

    fun clearPin() {
        prefs.edit().remove("pin_salt").remove("pin_hash").apply()
        _pinEnabled.value = false
    }

    /** Wipes every setting, including the PIN, and returns the app to first-run state. */
    fun clear() {
        prefs.edit().clear().apply()
        _userName.value = ""
        _periodStatus.value = PeriodStatus.SKIP
        _onboardingDone.value = false
        _pinEnabled.value = false
    }
}
