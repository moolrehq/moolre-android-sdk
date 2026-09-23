package com.moolre.sdk.model

/**
 * A prepared checkout that can be handed to a platform-specific UI layer.
 *
 * @property reference Moolre-generated transaction reference
 * @property externalReference Merchant reference sent as `externalref`
 */
data class MoolreCheckoutSession(
    val authorizationUrl: String,
    val reference: String,
    val redirectUrl: String,
    val externalReference: String? = null
) {
    init {
        require(authorizationUrl.isNotBlank()) { "Authorization URL cannot be blank" }
        require(reference.isNotBlank()) { "Reference cannot be blank" }
        require(redirectUrl.isNotBlank()) { "Redirect URL cannot be blank" }
        require(externalReference?.isNotBlank() != false) {
            "External reference cannot be blank"
        }
    }
}
