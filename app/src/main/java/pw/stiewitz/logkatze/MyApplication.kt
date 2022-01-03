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
import dagger.BindsInstance
import dagger.Component
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component(modules = [LogKatzeDatabaseModule::class])
interface ApplicationComponent {
    fun inject(notificationRuleListFragment: NotificationRuleListFragment)
    fun inject(rootLogCatService: RootLogCatService)
    fun inject(myApplication: MyApplication)

    fun logKatzeDatabase(): LogKatzeDatabase

    @Component.Builder
    interface Builder {
        fun build(): ApplicationComponent

        @BindsInstance
        fun application(app: MyApplication): Builder
    }
}

class MyApplication : Application() {
    @Inject
    lateinit var appComponent: ApplicationComponent

    var rootLogCatService: RootLogCatService? = null
    var rootLogCallback: RootLogCatService.Callback? = null
    var lastError = MutableLiveData<Exception?>(null)
    var serviceIsAlive = MutableLiveData(false)

    override fun onCreate() {
        super.onCreate()
        DaggerApplicationComponent.builder().application(this).build().inject(this)

        buildNotificationChannel()

        Intent(this, RootLogCatService::class.java).also { intent ->
            intent.action = RootLogCatService.ACTION_START
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
        NotificationChannel(
            "logCatNotifications",
            "LogKatze Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).let {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                it
            )
        }
    }
}