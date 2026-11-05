package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDetail(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("contact_type") val contactType: String = "",
    @SerialName("contact_value") val contactValue: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
