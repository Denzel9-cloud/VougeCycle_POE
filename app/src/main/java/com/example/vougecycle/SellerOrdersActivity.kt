package com.example.vougecycle

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vougecycle.databinding.ActivitySimpleListBinding
import kotlinx.coroutines.launch

class SellerOrdersActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivitySimpleListBinding
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvHeading.text = "Shop orders"
        binding.tvEmpty.text = "Your shop has no orders yet"
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
                if (!AppReliability.isOnline(this@SellerOrdersActivity)) throw IllegalStateException("network unavailable")
                val orders = MarketplaceRepository.sellerOrders().toMutableList()
                val adapter = OrderAdapter(
                    items = orders,
                    sellerMode = true,
                    onAdvance = { item, position ->
                        advanceOrder(item, position, binding.rvItems.adapter as OrderAdapter)
                    }
                )
                binding.rvItems.adapter = adapter
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                binding.rvItems.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.tvEmpty.text = AppReliability.friendlyMessage(e, "load shop orders")
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvItems.visibility = View.GONE
                binding.btnRetry.visibility = View.VISIBLE
            } finally {
                isLoading = false
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private fun advanceOrder(item: OrderDisplay, position: Int, adapter: OrderAdapter) {
        lifecycleScope.launch {
            try {
                val updatedOrder = MarketplaceRepository.advanceOrder(item.order)
                adapter.replace(position, item.copy(order = updatedOrder))
                LanguageManager.makeToast(this@SellerOrdersActivity, "Order marked ${updatedOrder.status.replace('_', ' ')}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@SellerOrdersActivity, AppReliability.friendlyMessage(e, "update order"), Toast.LENGTH_LONG).show()
            }
        }
    }
}
