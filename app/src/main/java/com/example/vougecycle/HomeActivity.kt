package com.example.vougecycle

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vougecycle.databinding.ActivityHomeBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {

    override val selectedTab: Tab = Tab.HOME

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ProductAdapter
    private val visibleProducts = mutableListOf<Product>()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(
            binding.tileMen to "Men",
            binding.tileWomen to "Women",
            binding.tileEras to "Eras",
            binding.tileVintage to "Vintage",
            binding.tileSports to "Sports"
        ).forEach { (tile, category) ->
            tile.setOnClickListener {
                startActivity(ExploreActivity.intentForCategory(this, category))
            }
        }

        setupProductGrid()
        binding.btnRetryHome.setOnClickListener { fetchProducts() }
        setupBottomNav()
    }
    override fun onResume() {
        super.onResume()
        fetchProducts()
    }

    private fun setupProductGrid() {
        adapter = ProductAdapter(
            products = visibleProducts,
            onHeartClick = { product ->
                toggleFavourite(product)
            },
            onCardClick = { product ->
                startActivity(ProductDetailActivity.intentFor(this, product))
            }
        )
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvProducts.adapter = adapter
    }

    private fun fetchProducts() {
        if (isLoading) return
        isLoading = true
        binding.progressHome.visibility = View.VISIBLE
        binding.btnRetryHome.visibility = View.GONE
        binding.tvHomeEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                if (!AppReliability.isOnline(this@HomeActivity)) throw IllegalStateException("network unavailable")
                val result = Supabase.client.postgrest
                    .from("products")
                    .select()
                val products = result.decodeList<Product>()
                ProductMemoryCache.save(products)

                visibleProducts.clear()
                visibleProducts.addAll(CatalogProducts.all)
                visibleProducts.addAll(products.sortedByDescending { it.createdAt })
                adapter.setFavouriteIds(MarketplaceRepository.favouriteProductIds())
                adapter.notifyDataSetChanged()
                binding.tvHomeEmpty.visibility = if (visibleProducts.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Log.e("Supabase", "Failed to load products: ${e.message}", e)
                val cached = ProductMemoryCache.load()
                visibleProducts.clear()
                visibleProducts.addAll(CatalogProducts.all)
                visibleProducts.addAll(cached.sortedByDescending { it.createdAt })
                if (visibleProducts.isNotEmpty()) {
                    adapter.notifyDataSetChanged()
                    LanguageManager.makeToast(this@HomeActivity, "Offline — showing recently loaded products", Toast.LENGTH_LONG).show()
                } else {
                    binding.tvHomeEmpty.text = AppReliability.friendlyMessage(e, "load products")
                    binding.tvHomeEmpty.visibility = View.VISIBLE
                    binding.btnRetryHome.visibility = View.VISIBLE
                }
            } finally {
                isLoading = false
                binding.progressHome.visibility = View.GONE
            }
        }
    }

    private fun toggleFavourite(product: Product) {
        lifecycleScope.launch {
            try {
                val added = MarketplaceRepository.toggleFavourite(product.id)
                adapter.setFavourite(product.id, added)
                LanguageManager.makeToast(this@HomeActivity, if (added) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@HomeActivity, AppReliability.friendlyMessage(e, "update favourite"), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
