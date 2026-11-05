package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vougecycle.databinding.ActivityChatBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var conversation: Conversation
    private lateinit var adapter: ChatMessageAdapter
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversation = Conversation(
            id = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty(),
            productId = intent.getStringExtra(EXTRA_PRODUCT_ID).orEmpty(),
            buyerId = intent.getStringExtra(EXTRA_BUYER_ID).orEmpty(),
            sellerId = intent.getStringExtra(EXTRA_SELLER_ID).orEmpty()
        )
        binding.tvChatName.text = intent.getStringExtra(EXTRA_NAME) ?: "Chat"
        if (intent.hasExtra(EXTRA_NAME)) binding.tvChatName.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvChatProduct.text = intent.getStringExtra(EXTRA_PRODUCT_TITLE) ?: "Product"
        if (intent.hasExtra(EXTRA_PRODUCT_TITLE)) binding.tvChatProduct.tag = LanguageManager.USER_CONTENT_TAG
        binding.ivBack.setOnClickListener { finish() }

        adapter = ChatMessageAdapter(mutableListOf(), MessagingRepository.currentUserId())
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            binding.btnSend.isEnabled = false
            lifecycleScope.launch {
                try {
                    MessagingRepository.sendMessage(conversation, text)
                    binding.etMessage.text?.clear()
                    loadMessages()
                } catch (e: Exception) {
                    LanguageManager.makeToast(this@ChatActivity, AppReliability.friendlyMessage(e, "send message"), Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                loadMessages()
                delay(2000)
            }
        }
    }

    override fun onStop() {
        refreshJob?.cancel()
        refreshJob = null
        super.onStop()
    }

    private suspend fun loadMessages() {
        try {
            val items = MessagingRepository.messages(conversation.id)
            adapter.replace(items)
            if (items.isNotEmpty()) binding.rvMessages.scrollToPosition(items.lastIndex)
            MessagingRepository.markConversationRead(conversation.id)
        } catch (_: Exception) {
            // The next live refresh retries automatically.
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val EXTRA_PRODUCT_ID = "product_id"
        private const val EXTRA_BUYER_ID = "buyer_id"
        private const val EXTRA_SELLER_ID = "seller_id"
        private const val EXTRA_NAME = "chat_name"
        private const val EXTRA_PRODUCT_TITLE = "product_title"

        fun intentFor(context: Context, item: ConversationDisplay) = Intent(context, ChatActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_ID, item.conversation.id)
            putExtra(EXTRA_PRODUCT_ID, item.conversation.productId)
            putExtra(EXTRA_BUYER_ID, item.conversation.buyerId)
            putExtra(EXTRA_SELLER_ID, item.conversation.sellerId)
            putExtra(EXTRA_NAME, item.otherUserName)
            putExtra(EXTRA_PRODUCT_TITLE, item.productTitle)
        }

        fun intentFor(context: Context, conversation: Conversation, name: String, productTitle: String) =
            Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(EXTRA_PRODUCT_ID, conversation.productId)
                putExtra(EXTRA_BUYER_ID, conversation.buyerId)
                putExtra(EXTRA_SELLER_ID, conversation.sellerId)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_PRODUCT_TITLE, productTitle)
            }
    }
}
