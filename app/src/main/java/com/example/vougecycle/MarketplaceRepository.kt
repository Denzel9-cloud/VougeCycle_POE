package com.example.vougecycle

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object MarketplaceRepository {
    private fun userId(): String = Supabase.client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("You must be logged in")

    suspend fun bankAccounts(): List<BankAccount> {
        val uid = userId()
        return Supabase.client.postgrest.from("bank_accounts").select()
            .decodeList<BankAccount>()
            .filter { it.userId == uid && it.isActive }
            .sortedByDescending { it.isDefault }
    }

    suspend fun addBankAccount(bankName: String, accountName: String, openingBalance: Double) {
        val uid = userId()
        require(bankName.isNotBlank() && accountName.isNotBlank()) { "Enter the bank and account names" }
        require(openingBalance >= 0.0) { "Starting balance cannot be negative" }
        val existing = bankAccounts()
        require(existing.size < 3) { "You can add up to three bank accounts" }
        val row = buildJsonObject {
            put("user_id", JsonPrimitive(uid))
            put("bank_name", JsonPrimitive(bankName.trim()))
            put("account_name", JsonPrimitive(accountName.trim()))
            put("opening_balance", JsonPrimitive(openingBalance))
            put("balance", JsonPrimitive(openingBalance))
            put("is_default", JsonPrimitive(existing.isEmpty()))
        }
        Supabase.client.postgrest.from("bank_accounts").insert(row)
    }

    suspend fun setDefaultBankAccount(accountId: String) {
        val uid = userId()
        Supabase.client.postgrest.from("bank_accounts").update(
            buildJsonObject { put("is_default", JsonPrimitive(false)) }
        ) { filter { eq("user_id", uid) } }
        Supabase.client.postgrest.from("bank_accounts").update(
            buildJsonObject { put("is_default", JsonPrimitive(true)) }
        ) { filter { eq("id", accountId); eq("user_id", uid) } }
    }

    suspend fun updateBankAccountBalance(accountId: String, newBalance: Double) {
        val uid = userId()
        require(newBalance >= 0.0) { "Balance cannot be negative" }
        Supabase.client.postgrest.from("bank_accounts").update(
            buildJsonObject { put("balance", JsonPrimitive(newBalance)) }
        ) { filter { eq("id", accountId); eq("user_id", uid); eq("is_active", true) } }
    }

    suspend fun deleteBankAccount(accountId: String) {
        val uid = userId()
        Supabase.client.postgrest.from("bank_accounts").update(
            buildJsonObject {
                put("is_active", JsonPrimitive(false))
                put("is_default", JsonPrimitive(false))
            }
        ) { filter { eq("id", accountId); eq("user_id", uid); eq("is_active", true) } }
    }

    suspend fun accountTransactions(accountId: String): List<AccountTransaction> {
        val uid = userId()
        return Supabase.client.postgrest.from("account_transactions").select()
            .decodeList<AccountTransaction>()
            .filter { it.userId == uid && it.accountId == accountId }
            .sortedByDescending { it.createdAt }
    }

    suspend fun favouriteProductIds(): MutableSet<String> {
        val uid = userId()
        return Supabase.client.postgrest.from("favourites").select()
            .decodeList<Favourite>()
            .filter { it.userId == uid }
            .mapTo(mutableSetOf()) { it.productId }
    }

    suspend fun toggleFavourite(productId: String): Boolean {
        val uid = userId()
        val existing = Supabase.client.postgrest.from("favourites").select()
            .decodeList<Favourite>()
            .any { it.userId == uid && it.productId == productId }

        if (existing) {
            Supabase.client.postgrest.from("favourites").delete {
                filter {
                    eq("user_id", uid)
                    eq("product_id", productId)
                }
            }
            return false
        }

        val row = buildJsonObject {
            put("user_id", JsonPrimitive(uid))
            put("product_id", JsonPrimitive(productId))
        }
        Supabase.client.postgrest.from("favourites").insert(row)
        return true
    }

    suspend fun createDemoPaidOrder(
        product: Product,
        deliveryName: String,
        deliveryPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        paymentReference: String,
        courier: String,
        deliveryMethod: String,
        shippingCost: Double,
        estimatedDelivery: String,
        buyerBankAccountId: String
    ) {
        val uid = userId()
        require(uid != product.sellerId) { "You cannot buy your own item" }
        require(!product.isSold) { "This item has already been sold" }

        val row = buildJsonObject {
            put("buyer_id", JsonPrimitive(uid))
            put("buyer_bank_account_id", JsonPrimitive(buyerBankAccountId))
            put("seller_id", JsonPrimitive(product.sellerId))
            put("product_id", JsonPrimitive(product.id))
            put("amount", JsonPrimitive(product.price))
            put("status", JsonPrimitive("paid"))
            put("delivery_name", JsonPrimitive(deliveryName))
            put("delivery_phone", JsonPrimitive(deliveryPhone))
            put("delivery_address", JsonPrimitive(deliveryAddress))
            put("payment_method", JsonPrimitive(paymentMethod))
            put("payment_reference", JsonPrimitive(paymentReference))
            put("courier", JsonPrimitive(courier))
            put("delivery_method", JsonPrimitive(deliveryMethod))
            put("shipping_cost", JsonPrimitive(shippingCost))
            put("estimated_delivery", JsonPrimitive(estimatedDelivery))
        }
        Supabase.client.postgrest.from("orders").insert(row)
    }

    suspend fun buyerOrders(): List<OrderDisplay> {
        val uid = userId()
        val orders = Supabase.client.postgrest.from("orders").select()
            .decodeList<Order>()
            .filter { it.buyerId == uid }
        return attachProducts(orders)
    }

    suspend fun sellerOrders(): List<OrderDisplay> {
        val uid = userId()
        val orders = Supabase.client.postgrest.from("orders").select()
            .decodeList<Order>()
            .filter { it.sellerId == uid }
        return attachProducts(orders)
    }

    private suspend fun attachProducts(orders: List<Order>): List<OrderDisplay> {
        val products = Supabase.client.postgrest.from("products").select().decodeList<Product>()
        return orders.sortedByDescending { it.createdAt }.map { order ->
            OrderDisplay(order, products.firstOrNull { it.id == order.productId })
        }
    }

    suspend fun advanceOrder(order: Order): Order {
        val next = OrderStatus.next(order.status)
            ?: throw IllegalStateException("This order is already delivered")
        val tracking = if (next == "shipped") {
            val prefix = when (order.courier) {
                "Pudo" -> "PUDO"
                "The Courier Guy" -> "TCG"
                "Buffalo Courier" -> "BUF"
                else -> "VC"
            }
            "$prefix-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
        } else order.trackingNumber
        Supabase.client.postgrest.from("orders").update(
            buildJsonObject {
                put("status", JsonPrimitive(next))
                if (tracking != null) put("tracking_number", JsonPrimitive(tracking))
            }
        ) {
            filter { eq("id", order.id) }
        }
        return order.copy(status = next, trackingNumber = tracking)
    }
}
