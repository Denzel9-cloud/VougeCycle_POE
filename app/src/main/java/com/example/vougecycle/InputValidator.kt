package com.example.vougecycle

object InputValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun passwordError(password: String): String? = when {
        password.length < 6 -> "Password must be at least 6 characters"
        password.none { it.isDigit() } -> "Password must contain a number"
        password.none { it.isUpperCase() } -> "Password must contain a capital letter"
        else -> null
    }

    fun validPrice(value: String): Double? = value.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
}
