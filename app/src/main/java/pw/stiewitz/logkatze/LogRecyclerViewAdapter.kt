/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class LogRecyclerViewAdapter : RecyclerView.Adapter<LogRecyclerViewAdapter.ViewHolder>() {
    private var items: ArrayList<LogEntry> = ArrayList()
    var discardOld: Boolean = true

    fun addItem(entry: LogEntry) {
        if (items.isNotEmpty() && items.last() == entry) return
        if (discardOld && items.size > 100) {
            items.removeAt(0)
            items.add(entry)
            notifyDataSetChanged()
        } else {
            items.add(entry)
            notifyItemInserted(items.size - 1)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val priorityView: TextView = itemView.findViewById(R.id.priority)
        val componentView: TextView = itemView.findViewById(R.id.component)
        val textView: TextView = itemView.findViewById(R.id.text)

        init {
            textView.setOnClickListener {
                val item = items[adapterPosition]
                AlertDialog.Builder(itemView.context)
                    .setMessage(item.text.joinToString("\n"))
                    .setTitle("${item.pid}:${item.tid} - ${item.component}")
                    .setPositiveButton("OK") { _, _ -> }
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false).let {
            ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.priorityView.text = when(items[position].priority) {
            LogEntry.Priority.VERBOSE -> "verbose"
            LogEntry.Priority.DEBUG -> "debug"
            LogEntry.Priority.INFO -> "Info"
            LogEntry.Priority.WARNING -> "Warning"
            LogEntry.Priority.ERROR -> "ERROR"
            LogEntry.Priority.FATAL -> "FATAL"
        }
        holder.priorityView.setTextColor(when(items[position].priority) {
            LogEntry.Priority.VERBOSE -> Color.GRAY
            LogEntry.Priority.DEBUG -> Color.BLUE
            LogEntry.Priority.INFO -> Color.BLACK
            LogEntry.Priority.WARNING -> Color.MAGENTA
            LogEntry.Priority.ERROR -> Color.RED
            LogEntry.Priority.FATAL -> Color.RED
        })
        holder.componentView.text = items[position].component
        holder.textView.text = items[position].text.firstOrNull() ?: "No message"
    }

    override fun getItemCount() = items.size
}