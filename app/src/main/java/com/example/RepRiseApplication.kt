package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserSettingsStore
import com.example.data.repository.PushUpRepository
import com.example.notifications.ReminderScheduler

class RepRiseApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsStore: UserSettingsStore by lazy { UserSettingsStore(this) }
    val repository: PushUpRepository by lazy {
        PushUpRepository(database.pushUpDayDao(), settingsStore)
    }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)
    }
}
