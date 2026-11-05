package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : SafeAreaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the brand canvas visible behind the status and navigation bars.
        window.decorView.setBackgroundColor(getColor(R.color.vc_white))
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            val currentUser = try {
                Supabase.client.auth.currentUserOrNull()
            } catch (e: Exception) { null }

            val dest = if (currentUser != null) HomeActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this@MainActivity, dest))
            finish()
        }
    }
}
