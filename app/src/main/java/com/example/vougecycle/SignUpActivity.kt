package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivitySignUpBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class SignUpActivity : SafeAreaActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLanguage.setOnClickListener { LanguageManager.showLanguagePicker(this) }
        UiSafety.preventAutomaticKeyboard(this)
        UiSafety.bindPasswordVisibility(binding.etPassword, binding.ivPasswordVisibility)
        UiSafety.bindPasswordVisibility(binding.etConfirmPassword, binding.ivConfirmPasswordVisibility)

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnSignUp.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            // --- Local validation (unchanged from before) ---
            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                toast("Please fill in all fields")
                return@setOnClickListener
            }
            if (!InputValidator.isValidEmail(email)) {
                toast("Please enter a valid email address")
                return@setOnClickListener
            }
            InputValidator.passwordError(password)?.let {
                toast(it)
                return@setOnClickListener
            }
            if (password != confirm) {
                toast("Passwords do not match")
                return@setOnClickListener
            }

            // --- Call Supabase ---
            binding.btnSignUp.isEnabled = false
            binding.btnSignUp.text = LanguageManager.translate(this, "Creating account...")

            lifecycleScope.launch {
                try {
                    Supabase.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        // These get written into raw_user_meta_data,
                        // which our trigger reads to populate the profiles row
                        this.data = kotlinx.serialization.json.buildJsonObject {
                            put("username", kotlinx.serialization.json.JsonPrimitive(username))
                            put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                        }
                    }

                    val signedIn = Supabase.client.auth.currentUserOrNull() != null
                    toast(if (signedIn) "Account created! Welcome, $username" else "Account created. Check your email to confirm it.")
                    startActivity(Intent(this@SignUpActivity, if (signedIn) HomeActivity::class.java else LoginActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = LanguageManager.translate(this@SignUpActivity, "Sign Up")
                    toast("Sign up failed: ${e.message ?: "unknown error"}")
                }
            }
        }
    }

    private fun toast(msg: String) {
        LanguageManager.makeToast(this, msg, Toast.LENGTH_LONG).show()
    }
}
