package com.moolre.sdk

import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.model.PaymentResponse
import com.moolre.sdk.model.VerificationResponse
import com.moolre.sdk.model.MoolreEnvironment

/**
 * Network boundary used by the payment coordinator and replaceable in tests.
 */
interface MoolrePaymentGateway {
    suspend fun initiatePayment(params: PaymentParams): PaymentResponse

    suspend fun verifyPayment(
        reference: String,
        environment: MoolreEnvironment,
        apiUser: String,
        publicKey: String,
        accountNumber: String
    ): VerificationResponse
}
