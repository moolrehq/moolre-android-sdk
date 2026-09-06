package com.moolre.sdk

import com.moolre.sdk.model.VerificationResponse

/**
 * Final outcome of a payment attempt.
 */
sealed interface MoolrePaymentResult {
    /**
     * The payment was verified successfully.
     */
    data class Success(
        val reference: String,
        val verification: VerificationResponse
    ) : MoolrePaymentResult

    /**
     * The customer left checkout before a verified payment was available.
     */
    data object Cancelled : MoolrePaymentResult

    /**
     * The payment could not be prepared or verified.
     */
    data class Failure(
        val code: String,
        val message: String
    ) : MoolrePaymentResult
}
