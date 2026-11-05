package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vougecycle.databinding.ActivitySimpleListBinding
import kotlinx.coroutines.launch

class MyPurchasesActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivitySimpleListBinding
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvHeading.text = "My purchases"
        binding.tvEmpty.text = "You have not purchased anything yet"
        binding.ivBack.setOnClickListener { finish() }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.btnRetry.setOnClickListener { loadOrders() }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        if (isLoading) return
        isLoading = true
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.rvItems.visibility = View.GONE
        lifecycleScope.launch {
            try {
                if (!AppReliability.isOnline(this@MyPurchasesActivity)) throw IllegalStateException("network unavailable")
                val orders = MarketplaceRepository.buyerOrders().toMutableList()
                binding.rvItems.adapter = OrderAdapter(
                    items = orders,
                    sellerMode = false,
                    onTrack = { item -> startActivity(OrderTrackingActivity.intentFor(this@MyPurchasesActivity, item)) },
                    onReview = { item -> startActivity(ReviewActivity.intentFor(this@MyPurchasesActivity, item)) }
                )
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                binding.rvItems.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.tvEmpty.text = AppReliability.friendlyMessage(e, "load purchases")
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvItems.visibility = View.GONE
                binding.btnRetry.visibility = View.VISIBLE
            } finally {
                isLoading = false
                binding.progressLoading.visibility = View.GONE
            }
        }
    }
}
