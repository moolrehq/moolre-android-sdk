package com.moolre.sdk

import com.moolre.sdk.utils.Constants

internal data class MoolreCheckoutFailure(
    val code: String,
    val message: String
)

internal object MoolreCheckoutFailurePolicy {
    fun fromHttpStatus(statusCode: Int): MoolreCheckoutFailure? {
        if (statusCode < 400) return null
        return MoolreCheckoutFailure(
            code = if (statusCode == 404) {
                Constants.ERROR_CHECKOUT_PAGE_NOT_FOUND
            } else {
                Constants.ERROR_WEBVIEW
            },
            message = "Payment checkout could not be loaded (HTTP $statusCode)."
        )
    }

    fun fromNetworkError(): MoolreCheckoutFailure {
        return MoolreCheckoutFailure(
            code = Constants.ERROR_WEBVIEW,
            message = "Payment checkout could not be loaded."
        )
    }
}
