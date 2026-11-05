package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vougecycle.databinding.ActivitySimpleListBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class FavouritesActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivitySimpleListBinding
    private val products = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvHeading.text = "Favourites"
        binding.tvEmpty.text = "You have no favourite items yet"
        binding.ivBack.setOnClickListener { finish() }

        adapter = ProductAdapter(products, ::removeFavourite, ::openProduct)
        binding.rvItems.layoutManager = GridLayoutManager(this, 2)
        binding.rvItems.adapter = adapter
        binding.btnRetry.setOnClickListener { loadFavourites() }
    }

    override fun onResume() {
        super.onResume()
        loadFavourites()
    }

    private fun loadFavourites() {
        if (isLoading) return
        isLoading = true
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.rvItems.visibility = View.GONE
        lifecycleScope.launch {
            try {
                if (!AppReliability.isOnline(this@FavouritesActivity)) throw IllegalStateException("network unavailable")
                val ids = MarketplaceRepository.favouriteProductIds()
                val allProducts = Supabase.client.postgrest.from("products").select().decodeList<Product>()
                products.clear()
                products.addAll(allProducts.filter { ids.contains(it.id) }.sortedByDescending { it.createdAt })
                adapter.setFavouriteIds(ids)
                binding.tvEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                binding.rvItems.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.tvEmpty.text = AppReliability.friendlyMessage(e, "load favourites")
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvItems.visibility = View.GONE
                binding.btnRetry.visibility = View.VISIBLE
            } finally {
                isLoading = false
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private fun removeFavourite(product: Product) {
        lifecycleScope.launch {
            try {
                MarketplaceRepository.toggleFavourite(product.id)
                loadFavourites()
                LanguageManager.makeToast(this@FavouritesActivity, "Removed from favourites", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@FavouritesActivity, AppReliability.friendlyMessage(e, "update favourite"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openProduct(product: Product) {
        startActivity(ProductDetailActivity.intentFor(this, product))
    }
}
