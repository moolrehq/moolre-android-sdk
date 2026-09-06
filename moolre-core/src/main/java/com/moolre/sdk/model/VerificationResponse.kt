package com.moolre.sdk.model

import java.math.BigDecimal

/**
 * Data class representing payment verification response.
 * @property status API envelope status (1 = request succeeded)
 * @property transactionStatus Transaction status (1 = payment succeeded)
 * @property reference Transaction external reference
 * @property amount Verified amount
 * @property accountNumber Account credited by the transaction
 */
data class VerificationResponse(
    val status: Int,
    val transactionStatus: Int = status,
    val reference: String,
    val amount: BigDecimal,
    val accountNumber: String? = null,
    val transactionId: String? = null,
    val timestamp: String? = null,
    val currency: String? = null
) {
    init {
        require(reference.isNotBlank()) { "Reference cannot be blank" }
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
    }

    val isSuccessful: Boolean
        get() = status == 1 && transactionStatus == 1
}
