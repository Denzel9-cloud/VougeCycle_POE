package com.example.vougecycle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankAccount(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_name") val accountName: String = "",
    @SerialName("opening_balance") val openingBalance: Double = 0.0,
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun displayName() = "$accountName • $bankName"
}

@Serializable
data class AccountTransaction(
    @SerialName("id") val id: String = "",
    @SerialName("account_id") val accountId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("transaction_type") val transactionType: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("payment_reference") val paymentReference: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
