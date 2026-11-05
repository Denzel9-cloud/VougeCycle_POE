package com.example.vougecycle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class OrderAdapter(
    private val items: MutableList<OrderDisplay>,
    private val sellerMode: Boolean,
    private val onAdvance: (OrderDisplay, Int) -> Unit = { _, _ -> },
    private val onTrack: (OrderDisplay) -> Unit = {},
    private val onReview: (OrderDisplay) -> Unit = {}
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivProduct)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val shipping: TextView = view.findViewById(R.id.tvShipping)
        val action: Button = view.findViewById(R.id.btnAction)
        val track: Button = view.findViewById(R.id.btnTrack)
        val review: Button = view.findViewById(R.id.btnReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false))
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.product?.title ?: "Product unavailable"
        if (item.product != null) holder.title.tag = LanguageManager.USER_CONTENT_TAG
        else holder.title.tag = null
        holder.amount.text = "Total ${item.order.totalFormatted()}"
        holder.status.text = item.order.status.uppercase()
        holder.shipping.text = listOfNotNull(
            item.order.courier,
            item.order.deliveryMethod,
            item.order.trackingNumber
        ).joinToString(" • ").ifBlank { "Shipping details unavailable" }
        if (item.order.courier != null || item.order.deliveryMethod != null || item.order.trackingNumber != null) {
            holder.shipping.tag = LanguageManager.USER_CONTENT_TAG
        } else {
            holder.shipping.tag = null
        }
        holder.image.load(item.product?.imageUrl) {
            placeholder(R.color.vc_grey_bg)
            error(R.color.vc_grey_bg)
        }

        val nextLabel = when (item.order.status) {
            "pending" -> "Mark paid"
            "paid" -> "Prepare parcel"
            "preparing" -> "Dispatch parcel"
            "shipped" -> "Out for delivery"
            "out_for_delivery" -> "Mark delivered"
            else -> null
        }
        holder.action.visibility = if (sellerMode && nextLabel != null) View.VISIBLE else View.GONE
        holder.action.text = nextLabel ?: ""
        holder.action.setOnClickListener { onAdvance(item, position) }
        holder.track.visibility = if (sellerMode) View.GONE else View.VISIBLE
        holder.track.setOnClickListener { onTrack(item) }
        holder.review.visibility = if (!sellerMode && item.order.status == "delivered") View.VISIBLE else View.GONE
        holder.review.setOnClickListener { onReview(item) }
    }

    override fun getItemCount(): Int = items.size

    fun replace(position: Int, item: OrderDisplay) {
        items[position] = item
        notifyItemChanged(position)
    }
}
