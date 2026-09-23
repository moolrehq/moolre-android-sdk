package com.moolre.sdk

import com.moolre.sdk.model.MoolreCheckoutSession
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Coordinates payment initiation and verification without depending on a UI toolkit.
 */
class MoolrePaymentCoordinator(
    private val gateway: MoolrePaymentGateway,
    private val verificationAttempts: Int = 3,
    private val verificationDelayMillis: Long = 500L
) {
    init {
        require(verificationAttempts >= 1) { "Verification attempts must be at least 1." }
        require(verificationDelayMillis >= 0) { "Verification delay cannot be negative." }
    }
    suspend fun initiatePayment(params: PaymentParams): Result<MoolreCheckoutSession> {
        return try {
            val response = gateway.initiatePayment(params)
            Result.success(
                MoolreCheckoutSession(
                    authorizationUrl = response.authorizationUrl,
                    reference = response.reference,
                    redirectUrl = params.redirect,
                    externalReference = params.reference
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error.asMoolreException(Constants.ERROR_INITIATION_FAILED))
        }
    }

    /**
     * Verifies a payment using a successful gateway status and an exact external-reference match.
     * Amount and currency are intentionally not compared by this V1 client-side check.
     */
    suspend fun verifyPayment(
        params: PaymentParams,
        redirectReference: String
    ): MoolrePaymentResult {
        if (redirectReference.isBlank()) {
            return MoolrePaymentResult.Failure(
                code = Constants.ERROR_MISSING_REFERENCE,
                message = "Payment reference cannot be blank."
            )
        }

        return verifyPayment(
            reference = params.reference,
            environment = params.environment,
            apiUser = params.apiUser,
            publicKey = params.publicKey,
            accountNumber = params.accountNumber
        )
    }

    suspend fun verifyPayment(
        reference: String,
        environment: MoolreEnvironment,
        apiUser: String,
        publicKey: String,
        accountNumber: String
    ): MoolrePaymentResult {
        if (reference.isBlank()) {
            return MoolrePaymentResult.Failure(
                code = Constants.ERROR_VERIFICATION_FAILED,
                message = "Payment reference cannot be blank."
            )
        }

        return try {
            var verification = gateway.verifyPayment(
                reference = reference,
                environment = environment,
                apiUser = apiUser,
                publicKey = publicKey,
                accountNumber = accountNumber
            )
            repeat(verificationAttempts - 1) {
                if (verification.status == 1 && verification.transactionStatus != 1) {
                    delay(verificationDelayMillis)
                    verification = gateway.verifyPayment(
                        reference = reference,
                        environment = environment,
                        apiUser = apiUser,
                        publicKey = publicKey,
                        accountNumber = accountNumber
                    )
                }
            }

            when {
                !verification.isSuccessful -> MoolrePaymentResult.Failure(
                    code = Constants.ERROR_VERIFICATION_FAILED,
                    message = "Payment verification failed."
                )
                verification.reference != reference -> MoolrePaymentResult.Failure(
                    code = Constants.ERROR_REFERENCE_MISMATCH,
                    message = "Verified external reference did not match the payment request."
                )
                else -> MoolrePaymentResult.Success(reference, verification)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val mapped = error.asMoolreException(Constants.ERROR_VERIFICATION_FAILED)
            MoolrePaymentResult.Failure(mapped.code, mapped.message ?: "Payment verification failed.")
        }
    }

    private fun Exception.asMoolreException(fallbackCode: String): MoolrePaymentException {
        return this as? MoolrePaymentException
            ?: MoolrePaymentException(
                code = fallbackCode,
                message = message ?: "Payment operation failed.",
                cause = this
            )
    }
}
