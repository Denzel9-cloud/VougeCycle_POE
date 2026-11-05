package com.example.vougecycle

object OrderStatus {
    val validStatuses = setOf("pending", "paid", "preparing", "shipped", "out_for_delivery", "delivered")

    fun next(current: String): String? = when (current) {
        "pending" -> "paid"
        "paid" -> "preparing"
        "preparing" -> "shipped"
        "shipped" -> "out_for_delivery"
        "out_for_delivery" -> "delivered"
        else -> null
    }
}
