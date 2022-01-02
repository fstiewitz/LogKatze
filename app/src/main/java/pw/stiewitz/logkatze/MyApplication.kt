/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.lifecycle.MutableLiveData

class MyApplication : Application() {
    var rootLogCatService: RootLogCatService? = null
    var rootLogCallback: RootLogCatService.Callback? = null
    var lastError = MutableLiveData<Exception?>(null)

    override fun onCreate() {
        super.onCreate()

        buildNotificationChannel()

        Intent(this, RootLogCatService::class.java).also { intent ->
            applicationContext.startForegroundService(intent)
        }
    }

    private fun buildNotificationChannel() {
        NotificationChannel(
            "logCatServiceRunning",
            "LogKatze Reader Service",
            NotificationManager.IMPORTANCE_DEFAULT
        ).let {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                it
            )
        }
    }
}