package com.moolre.sdk

/**
 * Raw result returned by the checkout activity before server verification.
 */
sealed interface MoolreCheckoutResult {
    /**
     * Checkout reached the configured redirect with a transaction reference.
     */
    data class Completed(val reference: String) : MoolreCheckoutResult

    /**
     * Checkout ended without a completed payment.
     */
    data object Cancelled : MoolreCheckoutResult

    /**
     * Checkout encountered a recoverable or configuration error.
     */
    data class Failed(val code: String, val message: String) : MoolreCheckoutResult
}
