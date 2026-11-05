package com.example.vougecycle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewAdapter(private val items: List<ReviewDisplay>) : RecyclerView.Adapter<ReviewAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvReviewerName)
        val stars: TextView = view.findViewById(R.id.tvReviewStars)
        val comment: TextView = view.findViewById(R.id.tvReviewComment)
        val date: TextView = view.findViewById(R.id.tvReviewDate)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
    )
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.buyerName
        holder.name.tag = LanguageManager.USER_CONTENT_TAG
        holder.stars.text = "★".repeat(item.review.rating) + "☆".repeat(5 - item.review.rating)
        holder.comment.text = item.review.comment
        holder.comment.tag = LanguageManager.USER_CONTENT_TAG
        holder.date.text = item.review.createdAt.take(10)
    }
    override fun getItemCount() = items.size
}
