package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    @SerialName("id") val id: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("buyer_bank_account_id") val buyerBankAccountId: String? = null,
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller_bank_account_id") val sellerBankAccountId: String? = null,
    @SerialName("product_id") val productId: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("status") val status: String = "pending",
    @SerialName("delivery_name") val deliveryName: String? = null,
    @SerialName("delivery_phone") val deliveryPhone: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_reference") val paymentReference: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("courier") val courier: String? = null,
    @SerialName("delivery_method") val deliveryMethod: String? = null,
    @SerialName("shipping_cost") val shippingCost: Double = 0.0,
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("shipped_at") val shippedAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun amountFormatted(): String = "R %,.2f".format(amount)
    fun shippingFormatted(): String = "R %,.2f".format(shippingCost)
    fun totalFormatted(): String = "R %,.2f".format(amount + shippingCost)
}

data class OrderDisplay(val order: Order, val product: Product?)
