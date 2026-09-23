package com.moolre.sdk.model

import java.math.BigDecimal

/**
 * Customer-facing payment values supplied for one payment attempt.
 */
data class MoolrePaymentRequest(
    val amount: BigDecimal,
    val currency: String = "GHS",
    val email: String,
    val reference: String? = null,
    val webhookUrl: String? = null,
    val redirectUrl: String? = null,
    val reusable: Boolean = false,
    val expirationTimeMinutes: Int? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Combines merchant configuration and one payment request for the network layer.
 */
fun MoolrePaymentRequest.toPaymentParams(config: MoolreConfig): PaymentParams {
    return PaymentParams(
        amount = amount,
        environment = config.environment,
        apiUser = config.apiUser,
        publicKey = config.publicKey,
        accountNumber = config.accountNumber,
        currency = currency,
        email = email,
        reference = reference?.takeIf { it.isNotBlank() }
            ?: MoolreReferenceGenerator.generate(),
        callback = webhookUrl ?: config.webhookUrl,
        redirect = redirectUrl ?: config.redirectUrl,
        reusable = reusable,
        expirationTimeMinutes = expirationTimeMinutes,
        metadata = metadata
    )
}
