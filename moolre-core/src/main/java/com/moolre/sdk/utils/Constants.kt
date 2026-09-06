package com.moolre.sdk.utils

/**
 * SDK constants
 */
object Constants {
    const val LIVE_PAYMENT_URL = "https://api.moolre.com/embed/link"
    const val LIVE_STATUS_URL = "https://api.moolre.com/open/transact/status"
    const val SANDBOX_PAYMENT_URL = "https://sandbox.moolre.com/embed/link"
    const val SANDBOX_STATUS_URL = "https://sandbox.moolre.com/open/transact/status"
    const val DEFAULT_CALLBACK_SCHEME = "moolre"
    const val DEFAULT_CALLBACK_HOST = "payment-callback"
    const val DEFAULT_REDIRECT_URL = "$DEFAULT_CALLBACK_SCHEME://$DEFAULT_CALLBACK_HOST"

    // Error codes
    const val ERROR_INVALID_CONFIG = "INVALID_CONFIG"
    const val ERROR_INVALID_AMOUNT = "INVALID_AMOUNT"
    const val ERROR_INITIATION_FAILED = "INITIATION_FAILED"
    const val ERROR_VERIFICATION_FAILED = "VERIFICATION_FAILED"
    const val ERROR_WEBVIEW = "WEBVIEW_ERROR"
    const val ERROR_USER_CANCELLED = "USER_CANCELLED"
    const val ERROR_INVALID_CHECKOUT_URL = "INVALID_CHECKOUT_URL"
    const val ERROR_INVALID_REDIRECT_URL = "INVALID_REDIRECT_URL"
    const val ERROR_MISSING_REFERENCE = "MISSING_REFERENCE"
    const val ERROR_REFERENCE_MISMATCH = "REFERENCE_MISMATCH"
    const val ERROR_UNSUPPORTED_REDIRECT = "UNSUPPORTED_REDIRECT"
    const val ERROR_LAUNCH_FAILED = "LAUNCH_FAILED"
    const val ERROR_MISSING_EMAIL = "MISSING_EMAIL"
    const val ERROR_MISSING_API_USER = "MISSING_API_USER"
    const val ERROR_DUPLICATE_REFERENCE = "DUPLICATE_REFERENCE"

    // Payment-link request values
    const val PAYMENT_LINK_TYPE = 1
    const val STATUS_ID_TYPE_EXTERNAL_REFERENCE = 1
}
