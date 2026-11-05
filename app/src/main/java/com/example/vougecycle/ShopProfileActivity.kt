package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vougecycle.databinding.ActivityShopProfileBinding
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import coil.load

class ShopProfileActivity : BaseActivity() {

    companion object {
        const val EXTRA_SELLER_ID = "extra_seller_id"
    }

    override val selectedTab = Tab.PROFILE

    private lateinit var binding: ActivityShopProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUserId = Supabase.client.auth.currentUserOrNull()?.id.orEmpty()
        val requestedSellerId = intent.getStringExtra(EXTRA_SELLER_ID)
        val sellerId = requestedSellerId?.takeIf { it.isNotBlank() } ?: currentUserId
        val isOwnShop = sellerId == currentUserId

        binding.tvTabShop.text = if (isOwnShop) "My shop" else "Shop"
        binding.ivBack.visibility = if (isOwnShop) View.GONE else View.VISIBLE
        binding.btnAddItems.visibility = if (isOwnShop) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSettings.visibility = if (isOwnShop) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvAddLinks.visibility = if (isOwnShop) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvAddDescription.visibility = if (isOwnShop) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvTabOrders.visibility = if (isOwnShop) View.VISIBLE else View.GONE
        binding.tvTabPurchases.visibility = if (isOwnShop) View.VISIBLE else View.GONE

        // Back
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Add items → Sell screen
        binding.btnAddItems.setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
        }

        // Settings → Settings screen (to be added)
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.tvAddLinks.setOnClickListener { if (isOwnShop) showAddLinksDialog() }

        binding.tvAddDescription.setOnClickListener {
            if (isOwnShop) startActivity(Intent(this, EditProfileActivity::class.java))
        }
        binding.tvTabFavorites.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
        }
        binding.tvTabOrders.setOnClickListener {
            startActivity(Intent(this, SellerOrdersActivity::class.java))
        }
        binding.tvTabPurchases.setOnClickListener {
            startActivity(Intent(this, MyPurchasesActivity::class.java))
        }
        binding.tvRating.setOnClickListener {
            startActivity(SellerReviewsActivity.intentFor(this, sellerId, binding.tvShopName.text.toString()))
        }

        if (isOwnShop) setupBottomNav() else findViewById<android.view.View>(R.id.bottomNav).visibility = android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        val currentUserId = Supabase.client.auth.currentUserOrNull()?.id.orEmpty()
        val sellerId = intent.getStringExtra(EXTRA_SELLER_ID)?.takeIf { it.isNotBlank() } ?: currentUserId
        loadProfile(sellerId)
        setupShopGrid(sellerId)
    }

    private fun loadProfile(sellerId: String) {
        if (sellerId.isBlank()) return
        lifecycleScope.launch {
            try {
                val profiles = Supabase.client.postgrest.from("profiles").select().decodeList<Profile>()
                val profile = profiles.firstOrNull { it.id == sellerId } ?: return@launch
                binding.tvShopName.text = profile.username.ifBlank { profile.fullName }.removePrefix("@")
                binding.tvShopName.tag = LanguageManager.USER_CONTENT_TAG
                binding.tvShopLocation.text = profile.location ?: "Location not provided"
                binding.tvShopLocation.tag = LanguageManager.USER_CONTENT_TAG
                binding.tvShopBio.text = profile.bio.orEmpty()
                binding.tvShopBio.visibility = if (profile.bio.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.tvShopBio.tag = LanguageManager.USER_CONTENT_TAG
                val links = profile.shopLinks.orEmpty().lines().filter { it.isNotBlank() }
                binding.tvShopLinks.text = links.joinToString("\n")
                binding.tvShopLinks.visibility = if (links.isEmpty()) View.GONE else View.VISIBLE
                binding.tvShopLinks.tag = LanguageManager.USER_CONTENT_TAG
                binding.ivAvatar.load(profile.avatarUrl) {
                    placeholder(R.color.vc_grey_bg)
                    error(R.color.vc_grey_bg)
                }
                val reviews = ReviewRepository.reviewsForSeller(sellerId)
                val average = reviews.map { it.review.rating }.average()
                binding.tvRating.text = if (reviews.isEmpty()) {
                    "No reviews yet"
                } else {
                    "★ %.1f (${reviews.size} review${if (reviews.size == 1) "" else "s"})".format(average)
                }
            } catch (_: Exception) {
                LanguageManager.makeToast(this@ShopProfileActivity, "Failed to load seller profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddLinksDialog() {
        val input = EditText(this).apply {
            hint = "Paste a website or social link"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(binding.tvShopLinks.text.toString())
        }
        AlertDialog.Builder(this)
            .setTitle("+Add links")
            .setMessage("Add website or social links, one per line.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> saveShopLinks(input.text.toString()) }
            .show()
    }

    private fun saveShopLinks(rawLinks: String) {
        val links = rawLinks.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { link ->
            if (link.startsWith("http://", true) || link.startsWith("https://", true)) link else "https://$link"
        }.distinct()
        if (links.any { !Patterns.WEB_URL.matcher(it).matches() }) {
            LanguageManager.makeToast(this, "Enter valid website links", Toast.LENGTH_LONG).show()
            return
        }
        val uid = Supabase.client.auth.currentUserOrNull()?.id ?: return
        lifecycleScope.launch {
            try {
                val update = buildJsonObject { put("shop_links", JsonPrimitive(links.joinToString("\n"))) }
                Supabase.client.postgrest.from("profiles").update(update) { filter { eq("id", uid) } }
                binding.tvShopLinks.text = links.joinToString("\n")
                binding.tvShopLinks.visibility = if (links.isEmpty()) View.GONE else View.VISIBLE
                binding.tvShopLinks.tag = LanguageManager.USER_CONTENT_TAG
                LanguageManager.makeToast(this@ShopProfileActivity, "Shop links saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@ShopProfileActivity, AppReliability.friendlyMessage(e, "save shop links"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupShopGrid(sellerId: String) {
        lifecycleScope.launch {
            try {
                val result = Supabase.client.postgrest.from("products").select()
                val products = result.decodeList<Product>().filter { it.sellerId == sellerId }
                binding.tvSoldCount.text = "${products.count { it.isSold }} sold"

                val adapter = ProductAdapter(
                    products = products,
                    onHeartClick = { product ->
                        lifecycleScope.launch {
                            try {
                                val added = MarketplaceRepository.toggleFavourite(product.id)
                                (binding.rvShopProducts.adapter as? ProductAdapter)?.setFavourite(product.id, added)
                                LanguageManager.makeToast(this@ShopProfileActivity, if (added) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                LanguageManager.makeToast(this@ShopProfileActivity, "Could not update favourite", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCardClick = { product ->
                        startActivity(ProductDetailActivity.intentFor(this@ShopProfileActivity, product))
                    }
                )
                adapter.setFavouriteIds(MarketplaceRepository.favouriteProductIds())
                binding.rvShopProducts.layoutManager = GridLayoutManager(this@ShopProfileActivity, 2)
                binding.rvShopProducts.adapter = adapter

            } catch (e: Exception) {
                LanguageManager.makeToast(this@ShopProfileActivity, "Failed to load shop", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
