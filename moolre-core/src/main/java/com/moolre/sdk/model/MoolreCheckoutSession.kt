package com.moolre.sdk.model

/**
 * A prepared checkout that can be handed to a platform-specific UI layer.
 */
data class MoolreCheckoutSession(
    val authorizationUrl: String,
    val reference: String,
    val redirectUrl: String
) {
    init {
        require(authorizationUrl.isNotBlank()) { "Authorization URL cannot be blank" }
        require(reference.isNotBlank()) { "Reference cannot be blank" }
        require(redirectUrl.isNotBlank()) { "Redirect URL cannot be blank" }
    }
}
