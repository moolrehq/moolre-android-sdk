package com.moolre.sdk.model

import com.moolre.sdk.utils.Constants

/**
 * Merchant configuration shared by the XML and Compose entry points.
 */
data class MoolreConfig(
    val environment: MoolreEnvironment = MoolreEnvironment.SANDBOX,
    val apiUser: String,
    val publicKey: String,
    val accountNumber: String,
    val webhookUrl: String? = null,
    val redirectUrl: String = Constants.DEFAULT_REDIRECT_URL
) {
    init {
        require(apiUser.isNotBlank()) { "API user cannot be blank" }
        require(publicKey.isNotBlank()) { "Public key cannot be blank" }
        require(accountNumber.isNotBlank()) { "Account number cannot be blank" }
        require(webhookUrl?.isNotBlank() != false) { "Webhook URL cannot be blank" }
        require(redirectUrl.isNotBlank()) { "Redirect URL cannot be blank" }
    }
}
