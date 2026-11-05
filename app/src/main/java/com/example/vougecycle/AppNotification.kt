package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("notification_type") val notificationType: String = "",
    @SerialName("related_order_id") val relatedOrderId: String? = null,
    @SerialName("related_message_id") val relatedMessageId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)
