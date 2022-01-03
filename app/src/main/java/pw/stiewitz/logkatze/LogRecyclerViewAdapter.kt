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

    interface Callback {
        fun itemAdded(entry: LogEntry)
    }

    var callbacks: Callback? = null

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
        callbacks?.itemAdded(entry)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val priorityView: TextView = itemView.findViewById(R.id.priority)
        val componentView: TextView = itemView.findViewById(R.id.component)
        val processView: TextView = itemView.findViewById(R.id.process)
        val textView: TextView = itemView.findViewById(R.id.text)

        init {
            itemView.setOnClickListener {
                val item = items[adapterPosition]
                val view = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.dialog_entry, null, false)

                view.findViewById<TextView>(R.id.priority).let {
                    it.text = item.priority.name.first().toString()
                    when (item.priority) {
                        LogEntry.Priority.VERBOSE -> {
                            it.setBackgroundColor(Color.GRAY)
                            it.setTextColor(Color.BLACK)
                        }
                        LogEntry.Priority.DEBUG -> {
                            it.setBackgroundColor(Color.BLUE)
                            it.setTextColor(Color.WHITE)
                        }
                        LogEntry.Priority.INFO -> {
                            it.setBackgroundColor(Color.WHITE)
                            it.setTextColor(Color.BLACK)
                        }
                        LogEntry.Priority.WARNING -> {
                            it.setBackgroundColor(Color.YELLOW)
                            it.setTextColor(Color.BLACK)
                        }
                        LogEntry.Priority.ERROR -> {
                            it.setBackgroundColor(Color.RED)
                            it.setTextColor(Color.WHITE)
                        }
                        LogEntry.Priority.FATAL -> {
                            it.setBackgroundColor(Color.RED)
                            it.setTextColor(Color.WHITE)
                        }
                    }
                }
                view.findViewById<TextView>(R.id.processName).let {
                    it.text = item.process ?: "PID: ${item.pid.toString()}"
                }
                view.findViewById<TextView>(R.id.component).text = item.component
                view.findViewById<TextView>(R.id.content).text = item.text.joinToString("\n")

                AlertDialog.Builder(itemView.context)
                    .setView(view)
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
        holder.priorityView.text = when (items[position].priority) {
            LogEntry.Priority.VERBOSE -> "verbose"
            LogEntry.Priority.DEBUG -> "debug"
            LogEntry.Priority.INFO -> "Info"
            LogEntry.Priority.WARNING -> "Warning"
            LogEntry.Priority.ERROR -> "ERROR"
            LogEntry.Priority.FATAL -> "FATAL"
        }
        holder.priorityView.setTextColor(
            when (items[position].priority) {
                LogEntry.Priority.VERBOSE -> Color.GRAY
                LogEntry.Priority.DEBUG -> Color.BLUE
                LogEntry.Priority.INFO -> Color.BLACK
                LogEntry.Priority.WARNING -> Color.MAGENTA
                LogEntry.Priority.ERROR -> Color.RED
                LogEntry.Priority.FATAL -> Color.RED
            }
        )
        holder.componentView.text = items[position].component
        holder.processView.text = items[position].process ?: ""
        holder.processView.visibility =
            if (items[position].process != null) View.VISIBLE else View.GONE
        holder.textView.text = items[position].text.firstOrNull() ?: "No message"
    }

    override fun getItemCount() = items.size
    fun setItems(it: ArrayList<LogEntry>) {
        items = it
        notifyDataSetChanged()
    }

    fun clean() {
        items.clear()
        notifyDataSetChanged()
    }
}