package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vougecycle.databinding.ActivityPaymentSuccessBinding

class PaymentSuccessActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityPaymentSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvAmount.text = intent.getStringExtra(EXTRA_AMOUNT).orEmpty()
        binding.tvMethod.text = intent.getStringExtra(EXTRA_METHOD).orEmpty()
        binding.tvReference.text = intent.getStringExtra(EXTRA_REFERENCE).orEmpty()

        binding.btnViewOrders.setOnClickListener {
            startActivity(Intent(this, MyPurchasesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }

    companion object {
        const val EXTRA_AMOUNT = "payment_amount"
        const val EXTRA_METHOD = "payment_method"
        const val EXTRA_REFERENCE = "payment_reference"
    }
}
