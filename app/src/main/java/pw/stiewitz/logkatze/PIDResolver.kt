/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import java.util.*
import kotlin.collections.HashMap

class PIDResolver(val context: Context, val onResolved: (LogEntry) -> Unit) {
    private var pidToAppName: Map<Int, String> = HashMap()
    private var entryQueue: Queue<LogEntry> = LinkedList()
    private val timer = Timer("PIDResolver").also {
        it.schedule(object : TimerTask() {
            override fun run() {
                reload()
                while (true) {
                    val e = entryQueue.poll() ?: break
                    resolveEntry(e)
                    onResolved(e)
                }
            }
        }, 1000, 5 * 1000L)
    }

    fun stop() {
        timer.cancel()
    }

    fun reload() {
        val manager = context.getSystemService(Application.ACTIVITY_SERVICE) as ActivityManager
        pidToAppName = manager.runningAppProcesses.map {
            Pair(it.pid, it.processName)
        }.toMap()
    }

    fun resolveEntry(entry: LogEntry) {
        pidToAppName[entry.pid]?.let {
            entry.process = it
        }
    }

    fun scheduleEntryResolve(entry: LogEntry) {
        entryQueue.offer(entry)
    }
}