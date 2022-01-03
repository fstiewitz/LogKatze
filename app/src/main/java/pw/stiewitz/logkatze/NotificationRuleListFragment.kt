/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotificationRuleListFragment : Fragment() {
    @Inject
    lateinit var logKatzeDatabase: LogKatzeDatabase

    lateinit var notificationRecyclerView: RecyclerView

    override fun onAttach(context: Context) {
        (context.applicationContext as MyApplication).appComponent.inject(this)
        super.onAttach(context)
    }

    fun reload() {
        CoroutineScope(Dispatchers.IO).launch {
            val rules = logKatzeDatabase.notificationRuleDao().getAll()
            withContext(Dispatchers.Main) {
                notificationRecyclerView.swapAdapter(RuleRecyclerAdapter(rules), true)
            }
        }
    }

    fun makeDialog(notificationRule: NotificationRule?) {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rule, null, false)

        val processEditText = v.findViewById<AutoCompleteTextView>(R.id.processText)
        val componentEditText = v.findViewById<EditText>(R.id.componentText)
        val priorityEditText = v.findViewById<EditText>(R.id.priorityText)
        val contentText = v.findViewById<EditText>(R.id.contentText)

        requireActivity().packageManager.getInstalledPackages(0).map {
            it.packageName
        }.let {
            ArrayAdapter(requireContext(), R.layout.item_package, it)
        }.let {
            processEditText.setAdapter(it)
        }

        notificationRule?.let {
            processEditText.setText(it.process)
            componentEditText.setText(it.component)
            priorityEditText.setText(it.priority)
            contentText.setText(it.contentRegex)
        }

        AlertDialog.Builder(requireContext())
            .setView(v)
            .setPositiveButton(if (notificationRule != null) "Update" else "Add") { _, _ ->
                val process = processEditText.text.toString().trim()
                val component = componentEditText.text.toString().trim()
                val priority = priorityEditText.text.toString().trim().lowercase()
                val content = contentText.text.toString().trim()

                if (process.isNotEmpty() || component.isNotEmpty() || priority.isNotEmpty() || content.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        NotificationRule(process, component, priority, content).let {
                            notificationRule?.let {
                                logKatzeDatabase.notificationRuleDao().deleteAll(it)
                            }
                            logKatzeDatabase.notificationRuleDao().insertAll(it)
                        }
                        (requireActivity().applicationContext as MyApplication).rootLogCatService?.updateRules()
                        withContext(Dispatchers.Main) {
                            reload()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .create().show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_rule_list, container, false)

        notificationRecyclerView = view.findViewById(R.id.notificationListRecycler)
        view.findViewById<FloatingActionButton>(R.id.addNotificationButton).setOnClickListener {
            makeDialog(null)
        }

        notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        reload()
        return view
    }

    inner class RuleRecyclerAdapter(val rules: List<NotificationRule>) :
        RecyclerView.Adapter<RuleRecyclerAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.text)

            init {
                itemView.setOnClickListener {
                    makeDialog(rules[adapterPosition])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return LayoutInflater.from(parent.context).inflate(R.layout.item_rule, parent, false)
                .let {
                    ViewHolder(it)
                }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = rules[position]
            holder.textView.text = item.getNiceName()
        }

        override fun getItemCount() = rules.size
    }
}
