package com.moolre.sdk.model

import java.math.BigDecimal

/**
 * Data class representing payment parameters.
 * @property amount The payment amount in major currency units
 * @property environment Moolre API environment
 * @property apiUser Merchant API user
 * @property publicKey Merchant public key
 * @property accountNumber Merchant account number
 * @property currency Currency code (default: "GHS")
 * @property email Customer email
 * @property reference Unique external reference
 * @property callback Server webhook URL (optional)
 * @property redirect In-app redirect URI
 */
data class PaymentParams(
    val amount: BigDecimal,
    val environment: MoolreEnvironment,
    val apiUser: String,
    val publicKey: String,
    val accountNumber: String,
    val currency: String = "GHS",
    val email: String,
    val reference: String,
    val callback: String? = null,
    val redirect: String,
    val reusable: Boolean = false,
    val expirationTimeMinutes: Int? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be greater than 0" }
        require(apiUser.isNotBlank()) { "API user cannot be blank" }
        require(publicKey.isNotBlank()) { "Public key cannot be blank" }
        require(accountNumber.isNotBlank()) { "Account number cannot be blank" }
        require(currency.isNotBlank()) { "Currency cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(reference.isNotBlank()) { "Reference cannot be blank" }
        require(callback?.isNotBlank() != false) { "Webhook URL cannot be blank" }
        require(redirect.isNotBlank()) { "Redirect URL cannot be blank" }
        require(expirationTimeMinutes == null || expirationTimeMinutes >= 1) {
            "Expiration time must be at least 1 minute"
        }
    }
}
