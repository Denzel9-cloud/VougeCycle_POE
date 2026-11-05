package com.example.vougecycle

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityProductDetailBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ProductDetailActivity : AuthenticatedActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PRICE = "extra_price"
        const val EXTRA_PRICE_VALUE = "extra_price_value"
        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_SOLD = "extra_sold"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_CONDITION = "extra_condition"
        const val EXTRA_SELLER_ID = "extra_seller_id"
        const val EXTRA_BRAND = "extra_brand"
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_COLOUR = "extra_colour"
        const val EXTRA_MATERIAL = "extra_material"
        const val EXTRA_DELIVERY_PREFERENCE = "extra_delivery_preference"
        const val EXTRA_DELIVERY_NOTES = "extra_delivery_notes"

        fun intentFor(context: Context, product: Product): Intent =
            Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, product.id)
                putExtra(EXTRA_TITLE, product.title)
                putExtra(EXTRA_PRICE, product.priceFormatted())
                putExtra(EXTRA_PRICE_VALUE, product.price)
                putExtra(EXTRA_IMAGE_URL, product.imageUrl)
                putExtra(EXTRA_SOLD, product.isSold)
                putExtra(EXTRA_DESCRIPTION, product.description)
                putExtra(EXTRA_CONDITION, product.condition)
                putExtra(EXTRA_SELLER_ID, product.sellerId)
                putExtra(EXTRA_BRAND, product.brand)
                putExtra(EXTRA_SIZE, product.size)
                putExtra(EXTRA_COLOUR, product.colour)
                putExtra(EXTRA_MATERIAL, product.material)
                putExtra(EXTRA_DELIVERY_PREFERENCE, product.sellerDeliveryPreference)
                putExtra(EXTRA_DELIVERY_NOTES, product.sellerDeliveryNotes)
            }
    }

    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Product"
        val price = intent.getStringExtra(EXTRA_PRICE) ?: "R 0.00"
        val priceValue = intent.getDoubleExtra(EXTRA_PRICE_VALUE, 0.0)
        val productId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        val sold = intent.getBooleanExtra(EXTRA_SOLD, false)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: "No description provided."
        val condition = intent.getStringExtra(EXTRA_CONDITION) ?: "Used"
        val sellerId = intent.getStringExtra(EXTRA_SELLER_ID).orEmpty()
        val isCatalogPreview = productId.startsWith("catalog_")

        binding.tvTitle.text = title
        binding.tvTitle.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvPrice.text = price
        binding.tvCondition.text = if (sold) "SOLD" else condition
        binding.tvDescription.text = description
        binding.tvDescription.tag = LanguageManager.USER_CONTENT_TAG
        val productDetails = listOfNotNull(
            intent.getStringExtra(EXTRA_BRAND)?.takeIf { it.isNotBlank() }?.let { "${LanguageManager.translate(this, "Brand")}: $it" },
            intent.getStringExtra(EXTRA_SIZE)?.takeIf { it.isNotBlank() }?.let { "${LanguageManager.translate(this, "Size")}: $it" },
            intent.getStringExtra(EXTRA_COLOUR)?.takeIf { it.isNotBlank() }?.let { "${LanguageManager.translate(this, "Colour")}: $it" },
            intent.getStringExtra(EXTRA_MATERIAL)?.takeIf { it.isNotBlank() }?.let { "${LanguageManager.translate(this, "Material")}: $it" }
        )
        if (productDetails.isNotEmpty()) {
            binding.tvProductMeta.text = productDetails.joinToString("\n")
            binding.tvProductMeta.visibility = android.view.View.VISIBLE
        }
        val deliveryPreference = intent.getStringExtra(EXTRA_DELIVERY_PREFERENCE)
        val deliveryNotes = intent.getStringExtra(EXTRA_DELIVERY_NOTES).orEmpty()
        val deliveryLines = mutableListOf<String>()
        if (!deliveryPreference.isNullOrBlank()) {
            deliveryLines.add("${LanguageManager.translate(this, "Delivery options")}: ${LanguageManager.translate(this, deliveryPreference)}")
        }
        if (deliveryNotes.isNotBlank()) deliveryLines.add(deliveryNotes)
        if (deliveryLines.isNotEmpty()) {
            binding.tvSellerDelivery.text = deliveryLines.joinToString("\n")
            binding.tvSellerDelivery.visibility = android.view.View.VISIBLE
        }
        binding.tvSellerName.text = if (isCatalogPreview) "VogueCycle sample catalogue" else "Seller"
        binding.tvSellerName.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvSellerLocation.text = if (isCatalogPreview) "Sample listing" else "Location not provided"
        binding.tvSellerLocation.tag = LanguageManager.USER_CONTENT_TAG
        if (!isCatalogPreview) loadSeller(sellerId)

        ProductImages.load(binding.ivProduct, imageUrl)

        binding.ivBack.setOnClickListener { finish() }

        var fav = false
        if (!isCatalogPreview) {
            lifecycleScope.launch {
                try {
                    fav = MarketplaceRepository.favouriteProductIds().contains(productId)
                    binding.ivHeart.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
                } catch (_: Exception) { }
            }
        }
        binding.ivHeart.setOnClickListener {
            if (isCatalogPreview) {
                LanguageManager.makeToast(this, "This sample listing is for preview only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    fav = MarketplaceRepository.toggleFavourite(productId)
                    binding.ivHeart.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
                    LanguageManager.makeToast(this@ProductDetailActivity, if (fav) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    LanguageManager.makeToast(this@ProductDetailActivity, "Could not update favourite", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.llSeller.setOnClickListener {
            if (isCatalogPreview) return@setOnClickListener
            startActivity(Intent(this, ShopProfileActivity::class.java).apply {
                putExtra(ShopProfileActivity.EXTRA_SELLER_ID, sellerId)
            })
        }

        binding.btnMessage.setOnClickListener {
            if (isCatalogPreview) {
                LanguageManager.makeToast(this, "Seller details will be added with the listing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
            if (currentUserId == sellerId) {
                LanguageManager.makeToast(this, "You cannot message yourself", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnMessage.isEnabled = false
            lifecycleScope.launch {
                try {
                    val product = Product(
                        id = productId,
                        sellerId = sellerId,
                        title = title,
                        description = description,
                        price = priceValue,
                        condition = condition,
                        imageUrl = imageUrl,
                        isSold = sold
                    )
                    val conversation = MessagingRepository.getOrCreateConversation(product)
                    startActivity(ChatActivity.intentFor(
                        this@ProductDetailActivity,
                        conversation,
                        binding.tvSellerName.text.toString(),
                        title
                    ))
                } catch (e: Exception) {
                    LanguageManager.makeToast(this@ProductDetailActivity, "Could not open chat: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnMessage.isEnabled = true
                }
            }
        }

        binding.btnBuy.setOnClickListener {
            if (isCatalogPreview) {
                LanguageManager.makeToast(this, "This sample listing is for preview only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
            when {
                sold -> LanguageManager.makeToast(this, "This item has already been sold", Toast.LENGTH_SHORT).show()
                currentUserId == sellerId -> LanguageManager.makeToast(this, "You cannot buy your own item", Toast.LENGTH_SHORT).show()
                else -> startActivity(Intent(this, OrderConfirmationActivity::class.java).apply {
                    putExtras(this@ProductDetailActivity.intent.extras ?: Bundle())
                    putExtra(EXTRA_PRICE_VALUE, priceValue)
                })
            }
        }
    }

    private fun loadSeller(sellerId: String) {
        if (sellerId.isBlank()) return
        lifecycleScope.launch {
            try {
                val profiles = Supabase.client.postgrest.from("profiles").select().decodeList<Profile>()
                val seller = profiles.firstOrNull { it.id == sellerId } ?: return@launch
                binding.tvSellerName.text = "@${seller.username.ifBlank { seller.fullName }}"
                binding.tvSellerName.tag = LanguageManager.USER_CONTENT_TAG
                binding.tvSellerLocation.text = seller.location ?: "Location not provided"
                binding.tvSellerLocation.tag = LanguageManager.USER_CONTENT_TAG
            } catch (_: Exception) {
                // Keep the safe fallback labels if the profile cannot be loaded.
            }
        }
    }
}
