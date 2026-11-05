package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vougecycle.databinding.ActivitySellerReviewsBinding
import kotlinx.coroutines.launch

class SellerReviewsActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivitySellerReviewsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }
        binding.tvSellerName.text = intent.getStringExtra(EXTRA_NAME) ?: "Seller reviews"
        if (intent.hasExtra(EXTRA_NAME)) binding.tvSellerName.tag = LanguageManager.USER_CONTENT_TAG
        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        loadReviews()
    }
    private fun loadReviews() {
        val sellerId = intent.getStringExtra(EXTRA_SELLER_ID).orEmpty()
        lifecycleScope.launch {
            try {
                val reviews = ReviewRepository.reviewsForSeller(sellerId)
                binding.rvReviews.adapter = ReviewAdapter(reviews)
                binding.tvEmpty.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                binding.rvReviews.visibility = if (reviews.isEmpty()) View.GONE else View.VISIBLE
                val average = reviews.map { it.review.rating }.average()
                binding.tvSummary.text = if (reviews.isEmpty()) "No reviews yet" else "★ %.1f from ${reviews.size} review${if (reviews.size == 1) "" else "s"}".format(average)
            } catch (e: Exception) {
                LanguageManager.makeToast(this@SellerReviewsActivity, "Could not load reviews: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    companion object {
        private const val EXTRA_SELLER_ID = "seller_id"
        private const val EXTRA_NAME = "seller_name"
        fun intentFor(context: Context, sellerId: String, name: String) = Intent(context, SellerReviewsActivity::class.java).apply {
            putExtra(EXTRA_SELLER_ID, sellerId)
            putExtra(EXTRA_NAME, name)
        }
    }
}
