package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id") val id: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("shop_links") val shopLinks: String? = null
)
