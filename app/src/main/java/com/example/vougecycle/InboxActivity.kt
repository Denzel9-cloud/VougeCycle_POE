package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vougecycle.databinding.ActivityInboxBinding
import kotlinx.coroutines.launch

class InboxActivity : BaseActivity() {
    override val selectedTab: Tab = Tab.INBOX
    private lateinit var binding: ActivityInboxBinding
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        binding.ivSearch.setOnClickListener {
            LanguageManager.makeToast(this, "Conversation search will be added later", Toast.LENGTH_SHORT).show()
        }
        binding.btnRetryInbox.setOnClickListener { loadConversations() }
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun loadConversations() {
        if (isLoading) return
        isLoading = true
        binding.progressInbox.visibility = View.VISIBLE
        binding.btnRetryInbox.visibility = View.GONE
        binding.llEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                if (!AppReliability.isOnline(this@InboxActivity)) throw IllegalStateException("network unavailable")
                val items = MessagingRepository.conversationList()
                binding.rvConversations.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                binding.llEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.rvConversations.adapter = ConversationAdapter(items) { item ->
                    startActivity(ChatActivity.intentFor(this@InboxActivity, item))
                }
            } catch (e: Exception) {
                binding.rvConversations.visibility = View.GONE
                binding.llEmpty.visibility = View.VISIBLE
                binding.btnRetryInbox.visibility = View.VISIBLE
                LanguageManager.makeToast(this@InboxActivity, AppReliability.friendlyMessage(e, "load messages"), Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                binding.progressInbox.visibility = View.GONE
            }
        }
    }
}
