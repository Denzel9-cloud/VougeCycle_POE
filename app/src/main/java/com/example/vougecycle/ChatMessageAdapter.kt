package com.example.vougecycle

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.tvMessageBody)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
    )
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = messages[position]
        val outgoing = item.senderId == currentUserId
        holder.message.text = item.body
        holder.message.tag = LanguageManager.USER_CONTENT_TAG
        holder.message.setTextColor(if (outgoing) Color.WHITE else Color.parseColor("#1A1A1A"))
        holder.message.setBackgroundResource(if (outgoing) R.drawable.bg_message_outgoing else R.drawable.bg_message_incoming)
        val params = holder.message.layoutParams as ViewGroup.MarginLayoutParams
        if (params is android.widget.FrameLayout.LayoutParams) {
            params.gravity = if (outgoing) Gravity.END else Gravity.START
            holder.message.layoutParams = params
        }
    }
    override fun getItemCount() = messages.size
    fun replace(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
