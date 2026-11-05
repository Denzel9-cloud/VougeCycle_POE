package com.example.vougecycle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConversationAdapter(
    private val conversations: List<ConversationDisplay>,
    private val onRowClick: (ConversationDisplay) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {
    class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvUnread: TextView = view.findViewById(R.id.tvUnread)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConversationViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
    )
    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = conversations[position]
        holder.tvName.text = item.otherUserName
        holder.tvName.tag = LanguageManager.USER_CONTENT_TAG
        holder.tvLastMessage.text = "${item.productTitle}: ${item.lastMessage}"
        holder.tvLastMessage.tag = LanguageManager.USER_CONTENT_TAG
        holder.tvTime.text = item.timeAgo
        holder.tvUnread.visibility = if (item.unreadCount > 0) View.VISIBLE else View.GONE
        holder.tvUnread.text = item.unreadCount.toString()
        holder.itemView.setOnClickListener { onRowClick(item) }
    }
    override fun getItemCount() = conversations.size
}
