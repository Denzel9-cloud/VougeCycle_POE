package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    @SerialName("id") val id: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

data class ConversationDisplay(
    val conversation: Conversation,
    val otherUserName: String,
    val productTitle: String,
    val lastMessage: String,
    val timeAgo: String,
    val unreadCount: Int = 0
)
