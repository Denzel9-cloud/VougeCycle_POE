package com.example.vougecycle

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object NotificationRepository {
    private fun userId(): String = Supabase.client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("You must be logged in")

    suspend fun list(): List<AppNotification> {
        val uid = userId()
        return Supabase.client.postgrest.from("notifications").select()
            .decodeList<AppNotification>()
            .filter { it.userId == uid }
            .sortedByDescending { it.createdAt }
    }

    suspend fun markRead(notificationId: String) {
        val uid = userId()
        Supabase.client.postgrest.from("notifications").update(
            buildJsonObject { put("is_read", JsonPrimitive(true)) }
        ) {
            filter {
                eq("id", notificationId)
                eq("user_id", uid)
            }
        }
    }

    suspend fun markAllRead() {
        val uid = userId()
        Supabase.client.postgrest.from("notifications").update(
            buildJsonObject { put("is_read", JsonPrimitive(true)) }
        ) {
            filter {
                eq("user_id", uid)
                eq("is_read", false)
            }
        }
    }
}
