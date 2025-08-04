package com.leeweeder.greasethegroove

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.leeweeder.greasethegroove.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GtGApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GtGApplication)
            modules(appModule)
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Workout Reminders"
        val descriptionText = "Notifications for GtG sets"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "GTG_WORKOUT_CHANNEL"
    }
}