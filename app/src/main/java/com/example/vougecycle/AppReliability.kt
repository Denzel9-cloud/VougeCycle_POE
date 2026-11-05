package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.jan.supabase.auth.auth

object AppReliability {
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun friendlyMessage(error: Throwable, action: String = "complete this action"): String {
        val raw = error.message.orEmpty().lowercase()
        return when {
            raw.contains("seller needs to add a default payout account") ->
                "The seller needs to add a bank account and set it as the default payout account before this payment can be completed."
            raw.contains("selected demo account does not have enough balance") || raw.contains("does not have enough balance") ->
                "The selected account does not have enough balance for this payment and shipping."
            raw.contains("selected payment account does not belong to the buyer") ->
                "Choose a bank account saved under your signed-in profile."
            raw.contains("this product is no longer available") || raw.contains("already been sold") ->
                "This item is no longer available."
            raw.contains("timeout") -> "The request took too long. Please try again."
            raw.contains("unable to resolve") || raw.contains("failed to connect") || raw.contains("network") ->
                "No internet connection. Check your connection and try again."
            raw.contains("jwt") || raw.contains("unauthorized") || raw.contains("not authenticated") ->
                "Your session has expired. Please log in again."
            raw.contains("duplicate") || raw.contains("unique") -> "That information already exists."
            raw.contains("permission") || raw.contains("row-level security") ->
                "You do not have permission to $action."
            else -> "Could not $action. Please try again."
        }
    }
}

open class AuthenticatedActivity : SafeAreaActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        UiSafety.preventAutomaticKeyboard(this)
    }

    override fun onStart() {
        super.onStart()
        if (Supabase.client.auth.currentUserOrNull() == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}

object ProductMemoryCache {
    private val products = mutableListOf<Product>()
    fun save(items: List<Product>) {
        products.clear()
        products.addAll(items)
    }
    fun load(): List<Product> = products.toList()
}
