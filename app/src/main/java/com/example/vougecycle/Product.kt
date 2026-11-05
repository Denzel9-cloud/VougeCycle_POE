package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    @SerialName("id") val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("price") val price: Double = 0.0,
    @SerialName("category") val category: String? = null,
    @SerialName("condition") val condition: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("colour") val colour: String? = null,
    @SerialName("material") val material: String? = null,
    @SerialName("seller_delivery_preference") val sellerDeliveryPreference: String? = null,
    @SerialName("seller_delivery_notes") val sellerDeliveryNotes: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_sold") val isSold: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
) {
    val isCatalogPreview: Boolean
        get() = id.startsWith("catalog_")

    fun priceFormatted(): String =
        if (isCatalogPreview && price <= 0.0) "Price to be added" else "R %,.2f".format(price)
}
