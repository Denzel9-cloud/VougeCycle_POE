package com.example.vougecycle

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object ReviewRepository {
    private fun userId(): String = Supabase.client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("You must be logged in")

    suspend fun reviewsForSeller(sellerId: String): List<ReviewDisplay> {
        val reviews = Supabase.client.postgrest.from("reviews").select()
            .decodeList<Review>()
            .filter { it.sellerId == sellerId }
            .sortedByDescending { it.createdAt }
        val profiles = Supabase.client.postgrest.from("profiles").select().decodeList<Profile>()
        return reviews.map { review ->
            val profile = profiles.firstOrNull { it.id == review.buyerId }
            val name = profile?.username?.takeIf { it.isNotBlank() }
                ?: profile?.fullName?.takeIf { it.isNotBlank() }
                ?: "Buyer"
            ReviewDisplay(review, if (name.startsWith("@")) name else "@$name")
        }
    }

    suspend fun reviewForOrder(orderId: String): Review? =
        Supabase.client.postgrest.from("reviews").select().decodeList<Review>()
            .firstOrNull { it.orderId == orderId }

    suspend fun saveReview(order: Order, rating: Int, comment: String, existingId: String?) {
        require(order.status == "delivered") { "The order must be delivered before it can be reviewed" }
        require(rating in 1..5) { "Choose a rating from 1 to 5 stars" }
        require(comment.trim().length in 3..500) { "Review must contain 3 to 500 characters" }
        val uid = userId()
        require(uid == order.buyerId) { "Only the buyer can review this order" }

        val values = buildJsonObject {
            put("rating", JsonPrimitive(rating))
            put("comment", JsonPrimitive(comment.trim()))
        }
        if (existingId == null) {
            val row = buildJsonObject {
                put("order_id", JsonPrimitive(order.id))
                put("buyer_id", JsonPrimitive(uid))
                put("seller_id", JsonPrimitive(order.sellerId))
                put("product_id", JsonPrimitive(order.productId))
                put("rating", JsonPrimitive(rating))
                put("comment", JsonPrimitive(comment.trim()))
            }
            Supabase.client.postgrest.from("reviews").insert(row)
        } else {
            Supabase.client.postgrest.from("reviews").update(values) {
                filter { eq("id", existingId) }
            }
        }
    }

    suspend fun deleteReview(reviewId: String) {
        Supabase.client.postgrest.from("reviews").delete {
            filter { eq("id", reviewId) }
        }
    }
}
