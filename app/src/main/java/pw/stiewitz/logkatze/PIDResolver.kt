/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.content.Context
import java.util.*
import kotlin.collections.HashMap

class PIDResolver(val context: Context, val onResolved: (LogEntry) -> Unit) {
    private var pidToAppName: Map<Int, String> = HashMap()
    private val psProcess = ProcessBuilder("su").start()
    private val output = psProcess.outputStream.writer()
    private val input = psProcess.inputStream.bufferedReader()

    init {
        output.write("PS1=X\n")
        output.flush()
    }

    private fun readPS(): ArrayList<String> {
        output.write("ps -A -o PID,NAME;echo XX\n")
        output.flush()
        val out = ArrayList<String>()
        while (true) {
            val s = input.readLine().trim()
            if (s == "XX") break
            out.add(s)
        }
        return out
    }

    private fun reload() {
        pidToAppName = readPS().map {
            if (it.startsWith(" ")) listOf()
            else it.split(" ")
        }.filter { it.size == 2 }.mapNotNull { s ->
            s[0].toIntOrNull()?.let {
                Pair(it, s[1])
            }
        }
            .toMap()
    }

    fun resolveEntry(entry: LogEntry) {
        if (pidToAppName.containsKey(entry.pid)) {
            entry.process = pidToAppName[entry.pid]
        } else {
            reload()
            entry.process = pidToAppName[entry.pid]
        }
        onResolved(entry)
    }
}