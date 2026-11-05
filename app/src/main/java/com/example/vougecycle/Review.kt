package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    @SerialName("id") val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("rating") val rating: Int = 0,
    @SerialName("comment") val comment: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

data class ReviewDisplay(val review: Review, val buyerName: String)
