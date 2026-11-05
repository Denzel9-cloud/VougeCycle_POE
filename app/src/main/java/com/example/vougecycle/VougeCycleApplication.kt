package com.example.vougecycle

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class VougeCycleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val darkMode = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    companion object {
        const val PREFERENCES = "vougecycle_appearance"
        const val KEY_DARK_MODE = "dark_mode_enabled"
    }
}
