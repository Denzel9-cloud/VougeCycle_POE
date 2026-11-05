package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityReviewBinding
import kotlinx.coroutines.launch

class ReviewActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityReviewBinding
    private lateinit var order: Order
    private var existingReview: Review? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        order = Order(
            id = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty(),
            buyerId = intent.getStringExtra(EXTRA_BUYER_ID).orEmpty(),
            sellerId = intent.getStringExtra(EXTRA_SELLER_ID).orEmpty(),
            productId = intent.getStringExtra(EXTRA_PRODUCT_ID).orEmpty(),
            status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        )
        binding.tvProductName.text = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: "Purchased item"
        if (intent.hasExtra(EXTRA_PRODUCT_NAME)) binding.tvProductName.tag = LanguageManager.USER_CONTENT_TAG
        binding.ivBack.setOnClickListener { finish() }
        binding.btnSaveReview.setOnClickListener { saveReview() }
        binding.btnDeleteReview.setOnClickListener { confirmDelete() }
        loadExistingReview()
    }

    private fun loadExistingReview() {
        lifecycleScope.launch {
            try {
                existingReview = ReviewRepository.reviewForOrder(order.id)
                existingReview?.let {
                    binding.ratingBar.rating = it.rating.toFloat()
                    binding.etReview.setText(it.comment)
                    binding.btnSaveReview.text = "Update review"
                    binding.btnDeleteReview.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                LanguageManager.makeToast(this@ReviewActivity, AppReliability.friendlyMessage(e, "load review"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveReview() {
        val rating = binding.ratingBar.rating.toInt()
        val comment = binding.etReview.text.toString().trim()
        if (rating !in 1..5) {
            LanguageManager.makeToast(this, "Please choose 1 to 5 stars", Toast.LENGTH_SHORT).show()
            return
        }
        if (comment.length !in 3..500) {
            binding.etReview.error = "Write between 3 and 500 characters"
            return
        }
        binding.btnSaveReview.isEnabled = false
        lifecycleScope.launch {
            try {
                ReviewRepository.saveReview(order, rating, comment, existingReview?.id)
                LanguageManager.makeToast(this@ReviewActivity, "Review saved", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                binding.btnSaveReview.isEnabled = true
                LanguageManager.makeToast(this@ReviewActivity, AppReliability.friendlyMessage(e, "save review"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDelete() {
        val review = existingReview ?: return
        AlertDialog.Builder(this)
            .setTitle(LanguageManager.translate(this, "Delete review"))
            .setMessage(LanguageManager.translate(this, "Are you sure you want to delete this review?"))
            .setNegativeButton(LanguageManager.translate(this, "Cancel"), null)
            .setPositiveButton(LanguageManager.translate(this, "Delete")) { _, _ ->
                lifecycleScope.launch {
                    try {
                        ReviewRepository.deleteReview(review.id)
                        LanguageManager.makeToast(this@ReviewActivity, "Review deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        LanguageManager.makeToast(this@ReviewActivity, AppReliability.friendlyMessage(e, "delete review"), Toast.LENGTH_LONG).show()
                    }
                }
            }.show()
    }

    companion object {
        private const val EXTRA_ORDER_ID = "order_id"
        private const val EXTRA_BUYER_ID = "buyer_id"
        private const val EXTRA_SELLER_ID = "seller_id"
        private const val EXTRA_PRODUCT_ID = "product_id"
        private const val EXTRA_STATUS = "status"
        private const val EXTRA_PRODUCT_NAME = "product_name"

        fun intentFor(context: Context, item: OrderDisplay) = Intent(context, ReviewActivity::class.java).apply {
            putExtra(EXTRA_ORDER_ID, item.order.id)
            putExtra(EXTRA_BUYER_ID, item.order.buyerId)
            putExtra(EXTRA_SELLER_ID, item.order.sellerId)
            putExtra(EXTRA_PRODUCT_ID, item.order.productId)
            putExtra(EXTRA_STATUS, item.order.status)
            putExtra(EXTRA_PRODUCT_NAME, item.product?.title ?: "Purchased item")
        }
    }
}
