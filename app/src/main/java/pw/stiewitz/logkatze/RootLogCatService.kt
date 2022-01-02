/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.EventLog
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException

class RootLogCatService : Service(), Runnable {
    private var thread: Thread? = null
    lateinit var notification: Notification

    interface Callback {
        fun rawLine(s: String, isInitial: Boolean): Boolean
        fun logged(e: LogEntry, isInitial: Boolean)
        fun error(e: Exception)
    }

    override fun onCreate() {
        thread = Thread(this).also {
            it.start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        notification = NotificationCompat.Builder(this, "logCatServiceRunning")
            .setContentTitle("LogKatze")
            .setContentText("LogKatze is running")
            .setSmallIcon(R.drawable.baseline_text_snippet_black_24dp)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .build()

        startForeground(1, notification)

        (application as MyApplication).rootLogCatService = this

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun run() {
        val app = applicationContext as MyApplication
        try {
            val reader = ProcessBuilder("su", "-c", "logcat -v long,printable,epoch").let {
            //val reader = ProcessBuilder("logcat", "-v", "long,printable,epoch").let {
                it.start()
            }.inputStream.bufferedReader()
            val startTime = System.currentTimeMillis()
            var currentEntry: LogEntry? = null
            while (true) {
                val line = reader.readLine().trim()
                app.rootLogCallback?.let {
                    val initial = System.currentTimeMillis() < startTime + 3 * 1000
                    if(!it.rawLine(line, initial)) {
                        if(currentEntry == null) {
                            currentEntry = LogEntry.fromLine(line)
                            if(currentEntry!!.time == 0.0) {
                                currentEntry = null
                            }
                        } else if(line.isNotEmpty()) {
                            currentEntry!!.text.add(line)
                        } else {
                            it.logged(currentEntry!!, initial)
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
}