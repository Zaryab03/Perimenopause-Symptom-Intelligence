package com.perimenopauseintelligence.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import com.perimenopauseintelligence.app.data.AppDatabase
import com.perimenopauseintelligence.app.data.SettingsStore
import com.perimenopauseintelligence.app.data.TrackerRepository
import com.perimenopauseintelligence.app.reminders.ReminderScheduler
import com.perimenopauseintelligence.app.share.ExportFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface UnlockResult {
    data object Success : UnlockResult
    data object Wrong : UnlockResult
    data class TryLater(val seconds: Int) : UnlockResult
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: AppDatabase = AppDatabase.create(appContext)
    val repo = TrackerRepository(db)
    val settings = SettingsStore(appContext)
    val reminders = ReminderScheduler(appContext, settings)

    /** The day shown on the Today screen. The History screen sets this when a day is opened. */
    val selectedDate = MutableStateFlow(LocalDate.now())

    private val _locked = MutableStateFlow(settings.pinEnabled.value)
    val locked: StateFlow<Boolean> = _locked
    private var failedAttempts = 0
    private var blockedUntil = 0L
    private var backgroundedAt = 0L

    init {
        scope.launch { repo.seedDefaults() }
    }

    fun tryUnlock(pin: String): UnlockResult {
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntil) return UnlockResult.TryLater(((blockedUntil - now) / 1000).toInt() + 1)
        if (settings.verifyPin(pin)) {
            failedAttempts = 0
            _locked.value = false
            return UnlockResult.Success
        }
        failedAttempts++
        if (failedAttempts >= 5) {
            blockedUntil = now + 30_000L * (failedAttempts - 4)
            return UnlockResult.TryLater(30 * (failedAttempts - 4))
        }
        return UnlockResult.Wrong
    }

    fun lockNow() { if (settings.pinEnabled.value) _locked.value = true }
    fun onBackgrounded() { backgroundedAt = SystemClock.elapsedRealtime() }
    fun onForegrounded() {
        if (settings.pinEnabled.value && backgroundedAt != 0L &&
            SystemClock.elapsedRealtime() - backgroundedAt > AUTO_LOCK_MS
        ) _locked.value = true
    }
    fun unlockAfterReset() { _locked.value = false }

    companion object { const val AUTO_LOCK_MS = 30_000L }
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
        container.reminders.reschedule()
        ExportFiles.cleanup(this)
    }
}
