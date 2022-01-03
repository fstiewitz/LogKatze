/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    var logFragment: LogFragment? = null

    private val drawerLayout: DrawerLayout by lazy { findViewById(R.id.drawer_layout) }
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.main_frame) as NavHostFragment).navController
    }
    private val navigationView: NavigationView by lazy { findViewById(R.id.nav_view) }

    override fun onStart() {
        super.onStart()
        (applicationContext as MyApplication).rootLogCallback =
            object : RootLogCatService.Callback {
                override fun rawLine(s: String, isInitial: Boolean): Boolean {
                    return false
                }

                override fun logged(e: LogEntry, isInitial: Boolean) {
                    CoroutineScope(Dispatchers.Main).launch {
                        logFragment?.adapter?.discardOld = isInitial
                        logFragment?.adapter?.addItem(e)
                    }
                }

                override fun error(e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        e.message?.let { LogEntry.fromLine(it) }
                            ?.let { logFragment?.adapter?.addItem(it) }
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

        setupActionBarWithNavController(navController, drawerLayout)
        navigationView.setupWithNavController(navController)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(drawerLayout)
    }

}