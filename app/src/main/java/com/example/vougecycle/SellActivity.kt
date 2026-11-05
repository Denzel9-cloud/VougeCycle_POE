package com.example.vougecycle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivitySellBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject

class SellActivity : BaseActivity() {

    override val selectedTab: Tab = Tab.SELL

    private lateinit var binding: ActivitySellBinding

    // URI of the picked image (before upload)
    private var pickedImageUri: Uri? = null

    // Public URL after upload completes
    private var uploadedImageUrl: String? = null
    private var imageUploadInProgress = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
            uploadImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Category spinner
        val categories = listOf("Choose category", "Women", "Men", "Eras", "Vintage", "Sports")
        binding.spCategory.adapter = LocalizedArrayAdapter(this, categories)

        // Condition spinner
        val conditions = listOf(
            "Choose item condition",
            "New with tags", "New without tags",
            "Used - Excellent", "Used - Good", "Used - Fair"
        )
        binding.spCondition.adapter = LocalizedArrayAdapter(this, conditions)

        val deliveryOptions = listOf(
            "Both courier options",
            "Door-to-door only",
            "Locker collection only"
        )
        binding.spSellerDelivery.adapter = LocalizedArrayAdapter(this, deliveryOptions)

        binding.llUploadPhoto.setOnClickListener {
            pickImage.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.tvAdditional.setOnClickListener {
            val expanded = binding.additionalDetailsPanel.visibility != View.VISIBLE
            binding.additionalDetailsPanel.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.tvAdditional.text = if (expanded) {
                "⌄ ${LanguageManager.translate(this, "Additional Details")}"
            } else {
                "› ${LanguageManager.translate(this, "Additional Details")}"
            }
        }
        binding.tvDelivery.setOnClickListener {
            val expanded = binding.deliveryPanel.visibility != View.VISIBLE
            binding.deliveryPanel.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.tvDelivery.text = if (expanded) {
                "⌄ ${LanguageManager.translate(this, "Delivery")}"
            } else {
                "› ${LanguageManager.translate(this, "Delivery")}"
            }
        }

        // List Item
        binding.btnList.setOnClickListener {
            if (imageUploadInProgress) {
                LanguageManager.makeToast(this, "Please wait for the photo upload to finish", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val title = binding.etTitle.text.toString().trim()
            val priceText = binding.etPrice.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            // Validation
            if (title.isEmpty()) {
                LanguageManager.makeToast(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (priceText.isEmpty()) {
                LanguageManager.makeToast(this, "Please enter a price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val price = InputValidator.validPrice(priceText)
            if (price == null) {
                LanguageManager.makeToast(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                LanguageManager.makeToast(this, "Please add a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.spCategory.selectedItemPosition == 0) {
                LanguageManager.makeToast(this, "Please choose a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.spCondition.selectedItemPosition == 0) {
                LanguageManager.makeToast(this, "Please choose a condition", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = binding.spCategory.selectedItem.toString()
            val condition = binding.spCondition.selectedItem.toString()
            val brand = binding.etBrand.text.toString().trim()
            val size = binding.etSize.text.toString().trim()
            val colour = binding.etColour.text.toString().trim()
            val material = binding.etMaterial.text.toString().trim()
            val deliveryPreference = deliveryOptions[binding.spSellerDelivery.selectedItemPosition]
            val deliveryNotes = binding.etDeliveryNotes.text.toString().trim()

            // Logged-in user required
            val currentUser = Supabase.client.auth.currentUserOrNull()
            if (currentUser == null) {
                LanguageManager.makeToast(this, "You must be logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button while inserting
            binding.btnList.isEnabled = false
            binding.btnList.text = "Listing..."

            lifecycleScope.launch {
                try {
                    // Build the row as JSON. `uploadedImageUrl` may be null — that's fine.
                    val newRow = buildJsonObject {
                        put("seller_id", JsonPrimitive(currentUser.id))
                        put("title", JsonPrimitive(title))
                        put("description", JsonPrimitive(description))
                        put("price", JsonPrimitive(price))
                        put("category", JsonPrimitive(category))
                        put("condition", JsonPrimitive(condition))
                        if (brand.isNotEmpty()) put("brand", JsonPrimitive(brand))
                        if (size.isNotEmpty()) put("size", JsonPrimitive(size))
                        if (colour.isNotEmpty()) put("colour", JsonPrimitive(colour))
                        if (material.isNotEmpty()) put("material", JsonPrimitive(material))
                        put("seller_delivery_preference", JsonPrimitive(deliveryPreference))
                        if (deliveryNotes.isNotEmpty()) {
                            put("seller_delivery_notes", JsonPrimitive(deliveryNotes))
                        }
                        if (uploadedImageUrl != null) {
                            put("image_url", JsonPrimitive(uploadedImageUrl!!))
                        }
                        put("is_sold", JsonPrimitive(false))
                    }

                    Supabase.client.postgrest
                        .from("products")
                        .insert(newRow) {
                            select()
                        }

                    LanguageManager.makeToast(
                        this@SellActivity,
                        "Item listed successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Sell can be opened after the previous screen was finished by
                    // the bottom navigation. Always create a safe Home destination
                    // instead of leaving the task with no visible activity.
                    val homeIntent = Intent(this@SellActivity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()

                } catch (e: Exception) {
                    binding.btnList.isEnabled = true
                    binding.btnList.text = "List Item"
                    LanguageManager.makeToast(
                        this@SellActivity,
                        "Listing failed: ${e.message ?: "unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        setupBottomNav()
    }

    private fun uploadImage(uri: Uri) {
        val currentUser = Supabase.client.auth.currentUserOrNull()
        if (currentUser == null) {
            LanguageManager.makeToast(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.id
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$userId/$fileName"

        binding.llUploadPhoto.isEnabled = false
        imageUploadInProgress = true
        binding.btnList.isEnabled = false
        LanguageManager.makeToast(this, "Uploading...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val cacheFile = java.io.File(cacheDir, fileName)
            try {
                // 1. Copy the picked image to a cache file first (survives URI invalidation)
                contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Could not read selected image")

                // 2. Read the local copy into bytes
                val bytes = cacheFile.readBytes()
                if (bytes.size > 10 * 1024 * 1024) {
                    throw IllegalArgumentException("Image must be smaller than 10 MB")
                }

                // 3. Upload
                Supabase.client.storage
                    .from("product-images")
                    .upload(path, bytes) {
                        upsert = false
                        contentType = ContentType.parse(mimeType)
                    }

                // 4. Get the public URL
                val publicUrl = Supabase.client.storage
                    .from("product-images")
                    .publicUrl(path)

                uploadedImageUrl = publicUrl

                LanguageManager.makeToast(
                    this@SellActivity,
                    "Photo uploaded",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                LanguageManager.makeToast(
                    this@SellActivity,
                    "Upload failed: ${e.message ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                cacheFile.delete()
                binding.llUploadPhoto.isEnabled = true
                imageUploadInProgress = false
                binding.btnList.isEnabled = true
            }
        }
    }
}
