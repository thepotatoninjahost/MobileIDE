package com.fuck13.mobileide

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.fuck13.mobileide.data.preferences.UserPreferences

class MobileIDEApp : Application() {

    companion object {
        const val CHANNEL_ID_COMPILER = "compiler_service"
        const val CHANNEL_ID_TERMINAL = "terminal_output"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize preferences
        UserPreferences.init(this)
        
        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val compilerChannel = NotificationChannel(
                CHANNEL_ID_COMPILER,
                "Compiler Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows compilation progress and results"
                setShowBadge(false)
            }

            val terminalChannel = NotificationChannel(
                CHANNEL_ID_TERMINAL,
                "Terminal Output",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Terminal process notifications"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(compilerChannel, terminalChannel))
        }
    }
}
