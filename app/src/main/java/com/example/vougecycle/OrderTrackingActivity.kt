package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vougecycle.databinding.ActivityOrderTrackingBinding

class OrderTrackingActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityOrderTrackingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val status = intent.getStringExtra(EXTRA_STATUS) ?: "paid"
        binding.tvProductTitle.text = intent.getStringExtra(EXTRA_PRODUCT) ?: "Order"
        if (intent.hasExtra(EXTRA_PRODUCT)) binding.tvProductTitle.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvCourier.text = intent.getStringExtra(EXTRA_COURIER) ?: "Courier not selected"
        binding.tvDeliveryMethod.text = intent.getStringExtra(EXTRA_METHOD) ?: "Delivery method unavailable"
        binding.tvTracking.text = intent.getStringExtra(EXTRA_TRACKING) ?: "Generated when the seller dispatches your parcel"
        if (intent.hasExtra(EXTRA_TRACKING)) binding.tvTracking.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvEstimate.text = intent.getStringExtra(EXTRA_ESTIMATE) ?: "Delivery estimate unavailable"
        binding.tvDeliveryAddress.text = intent.getStringExtra(EXTRA_ADDRESS) ?: "Address unavailable"
        if (intent.hasExtra(EXTRA_ADDRESS)) binding.tvDeliveryAddress.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvOrderTotal.text = intent.getStringExtra(EXTRA_TOTAL) ?: "R 0.00"
        binding.ivBack.setOnClickListener { finish() }

        val currentStep = when (status) {
            "paid" -> 0
            "preparing" -> 1
            "shipped" -> 2
            "out_for_delivery" -> 3
            "delivered" -> 4
            else -> 0
        }
        val steps = listOf(binding.tvPaid, binding.tvPreparing, binding.tvShipped, binding.tvOutForDelivery, binding.tvDelivered)
        steps.forEachIndexed { index, textView ->
            textView.text = if (index <= currentStep) "✓  ${textView.text}" else "○  ${textView.text}"
            textView.setTextColor(getColor(if (index <= currentStep) R.color.vc_green else R.color.vc_grey_text))
            textView.setTypeface(null, if (index == currentStep) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    companion object {
        private const val EXTRA_STATUS = "status"
        private const val EXTRA_PRODUCT = "product"
        private const val EXTRA_COURIER = "courier"
        private const val EXTRA_METHOD = "method"
        private const val EXTRA_TRACKING = "tracking"
        private const val EXTRA_ESTIMATE = "estimate"
        private const val EXTRA_ADDRESS = "address"
        private const val EXTRA_TOTAL = "total"

        fun intentFor(context: Context, item: OrderDisplay) = Intent(context, OrderTrackingActivity::class.java).apply {
            putExtra(EXTRA_STATUS, item.order.status)
            putExtra(EXTRA_PRODUCT, item.product?.title ?: "Order")
            putExtra(EXTRA_COURIER, item.order.courier)
            putExtra(EXTRA_METHOD, item.order.deliveryMethod)
            putExtra(EXTRA_TRACKING, item.order.trackingNumber)
            putExtra(EXTRA_ESTIMATE, item.order.estimatedDelivery)
            putExtra(EXTRA_ADDRESS, item.order.deliveryAddress)
            putExtra(EXTRA_TOTAL, item.order.totalFormatted())
        }
    }
}
