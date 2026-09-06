package com.moolre.sdk

/**
 * Structured SDK failure that can be mapped to a stable public error code.
 */
class MoolrePaymentException(
    val code: String,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
