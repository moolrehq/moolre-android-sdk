package com.moolre.sdk

import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoolrePaymentServiceTest {

    @Test
    fun initiatePaymentUsesSandboxContract() {
        val request = MoolrePaymentService.create(OkHttpClient())
            .buildInitiationRequest(paymentParams())

        assertEquals("https://sandbox.moolre.com/embed/link", request.url.toString())
        assertEquals("sandbox-user", request.header("X-API-USER"))
        assertEquals("public-key", request.header("X-API-PUBKEY"))

        val body = requestBody(request)
        assertTrue(body.contains("\"type\":1"))
        assertTrue(body.contains("\"amount\":\"12.50\""))
        assertTrue(body.contains("\"email\":\"customer@example.com\""))
        assertTrue(body.contains("\"externalref\":\"order-123\""))
        assertTrue(body.contains("\"reusable\":\"0\""))
        assertTrue(body.contains("\"currency\":\"GHS\""))
        assertTrue(body.contains("\"accountnumber\":\"1234567890\""))
        assertTrue(body.contains("\"redirect\":\"moolre://payment-callback\""))
        assertTrue(body.contains("\"callback\":\"https://example.com/webhook\""))
        assertTrue(body.contains("\"expiration_time\":15"))
        assertTrue(body.contains("\"metadata\":{\"order_id\":\"42\"}"))
    }

    @Test
    fun verifyPaymentUsesSandboxStatusContract() {
        val request = MoolrePaymentService.create(OkHttpClient())
            .buildVerificationRequest(
                reference = "order-123",
                environment = MoolreEnvironment.SANDBOX,
                apiUser = "sandbox-user",
                publicKey = "public-key",
                accountNumber = "1234567890"
            )

        assertEquals("https://sandbox.moolre.com/open/transact/status", request.url.toString())
        assertEquals("sandbox-user", request.header("X-API-USER"))
        assertEquals("public-key", request.header("X-API-PUBKEY"))
        val body = requestBody(request)
        assertTrue(body.contains("\"type\":1"))
        assertTrue(body.contains("\"idtype\":1"))
        assertTrue(body.contains("\"id\":\"order-123\""))
        assertTrue(body.contains("\"accountnumber\":\"1234567890\""))
    }

    @Test
    fun liveEnvironmentUsesLivePaymentEndpoint() {
        val request = MoolrePaymentService.create(OkHttpClient())
            .buildInitiationRequest(paymentParams().copy(environment = MoolreEnvironment.LIVE))

        assertEquals("https://api.moolre.com/embed/link", request.url.toString())
    }

    private fun paymentParams(): PaymentParams = PaymentParams(
        amount = BigDecimal("12.50"),
        environment = MoolreEnvironment.SANDBOX,
        apiUser = "sandbox-user",
        publicKey = "public-key",
        accountNumber = "1234567890",
        currency = "GHS",
        email = "customer@example.com",
        reference = "order-123",
        callback = "https://example.com/webhook",
        redirect = "moolre://payment-callback",
        expirationTimeMinutes = 15,
        metadata = mapOf("order_id" to "42")
    )

    private fun requestBody(request: Request): String {
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8()
    }
}
