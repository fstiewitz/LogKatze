/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.PendingIntent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LogFragment : Fragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: LogRecyclerViewAdapter
    private lateinit var powerButton: FloatingActionButton
    private lateinit var scrollButton: FloatingActionButton

    private var startTime: Long = 0

    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
        (requireActivity() as MainActivity).let { mainActivity ->
            mainActivity.logFragment = this
            (mainActivity.applicationContext as MyApplication).let { myApplication ->
                myApplication.lastError.observe(this) { exception ->
                    exception?.message?.let {
                        adapter.addItem(LogEntry.fromLine(it))
                    }
                }
                myApplication.serviceIsAlive.observe(this) {
                    powerButton.visibility = if (it) View.GONE else View.VISIBLE
                    if (System.currentTimeMillis() > startTime + 5 * 1000) {
                        Toast.makeText(
                            requireContext(),
                            if (it) "Service Active" else "Service Stopped",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
        powerButton = view.findViewById(R.id.powerButton)
        scrollButton = view.findViewById(R.id.scrollDownButton)

        powerButton.setOnClickListener {
            adapter.clean()
            RootLogCatService.startIntent(requireContext()).let {
                PendingIntent.getService(requireContext(), 0, it, 0)
            }.send()
        }
        powerButton.visibility =
            if ((requireContext().applicationContext as MyApplication).rootLogCatService != null) View.GONE else View.VISIBLE

        scrollButton.setOnClickListener {
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }

        recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
            val b =
                (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() < adapter.itemCount - 20
            scrollButton.visibility = if (b && adapter.itemCount > 30) View.VISIBLE else View.GONE
        }

        adapter = LogRecyclerViewAdapter().also {
            it.callbacks = object : LogRecyclerViewAdapter.Callback {
                override fun itemAdded(entry: LogEntry) {
                    if ((recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == adapter.itemCount - 2) {
                        recyclerView.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        return view
    }
}