/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConstLogFragment : Fragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: LogRecyclerViewAdapter

    var notificationHash: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            notificationHash = it.getInt("by-rule-hash")
        }
    }

    override fun onResume() {
        super.onResume()
        if (notificationHash != 0) {
            (requireContext().applicationContext as MyApplication).rootLogCatService?.entriesByNotificationId(
                notificationHash
            )?.let {
                adapter.setItems(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log, container, false)

        recyclerView = view.findViewById(R.id.logRecycler)

        adapter = LogRecyclerViewAdapter()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        return view
    }
}