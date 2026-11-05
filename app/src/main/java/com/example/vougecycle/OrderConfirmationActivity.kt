package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.vougecycle.databinding.ActivityOrderConfirmationBinding
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class OrderConfirmationActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityOrderConfirmationBinding
    private var buyerAccounts: List<BankAccount> = emptyList()
    private var accountsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = Product(
            id = intent.getStringExtra(ProductDetailActivity.EXTRA_ID).orEmpty(),
            sellerId = intent.getStringExtra(ProductDetailActivity.EXTRA_SELLER_ID).orEmpty(),
            title = intent.getStringExtra(ProductDetailActivity.EXTRA_TITLE).orEmpty(),
            price = intent.getDoubleExtra(ProductDetailActivity.EXTRA_PRICE_VALUE, 0.0),
            imageUrl = intent.getStringExtra(ProductDetailActivity.EXTRA_IMAGE_URL),
            sellerDeliveryPreference = intent.getStringExtra(ProductDetailActivity.EXTRA_DELIVERY_PREFERENCE),
            isSold = intent.getBooleanExtra(ProductDetailActivity.EXTRA_SOLD, false)
        )

        binding.tvTitle.text = product.title
        binding.tvTitle.tag = LanguageManager.USER_CONTENT_TAG
        binding.tvPrice.text = product.priceFormatted()
        binding.ivProduct.load(product.imageUrl) {
            placeholder(R.color.vc_grey_bg)
            error(R.color.vc_grey_bg)
        }
        binding.ivBack.setOnClickListener { finish() }
        binding.btnConfirm.isEnabled = false
        binding.btnAddBankAccount.setOnClickListener {
            startActivity(Intent(this, BankAccountsActivity::class.java))
        }

        val couriers = listOf("Choose courier", "Pudo", "The Courier Guy", "Buffalo Courier")
        val deliveryMethods = when (product.sellerDeliveryPreference) {
            "Door-to-door only" -> listOf("Choose delivery method", "Door-to-door")
            "Locker collection only" -> listOf("Choose delivery method", "Locker collection")
            else -> listOf("Choose delivery method", "Door-to-door", "Locker collection")
        }
        binding.spCourier.adapter = LocalizedArrayAdapter(this, couriers)
        binding.spDeliveryMethod.adapter = LocalizedArrayAdapter(this, deliveryMethods)

        fun updateShippingTotal() {
            val courier = binding.spCourier.selectedItem?.toString().orEmpty()
            val method = binding.spDeliveryMethod.selectedItem?.toString().orEmpty()
            val shipping = shippingQuote(courier, method)
            binding.tvShippingCost.text = if (shipping == null) "Select courier and method" else "Shipping: R %,.2f".format(shipping.first)
            binding.tvDeliveryEstimate.text = shipping?.second ?: "Estimated delivery will appear here"
            binding.tvTotal.text = "Total: R %,.2f".format(product.price + (shipping?.first ?: 0.0))
        }
        val selectionListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = updateShippingTotal()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        binding.spCourier.onItemSelectedListener = selectionListener
        binding.spDeliveryMethod.onItemSelectedListener = selectionListener


        binding.btnConfirm.setOnClickListener {
            val deliveryName = binding.etDeliveryName.text.toString().trim()
            val deliveryPhone = binding.etDeliveryPhone.text.toString().trim()
            val deliveryAddress = binding.etDeliveryAddress.text.toString().trim()
            val paymentMethod = when (binding.rgPayment.checkedRadioButtonId) {
                R.id.rbCard -> "Card"
                R.id.rbEft -> "Instant EFT"
                R.id.rbPayShap -> "PayShap"
                else -> null
            }
            val courier = binding.spCourier.selectedItem.toString()
            val deliveryMethod = binding.spDeliveryMethod.selectedItem.toString()
            val shipping = shippingQuote(courier, deliveryMethod)
            val selectedAccount = buyerAccounts.getOrNull(binding.spBankAccount.selectedItemPosition)

            if (deliveryName.length < 2) {
                binding.etDeliveryName.error = "Enter the recipient's name"
                return@setOnClickListener
            }
            val phoneDigits = deliveryPhone.filter(Char::isDigit)
            if (phoneDigits.length !in 10..11) {
                binding.etDeliveryPhone.error = "Enter a valid South African phone number"
                return@setOnClickListener
            }
            if (deliveryAddress.length < 10) {
                binding.etDeliveryAddress.error = "Enter the full delivery address"
                return@setOnClickListener
            }
            if (paymentMethod == null) {
                LanguageManager.makeToast(this, "Choose a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (shipping == null) {
                LanguageManager.makeToast(this, "Choose a courier and delivery method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!accountsLoaded || selectedAccount == null) {
                LanguageManager.makeToast(this, "Add and choose a bank account first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (selectedAccount.balance < product.price + shipping.first) {
                LanguageManager.makeToast(this, "The selected demo account does not have enough balance", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val paymentReference = "VC-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}"
            binding.btnConfirm.isEnabled = false
            binding.btnConfirm.text = "Processing demo payment..."
            lifecycleScope.launch {
                try {
                    MarketplaceRepository.createDemoPaidOrder(
                        product = product,
                        deliveryName = deliveryName,
                        deliveryPhone = deliveryPhone,
                        deliveryAddress = deliveryAddress,
                        paymentMethod = paymentMethod,
                        paymentReference = paymentReference,
                        courier = courier,
                        deliveryMethod = deliveryMethod,
                        shippingCost = shipping.first,
                        estimatedDelivery = shipping.second,
                        buyerBankAccountId = selectedAccount.id
                    )
                    startActivity(Intent(this@OrderConfirmationActivity, PaymentSuccessActivity::class.java).apply {
                        putExtra(PaymentSuccessActivity.EXTRA_AMOUNT, "R %,.2f".format(product.price + shipping.first))
                        putExtra(PaymentSuccessActivity.EXTRA_METHOD, paymentMethod)
                        putExtra(PaymentSuccessActivity.EXTRA_REFERENCE, paymentReference)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                } catch (e: Exception) {
                    binding.btnConfirm.isEnabled = true
                    binding.btnConfirm.text = "Pay securely (Demo)"
                    LanguageManager.makeToast(this@OrderConfirmationActivity, AppReliability.friendlyMessage(e, "complete payment"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadBuyerAccounts()
    }

    private fun loadBuyerAccounts() {
        lifecycleScope.launch {
            try {
                buyerAccounts = MarketplaceRepository.bankAccounts()
                accountsLoaded = true
                val labels = if (buyerAccounts.isEmpty()) {
                    listOf("No bank accounts saved — add one in Settings")
                } else buyerAccounts.map { "${it.displayName()} — R %,.2f".format(it.balance) }
                binding.spBankAccount.adapter = ArrayAdapter(
                    this@OrderConfirmationActivity,
                    android.R.layout.simple_spinner_item,
                    labels.map { LanguageManager.translate(this@OrderConfirmationActivity, it) }
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                binding.btnConfirm.isEnabled = buyerAccounts.isNotEmpty()
            } catch (e: Exception) {
                accountsLoaded = false
                binding.btnConfirm.isEnabled = false
                LanguageManager.makeToast(this@OrderConfirmationActivity, AppReliability.friendlyMessage(e, "load bank accounts"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shippingQuote(courier: String, method: String): Pair<Double, String>? {
        if (courier == "Choose courier" || method == "Choose delivery method") return null
        return when (courier to method) {
            "Pudo" to "Locker collection" -> 60.0 to "Estimated delivery: 2–4 working days"
            "Pudo" to "Door-to-door" -> 85.0 to "Estimated delivery: 3–5 working days"
            "The Courier Guy" to "Locker collection" -> 70.0 to "Estimated delivery: 2–4 working days"
            "The Courier Guy" to "Door-to-door" -> 110.0 to "Estimated delivery: 2–3 working days"
            "Buffalo Courier" to "Locker collection" -> 65.0 to "Estimated delivery: 3–5 working days"
            "Buffalo Courier" to "Door-to-door" -> 95.0 to "Estimated delivery: 3–5 working days"
            else -> null
        }
    }
}
