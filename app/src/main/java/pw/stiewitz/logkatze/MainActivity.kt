/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    var logFragment: LogFragment? = null

    override fun onStart() {
        super.onStart()
        (applicationContext as MyApplication).rootLogCallback =
            object : RootLogCatService.Callback {
                override fun rawLine(s: String, isInitial: Boolean): Boolean {
                    CoroutineScope(Dispatchers.Main).launch {
                        logFragment?.adapter?.discardOld = isInitial
                        logFragment?.adapter?.addItem(LogEntry(s))
                    }
                    return true
                }

                override fun error(e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        e.message?.let { LogEntry(it) }?.let { logFragment?.adapter?.addItem(it) }
                    }
                }
            }
    }

    override fun onStop() {
        super.onStop()
        (applicationContext as MyApplication).rootLogCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}