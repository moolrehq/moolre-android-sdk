package com.moolre.sdk

import com.moolre.sdk.model.MoolreCheckoutSession
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.CancellationException

/**
 * Coordinates payment initiation and verification without depending on a UI toolkit.
 */
class MoolrePaymentCoordinator(
    private val gateway: MoolrePaymentGateway
) {
    suspend fun initiatePayment(params: PaymentParams): Result<MoolreCheckoutSession> {
        return try {
            val response = gateway.initiatePayment(params)
            Result.success(
                MoolreCheckoutSession(
                    authorizationUrl = response.authorizationUrl,
                    reference = response.reference,
                    redirectUrl = params.redirect
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error.asMoolreException(Constants.ERROR_INITIATION_FAILED))
        }
    }

    /**
     * Verifies a payment using a successful gateway status and an exact transaction-reference match.
     * Amount and currency are intentionally not compared by this V1 client-side check.
     */
    suspend fun verifyPayment(
        params: PaymentParams,
        reference: String
    ): MoolrePaymentResult {
        return verifyPayment(
            reference = reference,
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
            val verification = gateway.verifyPayment(
                reference = reference,
                environment = environment,
                apiUser = apiUser,
                publicKey = publicKey,
                accountNumber = accountNumber
            )
            if (!verification.isSuccessful) {
                MoolrePaymentResult.Failure(
                    code = Constants.ERROR_VERIFICATION_FAILED,
                    message = "Payment verification failed."
                )
            } else if (verification.reference != reference) {
                MoolrePaymentResult.Failure(
                    code = Constants.ERROR_REFERENCE_MISMATCH,
                    message = "Verified payment reference did not match the requested reference."
                )
            } else {
                MoolrePaymentResult.Success(reference, verification)
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
