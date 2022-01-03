/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import java.io.IOException
import javax.inject.Inject

class RootLogCatService : Service(), Runnable {
    @Inject
    lateinit var logKatzeDatabase: LogKatzeDatabase
    private var thread: Thread? = null
    private lateinit var notification: Notification

    private var rules: List<CompiledRule> = ArrayList()
    private var initial: Boolean = true

    private val pidResolver = PIDResolver(this) {
        (applicationContext as MyApplication).rootLogCallback?.logged(it, initial)
        processRules(it)
    }

    data class CompiledRule(val rule: NotificationRule) {
        val contentRegex: Regex? =
            if (rule.contentRegex.isNotEmpty()) Regex(rule.contentRegex) else null
        val componentRegex: Regex? = if (rule.component.isNotEmpty()) Regex(
            rule.component.replace(".", "\\.").replace("*", ".*")
        ) else null
        val processRegex: Regex? = if (rule.process.isNotEmpty()) Regex(
            rule.process.replace(".", "\\.").replace("*", ".*")
        ) else null
        val priority: LogEntry.Priority = when (rule.priority) {
            "verbose" -> LogEntry.Priority.VERBOSE
            "debug" -> LogEntry.Priority.DEBUG
            "info" -> LogEntry.Priority.INFO
            "warning" -> LogEntry.Priority.WARNING
            "error" -> LogEntry.Priority.ERROR
            "fatal" -> LogEntry.Priority.FATAL
            else -> LogEntry.Priority.VERBOSE
        }
    }

    interface Callback {
        fun rawLine(s: String, isInitial: Boolean): Boolean
        fun logged(e: LogEntry, isInitial: Boolean)
        fun error(e: Exception)
    }

    override fun onCreate() {
        (applicationContext as MyApplication).appComponent.inject(this)
        thread = Thread(this).also {
            it.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pidResolver.stop()
        (applicationContext as MyApplication).let {
            it.rootLogCatService = null
            it.serviceIsAlive.postValue(false)
        }
    }

    private fun startService() {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        notification = NotificationCompat.Builder(this, "logCatServiceRunning")
            .setContentTitle("LogKatze")
            .setContentText("LogKatze is running")
            .setSmallIcon(R.drawable.baseline_text_snippet_black_24dp)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.baseline_cancel_black_24dp, "Stop", stopPendingIntent())
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .build()

        startForeground(1, notification)

        (application as MyApplication).let {
            it.rootLogCatService = this
            it.serviceIsAlive.postValue(true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startService()
                START_STICKY
            }
            ACTION_STOP -> {
                stopSelf()
                stopForeground(true)
                START_NOT_STICKY
            }
            else -> {
                startService()
                START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun run() {
        val app = applicationContext as MyApplication
        try {
            updateRules()
            val reader = ProcessBuilder("su", "-c", "logcat -v long,printable,epoch").let {
            //val reader = ProcessBuilder("logcat", "-v", "long,printable,epoch").let {
                it.start()
            }.inputStream.bufferedReader()
            val startTime = System.currentTimeMillis()
            var currentEntry: LogEntry? = null
            while (true) {
                val line = reader.readLine().trim()
                app.rootLogCallback?.let { callback ->
                    val currentTime = System.currentTimeMillis()
                    initial = currentTime < startTime + 3 * 1000
                    when {
                        currentEntry == null -> {
                            currentEntry = LogEntry.fromLine(line).let {
                                if (it.time == 0.0) null
                                else it
                            }
                        }
                        line.isNotEmpty() -> {
                            currentEntry!!.text.add(line)
                        }
                        else -> {
                            if(!initial) pidResolver.scheduleEntryResolve(currentEntry!!)
                            currentEntry = null
                        }
                    }
                }
            }
        } catch (e: IOException) {
            (applicationContext as MyApplication).lastError.postValue(e)
            app.rootLogCallback?.error(e)
        }
    }

    private val notifications: HashMap<NotificationRule, NotificationData> = HashMap()

    fun entriesByNotificationId(id: Int): ArrayList<LogEntry>? {
        return notifications.firstNotNullOf {
            if (it.key.hashCode() == id) it.value.entries
            else null
        }
    }

    class NotificationData(
        val notificationCompat: NotificationCompat.Builder,
        val text: String,
        val id: Int,
        val entries: ArrayList<LogEntry>
    )

    private fun browseEntriesIntent(rule: NotificationRule): PendingIntent {
        return NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.constLogFragment)
            .setArguments(Bundle().apply {
                putInt("by-rule-hash", rule.hashCode())
            })
            .createPendingIntent()
    }

    private fun stopPendingIntent(): PendingIntent {
        return stopIntent(this).let {
            PendingIntent.getService(this, 0, it, 0)
        }
    }

    private fun notify(logEntry: LogEntry, rule: NotificationRule) {
        val notification = notifications.getOrPut(rule) {
            NotificationCompat.Builder(applicationContext, "logCatNotifications")
                .setContentTitle(rule.getNiceName())
                .setContentText(logEntry.text.firstOrNull() ?: "")
                .setContentIntent(browseEntriesIntent(rule))
                .setSmallIcon(R.drawable.baseline_notification_important_black_24dp)
                .let {
                    NotificationData(
                        it,
                        logEntry.text.firstOrNull() ?: "",
                        it.hashCode(),
                        ArrayList()
                    )
                }
        }
        notification.entries.add(logEntry)
        logEntry.text.firstOrNull()?.let {
            notification.notificationCompat.setContentText(notification.text + "\n" + it)
            NotificationManagerCompat.from(applicationContext)
                .notify(notification.id, notification.notificationCompat.build())
        }
    }

    private fun processRules(logEntry: LogEntry) {
        for (rule in rules) {
            var processMatch = rule.processRegex == null
            var componentMatch = true
            var contentMatch = true
            val priorityMatch =
                if (rule.rule.priority.isNotEmpty()) rule.priority == logEntry.priority else true
            logEntry.process?.let { process ->
                rule.processRegex?.matches(process)?.let {
                    processMatch = it
                }
            }
            rule.componentRegex?.matches(logEntry.component)?.let {
                componentMatch = it
            }
            rule.contentRegex?.matches(logEntry.text.joinToString("\n"))?.let {
                contentMatch = it
            }
            if (processMatch && componentMatch && contentMatch && priorityMatch) {
                notify(logEntry, rule.rule)
            }
        }
    }

    fun updateRules() {
        rules = logKatzeDatabase.notificationRuleDao().getAll().map {
            CompiledRule(it)
        }
    }

    companion object {
        const val ACTION_START = "start-service"
        const val ACTION_STOP = "stop-service"

        fun startIntent(context: Context): Intent {
            return Intent(context, RootLogCatService::class.java).also { intent ->
                intent.action = ACTION_START
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, RootLogCatService::class.java).also { intent ->
                intent.action = ACTION_STOP
            }
        }
    }
}