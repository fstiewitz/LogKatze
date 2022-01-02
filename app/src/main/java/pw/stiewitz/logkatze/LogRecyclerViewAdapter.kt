/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

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
        if (discardOld && items.size > 10) {
            items.removeAt(0)
            items.add(entry)
            notifyDataSetChanged()
        } else {
            items.add(entry)
            notifyItemInserted(items.size - 1)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false).let {
            ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].text
    }

    override fun getItemCount() = items.size
}