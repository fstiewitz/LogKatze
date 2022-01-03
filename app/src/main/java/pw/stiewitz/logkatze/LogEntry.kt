/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

data class LogEntry(
    val time: Double,
    val pid: Int,
    val tid: Int,
    val priority: Priority,
    val component: String,
    var text: ArrayList<String>
) {

    enum class Priority {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        FATAL
    }

    companion object {
        fun fromLine(line: String): LogEntry {
            return if (line.startsWith("[") && line.endsWith("]")) {
                parseMetaLine(line.slice(1..line.length - 2)).let { entry ->
                    line.slice(line.indexOf("]") + 1 until line.length).trim().let {
                        if (it.isNotEmpty()) entry.text.add(it)
                    }
                    entry
                }
            } else return fallbackParse(line)

        }

        private fun fallbackParse(line: String): LogEntry {
            return LogEntry(0.0, 0, 0, Priority.VERBOSE, "", arrayListOf(line))
        }

        private val metaRegex =
            Regex("\\s+(\\d+\\.?\\d*)\\s+(\\d+):\\s?(\\d+)\\s+(\\w+)/(\\w+)\\s*")

        private fun parseMetaLine(line: String): LogEntry {
            return metaRegex.matchEntire(line)?.let { match ->
                LogEntry(
                    match.groupValues[1].toDouble(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                    when (match.groupValues[4]) {
                        "V" -> Priority.VERBOSE
                        "D" -> Priority.DEBUG
                        "I" -> Priority.INFO
                        "W" -> Priority.WARNING
                        "E" -> Priority.ERROR
                        "F" -> Priority.FATAL
                        else -> Priority.VERBOSE
                    },
                    match.groupValues[5],
                    ArrayList()
                )
            } ?: fallbackParse(line)
        }
    }
}
