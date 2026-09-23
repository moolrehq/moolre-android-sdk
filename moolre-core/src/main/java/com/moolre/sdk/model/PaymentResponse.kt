package com.moolre.sdk.model

/**
 * Data class representing payment response.
 * @property authorizationUrl URL to redirect user for payment
 * @property reference Moolre-generated transaction reference returned by the link API
 */
data class PaymentResponse(
    val authorizationUrl: String,
    val reference: String
) {
    init {
        require(authorizationUrl.isNotBlank()) { "Authorization URL cannot be blank" }
        require(reference.isNotBlank()) { "Reference cannot be blank" }
    }
}
