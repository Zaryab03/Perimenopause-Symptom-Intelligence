package com.perimenopauseintelligence.app

import android.app.Application
import android.content.Context
import com.perimenopauseintelligence.app.data.AppDatabase
import com.perimenopauseintelligence.app.data.SettingsStore
import com.perimenopauseintelligence.app.data.TrackerRepository
import com.perimenopauseintelligence.app.reminders.ReminderScheduler
import com.perimenopauseintelligence.app.share.ExportFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: AppDatabase = AppDatabase.create(appContext)
    val repo = TrackerRepository(db)
    val settings = SettingsStore(appContext)
    val reminders = ReminderScheduler(appContext, settings)

    /** The day shown on the Today screen. The History screen sets this when a day is tapped. */
    val selectedDate = MutableStateFlow(LocalDate.now())

    init {
        scope.launch { repo.seedDefaults() }
    }
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
