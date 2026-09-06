package com.moolre.sdk.model

import com.moolre.sdk.utils.Constants

/**
 * Moolre API environment used for both payment initiation and verification.
 */
enum class MoolreEnvironment(
    internal val paymentEndpoint: String,
    internal val statusEndpoint: String
) {
    SANDBOX(Constants.SANDBOX_PAYMENT_URL, Constants.SANDBOX_STATUS_URL),
    LIVE(Constants.LIVE_PAYMENT_URL, Constants.LIVE_STATUS_URL)
}
