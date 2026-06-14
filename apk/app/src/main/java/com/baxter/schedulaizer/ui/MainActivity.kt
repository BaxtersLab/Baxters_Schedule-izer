package com.baxter.schedulaizer.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.fragment.NavHostFragment
import android.util.Log
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.databinding.ActivityMainBinding
import com.baxter.schedulaizer.ui.settings.SettingsActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.graphics.drawable.Animatable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var activeTransferIds: List<Long> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        val bottomNav = binding.bottomNav
        if (navController == null) {
            Log.w("MainActivity", "NavController not available at startup; disabling bottom navigation until ready")
            bottomNav.isEnabled = false
        } else {
            bottomNav.setupWithNavController(navController)
            // Avoid reloading the same fragment when a bottom-nav item is reselected
            bottomNav.setOnItemReselectedListener { /* no-op */ }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_alarms -> {
                startActivity(Intent(this, com.baxter.schedulaizer.ui.alarms.AlarmsActivity::class.java))
                true
            }
            R.id.menu_background_transfers -> {
                findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.backgroundTransfersFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun setTransfersUploading(uploading: Boolean) {
        val item = binding.bottomNav.menu.findItem(R.id.backgroundTransfersFragment)
        if (uploading) {
            val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_transfers_anim)
            item.icon = drawable
            (drawable as? Animatable)?.start()
        } else {
            item.icon = AppCompatResources.getDrawable(this, R.drawable.ic_transfers)
        }
    }

    fun setTransfersSuccess() {
        val item = binding.bottomNav.menu.findItem(R.id.backgroundTransfersFragment)
        item.icon = AppCompatResources.getDrawable(this, R.drawable.ic_transfers_success)

        // auto-reset after short delay
        binding.bottomNav.postDelayed({
            item.icon = AppCompatResources.getDrawable(this, R.drawable.ic_transfers)
        }, 1500)
    }

    fun setTransfersIdle() {
        val item = binding.bottomNav.menu.findItem(R.id.backgroundTransfersFragment)
        item.icon = AppCompatResources.getDrawable(this, R.drawable.ic_transfers)
    }

    fun updateActiveTransferIds(ids: List<Long>) {
        activeTransferIds = ids
    }

    fun getActiveTransferIds(): List<Long> = activeTransferIds
}
