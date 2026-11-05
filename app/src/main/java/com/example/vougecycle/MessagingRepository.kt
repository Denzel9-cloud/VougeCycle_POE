package com.example.vougecycle

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant

object MessagingRepository {
    fun currentUserId(): String = Supabase.client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("You must be logged in")

    suspend fun getOrCreateConversation(product: Product): Conversation {
        val uid = currentUserId()
        require(uid != product.sellerId) { "You cannot message yourself" }

        findConversation(product.id, uid, product.sellerId)?.let { return it }

        val row = buildJsonObject {
            put("product_id", JsonPrimitive(product.id))
            put("buyer_id", JsonPrimitive(uid))
            put("seller_id", JsonPrimitive(product.sellerId))
        }
        Supabase.client.postgrest.from("conversations").insert(row)
        return findConversation(product.id, uid, product.sellerId)
            ?: throw IllegalStateException("Could not open the conversation")
    }

    private suspend fun findConversation(productId: String, buyerId: String, sellerId: String): Conversation? =
        Supabase.client.postgrest.from("conversations").select()
            .decodeList<Conversation>()
            .firstOrNull {
                it.productId == productId && it.buyerId == buyerId && it.sellerId == sellerId
            }

    suspend fun conversationList(): List<ConversationDisplay> {
        val uid = currentUserId()
        val conversations = Supabase.client.postgrest.from("conversations").select()
            .decodeList<Conversation>()
            .filter { it.buyerId == uid || it.sellerId == uid }
        if (conversations.isEmpty()) return emptyList()

        val messages = Supabase.client.postgrest.from("messages").select().decodeList<ChatMessage>()
        val profiles = Supabase.client.postgrest.from("profiles").select().decodeList<Profile>()
        val products = Supabase.client.postgrest.from("products").select().decodeList<Product>()

        return conversations.map { conversation ->
            val conversationMessages = messages.filter { it.conversationId == conversation.id }
                .sortedBy { it.createdAt }
            val otherId = if (conversation.buyerId == uid) conversation.sellerId else conversation.buyerId
            val profile = profiles.firstOrNull { it.id == otherId }
            val name = profile?.username?.takeIf { it.isNotBlank() }
                ?: profile?.fullName?.takeIf { it.isNotBlank() }
                ?: "VougeCycle user"
            val productTitle = products.firstOrNull { it.id == conversation.productId }?.title ?: "Product"
            val latest = conversationMessages.lastOrNull()
            ConversationDisplay(
                conversation = conversation,
                otherUserName = if (name.startsWith("@")) name else "@$name",
                productTitle = productTitle,
                lastMessage = latest?.body ?: "Start the conversation",
                timeAgo = displayTime(latest?.createdAt ?: conversation.updatedAt),
                unreadCount = conversationMessages.count { it.receiverId == uid && it.readAt == null }
            )
        }.sortedByDescending { it.conversation.updatedAt }
    }

    suspend fun messages(conversationId: String): List<ChatMessage> =
        Supabase.client.postgrest.from("messages").select()
            .decodeList<ChatMessage>()
            .filter { it.conversationId == conversationId }
            .sortedBy { it.createdAt }

    suspend fun sendMessage(conversation: Conversation, body: String) {
        val uid = currentUserId()
        require(body.isNotBlank()) { "Message cannot be empty" }
        require(body.length <= 1000) { "Message is too long" }
        val receiverId = when (uid) {
            conversation.buyerId -> conversation.sellerId
            conversation.sellerId -> conversation.buyerId
            else -> throw IllegalStateException("You cannot access this conversation")
        }
        val row = buildJsonObject {
            put("conversation_id", JsonPrimitive(conversation.id))
            put("sender_id", JsonPrimitive(uid))
            put("receiver_id", JsonPrimitive(receiverId))
            put("body", JsonPrimitive(body.trim()))
        }
        Supabase.client.postgrest.from("messages").insert(row)
    }

    suspend fun markConversationRead(conversationId: String) {
        val uid = currentUserId()
        Supabase.client.postgrest.from("messages").update(
            buildJsonObject { put("read_at", JsonPrimitive(Instant.now().toString())) }
        ) {
            filter {
                eq("conversation_id", conversationId)
                eq("receiver_id", uid)
            }
        }
    }

    private fun displayTime(value: String): String = when {
        value.isBlank() -> "Now"
        value.length >= 16 -> value.substring(11, 16)
        else -> "Now"
    }
}
