package com.example.vougecycle

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityForgotPasswordBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ForgotPasswordActivity : SafeAreaActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSend.setOnClickListener {
            val identifier = binding.etIdentifier.text.toString().trim()

            if (!InputValidator.isValidEmail(identifier)) {
                LanguageManager.makeToast(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnSend.isEnabled = false
            binding.btnSend.text = "Sending..."
            lifecycleScope.launch {
                try {
                    Supabase.client.auth.resetPasswordForEmail(identifier.lowercase())
                    LanguageManager.makeToast(this@ForgotPasswordActivity, "Password reset email sent", Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {
                    binding.btnSend.isEnabled = true
                    binding.btnSend.text = "Send"
                    LanguageManager.makeToast(this@ForgotPasswordActivity, "Could not send reset email: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
