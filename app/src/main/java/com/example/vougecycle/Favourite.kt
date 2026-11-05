package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favourite(
    @SerialName("user_id") val userId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
