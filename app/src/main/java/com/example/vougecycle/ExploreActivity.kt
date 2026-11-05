package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vougecycle.databinding.ActivityExploreBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class ExploreActivity : BaseActivity() {

    override val selectedTab: Tab = Tab.EXPLORE

    private lateinit var binding: ActivityExploreBinding
    private lateinit var adapter: ProductAdapter
    private val allProducts = mutableListOf<Product>()
    private val visibleProducts = mutableListOf<Product>()
    private var activeChip: TextView? = null
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGrid()
        setupChips()
        setupSearch()
        setupAdvancedFilters()
        binding.btnRetryExplore.setOnClickListener { fetchProducts() }
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        fetchProducts()
    }

    private fun setupGrid() {
        adapter = ProductAdapter(
            products = visibleProducts,
            onHeartClick = { product ->
                toggleFavourite(product)
            },
            onCardClick = { product ->
                startActivity(ProductDetailActivity.intentFor(this, product))
            }
        )
        binding.rvExplore.layoutManager = GridLayoutManager(this, 2)
        binding.rvExplore.adapter = adapter
    }

    private fun setupChips() {
        binding.chipAll.tag = "All"
        binding.chipWomen.tag = "Women"
        binding.chipMen.tag = "Men"
        binding.chipEras.tag = "Eras"
        binding.chipVintage.tag = "Vintage"
        binding.chipSports.tag = "Sports"
        val chips = listOf(
            binding.chipAll, binding.chipMen, binding.chipWomen,
            binding.chipVintage, binding.chipEras, binding.chipSports
        )
        val requestedCategory = intent.getStringExtra(EXTRA_CATEGORY)
        val selectedChip = chips.firstOrNull {
            it.tag?.toString()?.equals(requestedCategory, ignoreCase = true) == true
        } ?: binding.chipAll
        highlightChip(selectedChip)
        chips.forEach { chip ->
            chip.setOnClickListener {
                highlightChip(chip)
                applyFilters()
            }
        }
    }

    private fun highlightChip(chip: TextView) {
        activeChip?.setBackgroundResource(R.drawable.bg_chip)
        activeChip?.setTextColor(getColor(R.color.vc_black))
        chip.setBackgroundResource(R.drawable.bg_button_primary)
        chip.setTextColor(getColor(R.color.vc_white))
        activeChip = chip
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })
    }

    private fun setupAdvancedFilters() {
        val conditions = listOf("Add conditions", "New with tags", "New without tags", "Used - Excellent", "Used - Good", "Used - Fair")
        val sorts = listOf("Newest", "Price: Low to high", "Price: High to low")
        binding.spCondition.adapter = LocalizedArrayAdapter(this, conditions)
        binding.spSort.adapter = LocalizedArrayAdapter(this, sorts)
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = applyFilters()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spCondition.onItemSelectedListener = listener
        binding.spSort.onItemSelectedListener = listener
        binding.llAdvancedFilters.visibility = View.GONE
        binding.btnMoreOptions.setOnClickListener {
            val showFilters = binding.llAdvancedFilters.visibility != View.VISIBLE
            binding.llAdvancedFilters.visibility = if (showFilters) View.VISIBLE else View.GONE
            binding.tvMoreOptionsArrow.text = if (showFilters) "⌃" else "⌄"
        }
        binding.cbIncludeSold.setOnCheckedChangeListener { _, _ -> applyFilters() }

        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilters()
        }
        binding.etMinPrice.addTextChangedListener(priceWatcher)
        binding.etMaxPrice.addTextChangedListener(priceWatcher)
        binding.btnClearFilters.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etMinPrice.text?.clear()
            binding.etMaxPrice.text?.clear()
            binding.spCondition.setSelection(0)
            binding.spSort.setSelection(0)
            binding.cbIncludeSold.isChecked = false
            highlightChip(binding.chipAll)
            applyFilters()
        }
    }

    private fun applyFilters() {
        val q = binding.etSearch.text.toString().trim().lowercase()
        val category = activeChip?.tag?.toString() ?: "All"
        val condition = binding.spCondition.selectedItem?.toString() ?: "Add conditions"
        val minPrice = binding.etMinPrice.text.toString().toDoubleOrNull()
        val maxPrice = binding.etMaxPrice.text.toString().toDoubleOrNull()
        val includeSold = binding.cbIncludeSold.isChecked

        var filtered = allProducts.filter { product ->
            val searchable = listOf(product.title, product.description.orEmpty(), product.category.orEmpty(), product.condition.orEmpty()).joinToString(" ").lowercase()
            val matchesQuery = q.isEmpty() || searchable.contains(q)
            // The four new sample listings have no single gender/era section assigned.
            // Keep them visible in every category so their photos show from every section.
            val matchesCategory = category == "All" ||
                product.category.equals(category, ignoreCase = true) ||
                (product.isCatalogPreview && product.category == null)
            val matchesCondition = condition == "Add conditions" || product.condition == condition
            val matchesMin = minPrice == null || product.price <= 0.0 || product.price >= minPrice
            val matchesMax = maxPrice == null || product.price <= 0.0 || product.price <= maxPrice
            val matchesSold = includeSold || !product.isSold
            matchesQuery && matchesCategory && matchesCondition && matchesMin && matchesMax && matchesSold
        }
        filtered = when (binding.spSort.selectedItem?.toString()) {
            "Price: Low to high" -> filtered.sortedBy { it.price }
            "Price: High to low" -> filtered.sortedByDescending { it.price }
            else -> filtered.sortedWith(compareByDescending<Product> { it.createdAt }.thenBy { it.id })
        }
        visibleProducts.clear()
        visibleProducts.addAll(filtered)
        adapter.notifyDataSetChanged()
        binding.tvResultCount.text = "${filtered.size} item${if (filtered.size == 1) "" else "s"}"
        binding.tvNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvExplore.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun fetchProducts() {
        if (isLoading) return
        isLoading = true
        binding.progressExplore.visibility = View.VISIBLE
        binding.btnRetryExplore.visibility = View.GONE
        binding.tvNoResults.visibility = View.GONE
        lifecycleScope.launch {
            try {
                if (!AppReliability.isOnline(this@ExploreActivity)) throw IllegalStateException("network unavailable")
                val result = Supabase.client.postgrest.from("products").select()
                val products = result.decodeList<Product>()
                ProductMemoryCache.save(products)

                allProducts.clear()
                allProducts.addAll(CatalogProducts.all)
                allProducts.addAll(products)
                adapter.setFavouriteIds(MarketplaceRepository.favouriteProductIds())

                applyFilters()

            } catch (e: Exception) {
                Log.e("Supabase", "Explore fetch failed: ${e.message}", e)
                val cached = ProductMemoryCache.load()
                allProducts.clear()
                allProducts.addAll(CatalogProducts.all)
                allProducts.addAll(cached)
                if (allProducts.isNotEmpty()) {
                    applyFilters()
                    LanguageManager.makeToast(this@ExploreActivity, "Offline — showing recently loaded products", Toast.LENGTH_LONG).show()
                } else {
                    binding.tvNoResults.text = AppReliability.friendlyMessage(e, "load products")
                    binding.tvNoResults.visibility = View.VISIBLE
                    binding.btnRetryExplore.visibility = View.VISIBLE
                }
            } finally {
                isLoading = false
                binding.progressExplore.visibility = View.GONE
            }
        }
    }

    private fun toggleFavourite(product: Product) {
        lifecycleScope.launch {
            try {
                val added = MarketplaceRepository.toggleFavourite(product.id)
                adapter.setFavourite(product.id, added)
                LanguageManager.makeToast(this@ExploreActivity, if (added) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@ExploreActivity, AppReliability.friendlyMessage(e, "update favourite"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_CATEGORY = "explore_category"

        fun intentForCategory(context: android.content.Context, category: String): Intent =
            Intent(context, ExploreActivity::class.java).putExtra(EXTRA_CATEGORY, category)
    }
}
