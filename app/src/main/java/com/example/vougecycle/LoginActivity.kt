package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : SafeAreaActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiSafety.preventAutomaticKeyboard(this)
        UiSafety.bindPasswordVisibility(binding.etPassword, binding.ivPasswordVisibility)

        binding.tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                LanguageManager.makeToast(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!InputValidator.isValidEmail(email)) {
                LanguageManager.makeToast(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Logging in..."

            lifecycleScope.launch {
                try {
                    if (!AppReliability.isOnline(this@LoginActivity)) throw IllegalStateException("network unavailable")
                    Supabase.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    LanguageManager.makeToast(
                        this@LoginActivity,
                        "Welcome back!",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Log In"
                    val raw = e.message.orEmpty().lowercase()
                    val message = if (raw.contains("invalid") || raw.contains("credentials")) {
                        "Incorrect email or password."
                    } else AppReliability.friendlyMessage(e, "log in")
                    LanguageManager.makeToast(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
