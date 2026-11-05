package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivitySettingsBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class SettingsActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appearancePreferences = getSharedPreferences(VougeCycleApplication.PREFERENCES, MODE_PRIVATE)
        binding.switchDarkMode.isChecked = appearancePreferences.getBoolean(VougeCycleApplication.KEY_DARK_MODE, false)
        updateThemeLabel(binding.switchDarkMode.isChecked)
        binding.switchDarkMode.setOnCheckedChangeListener { _, darkMode ->
            appearancePreferences.edit()
                .putBoolean(VougeCycleApplication.KEY_DARK_MODE, darkMode)
                .apply()
            updateThemeLabel(darkMode)
            AppCompatDelegate.setDefaultNightMode(
                if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        binding.rowAppearance.setOnClickListener {
            binding.switchDarkMode.isChecked = !binding.switchDarkMode.isChecked
        }

        binding.ivBack.setOnClickListener { finish() }

        binding.rowProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        binding.rowLanguage.setOnClickListener {
            LanguageManager.showLanguagePicker(this)
        }
        binding.rowBank.setOnClickListener {
            startActivity(Intent(this, BankAccountsActivity::class.java))
        }
        binding.rowContact.setOnClickListener {
            startActivity(Intent(this, ContactDetailsActivity::class.java))
        }
        binding.rowNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.rowPrivacy.setOnClickListener {
            startActivity(InformationActivity.intentFor(this, InformationActivity.PAGE_PRIVACY))
        }
        binding.rowAbout.setOnClickListener {
            startActivity(InformationActivity.intentFor(this, InformationActivity.PAGE_SHOP))
        }

        // Log out with confirmation dialog
        binding.rowLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(LanguageManager.translate(this, "Log out"))
                .setMessage(LanguageManager.translate(this, "Are you sure you want to log out?"))
                .setNegativeButton(LanguageManager.translate(this, "Cancel"), null)
                .setPositiveButton(LanguageManager.translate(this, "Log out")) { _, _ ->
                    lifecycleScope.launch {
                        try {
                            Supabase.client.auth.signOut()
                        } catch (e: Exception) {
                            LanguageManager.makeToast(this@SettingsActivity, "Log out failed. Please try again.", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .show()
        }
    }

    private fun updateThemeLabel(darkMode: Boolean) {
        binding.tvThemeMode.text = if (darkMode) "Dark mode" else "Light mode"
    }
}
