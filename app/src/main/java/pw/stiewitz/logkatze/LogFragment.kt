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

class LogFragment : Fragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: LogRecyclerViewAdapter

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).let { mainActivity ->
            mainActivity.logFragment = this
            (mainActivity.applicationContext as MyApplication).lastError.observe(this) { exception ->
                exception?.message?.let {
                    adapter.addItem(LogEntry(it))
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).logFragment = null
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