package com.example.vougecycle

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object ContactDetailsRepository {
    private fun userId(): String = Supabase.client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("You must be logged in")

    suspend fun list(): List<ContactDetail> {
        val uid = userId()
        return Supabase.client.postgrest.from("user_contact_details").select()
            .decodeList<ContactDetail>()
            .filter { it.userId == uid }
            .sortedBy { it.contactType }
    }

    suspend fun add(type: String, value: String) {
        val uid = userId()
        require(type == "phone" || type == "email") { "Choose a phone number or email address" }
        require(value.isNotBlank()) { "Enter a contact detail" }
        val row = buildJsonObject {
            put("user_id", JsonPrimitive(uid))
            put("contact_type", JsonPrimitive(type))
            put("contact_value", JsonPrimitive(value.trim()))
        }
        Supabase.client.postgrest.from("user_contact_details").insert(row)
    }

    suspend fun delete(contactId: String) {
        val uid = userId()
        Supabase.client.postgrest.from("user_contact_details").delete {
            filter {
                eq("id", contactId)
                eq("user_id", uid)
            }
        }
    }
}
