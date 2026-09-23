package com.moolre.sdk

import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.model.PaymentResponse
import com.moolre.sdk.model.VerificationResponse
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoolrePaymentCoordinatorTest {
    @Test
    fun initiatePayment_mapsGatewayResponseToCheckoutSession() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(
                paymentResponse = PaymentResponse("https://checkout.example/pay", "ref-123")
            )
        )

        val result = coordinator.initiatePayment(params())

        assertTrue(result.isSuccess)
        assertEquals("https://checkout.example/pay", result.getOrThrow().authorizationUrl)
        assertEquals("ref-123", result.getOrThrow().reference)
        assertEquals("ref-123", result.getOrThrow().externalReference)
    }

    @Test
    fun initiatePayment_preservesStructuredGatewayFailure() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(throwOnInitiate = MoolrePaymentException("DECLINED", "Payment declined"))
        )

        val result = coordinator.initiatePayment(params())

        assertFalse(result.isSuccess)
        assertEquals("DECLINED", (result.exceptionOrNull() as MoolrePaymentException).code)
    }

    @Test
    fun verifyPayment_returnsSuccessForVerifiedPayment() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(
                verificationResponse = VerificationResponse(
                    status = 1,
                    reference = "ref-123",
                    amount = BigDecimal("10.00"),
                    currency = "GHS"
                )
            )
        )

        val result = coordinator.verifyPayment(params(), "ref-123")

        assertTrue(result is MoolrePaymentResult.Success)
        assertEquals("ref-123", (result as MoolrePaymentResult.Success).reference)
    }

    @Test
    fun verifyPayment_usesMerchantReferenceWhenRedirectUsesGatewayReference() = runBlocking {
        val gateway = FakeGateway(
            verificationResponse = VerificationResponse(
                status = 1,
                reference = "merchant-order-42",
                amount = BigDecimal("10.00"),
                currency = "GHS"
            )
        )
        val coordinator = MoolrePaymentCoordinator(gateway)

        val result = coordinator.verifyPayment(params().copy(reference = "merchant-order-42"), "gateway-ref-42")

        assertTrue(result is MoolrePaymentResult.Success)
        assertEquals("merchant-order-42", gateway.lastVerifiedReference)
    }

    @Test
    fun verifyPayment_ignoresAmountAndCurrencyWhenStatusAndReferenceMatch() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(
                verificationResponse = VerificationResponse(
                    status = 1,
                    reference = "ref-123",
                    amount = BigDecimal("999.99"),
                    currency = "USD"
                )
            )
        )

        val result = coordinator.verifyPayment(params(), "ref-123")

        assertTrue(result is MoolrePaymentResult.Success)
    }

    @Test
    fun verifyPayment_returnsFailureForUnverifiedPayment() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(
                verificationResponse = VerificationResponse(
                    status = 0,
                    reference = "ref-123",
                    amount = BigDecimal("10.00"),
                    currency = "GHS"
                )
            )
        )

        val result = coordinator.verifyPayment(params(), "ref-123")

        assertTrue(result is MoolrePaymentResult.Failure)
        assertEquals(Constants.ERROR_VERIFICATION_FAILED, (result as MoolrePaymentResult.Failure).code)
    }

    @Test
    fun verifyPayment_retriesPendingTransactionBeforeFailing() = runBlocking {
        val gateway = FakeGateway(
            verificationResponses = listOf(
                VerificationResponse(status = 1, transactionStatus = 0, reference = "ref-123", amount = BigDecimal("10.00")),
                VerificationResponse(status = 1, transactionStatus = 1, reference = "ref-123", amount = BigDecimal("10.00"))
            )
        )
        val coordinator = MoolrePaymentCoordinator(
            gateway = gateway,
            verificationAttempts = 2,
            verificationDelayMillis = 0
        )

        val result = coordinator.verifyPayment(params(), "redirect-ref")

        assertTrue(result is MoolrePaymentResult.Success)
        assertEquals(2, gateway.verificationCallCount)
    }

    @Test
    fun verifyPayment_returnsFailureForMismatchedVerifiedReference() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(
            FakeGateway(
                verificationResponse = VerificationResponse(
                    status = 1,
                    reference = "different-reference",
                    amount = BigDecimal("10.00"),
                    currency = "GHS"
                )
            )
        )

        val result = coordinator.verifyPayment(params(), "ref-123")

        assertTrue(result is MoolrePaymentResult.Failure)
        assertEquals(Constants.ERROR_REFERENCE_MISMATCH, (result as MoolrePaymentResult.Failure).code)
    }

    @Test
    fun verifyPayment_rejectsBlankReference() = runBlocking {
        val coordinator = MoolrePaymentCoordinator(FakeGateway())

        val result = coordinator.verifyPayment(params(), " ")

        assertTrue(result is MoolrePaymentResult.Failure)
        assertEquals(Constants.ERROR_MISSING_REFERENCE, (result as MoolrePaymentResult.Failure).code)
    }

    private fun params() = PaymentParams(
        amount = BigDecimal("10.00"),
        environment = MoolreEnvironment.SANDBOX,
        apiUser = "api-user",
        publicKey = "public-key",
        accountNumber = "1234567890",
        email = "customer@example.com",
        reference = "ref-123",
        callback = "https://example.com/webhook",
        redirect = "moolre-example://payment-callback"
    )

    private class FakeGateway(
        private val paymentResponse: PaymentResponse = PaymentResponse(
            "https://checkout.example/pay",
            "ref-default"
        ),
        private val verificationResponse: VerificationResponse = VerificationResponse(
            status = 1,
            reference = "ref-default",
            amount = BigDecimal("10.00"),
            currency = "GHS"
        ),
        private val throwOnInitiate: Exception? = null,
        private val throwOnVerify: Exception? = null,
        private val verificationResponses: List<VerificationResponse> = emptyList()
    ) : MoolrePaymentGateway {
        var lastVerifiedReference: String? = null
        var verificationCallCount: Int = 0

        override suspend fun initiatePayment(params: PaymentParams): PaymentResponse {
            throwOnInitiate?.let { throw it }
            return paymentResponse
        }

        override suspend fun verifyPayment(
            reference: String,
            environment: MoolreEnvironment,
            apiUser: String,
            publicKey: String,
            accountNumber: String
        ): VerificationResponse {
            throwOnVerify?.let { throw it }
            lastVerifiedReference = reference
            verificationCallCount += 1
            return verificationResponses.getOrNull(verificationCallCount - 1) ?: verificationResponse
        }
    }
}
