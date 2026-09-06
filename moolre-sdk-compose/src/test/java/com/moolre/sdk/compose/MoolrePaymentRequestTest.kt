package com.moolre.sdk.compose

import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.MoolrePaymentRequest
import com.moolre.sdk.model.toPaymentParams
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoolrePaymentRequestTest {
    private val config = MoolreConfig(
        environment = MoolreEnvironment.SANDBOX,
        apiUser = "api-user",
        publicKey = "public-key",
        accountNumber = "account-number",
        webhookUrl = "https://example.com/webhook",
        redirectUrl = "moolre://payment-callback"
    )

    @Test
    fun `maps merchant config and payment values`() {
        val params = MoolrePaymentRequest(
            amount = BigDecimal("42.50"),
            currency = "GHS",
            email = "customer@example.com",
            reference = "order-42"
        ).toPaymentParams(config)

        assertEquals(BigDecimal("42.50"), params.amount)
        assertEquals("public-key", params.publicKey)
        assertEquals("account-number", params.accountNumber)
        assertEquals("GHS", params.currency)
        assertEquals("customer@example.com", params.email)
        assertEquals("order-42", params.reference)
        assertEquals("https://example.com/webhook", params.callback)
        assertEquals("moolre://payment-callback", params.redirect)
    }

    @Test
    fun `request webhook and redirect override merchant defaults`() {
        val params = MoolrePaymentRequest(
            amount = BigDecimal("5.00"),
            email = "customer@example.com",
            reference = "order-5",
            webhookUrl = "https://example.com/custom-webhook",
            redirectUrl = "moolre://custom-callback"
        ).toPaymentParams(config)

        assertEquals("https://example.com/custom-webhook", params.callback)
        assertEquals("moolre://custom-callback", params.redirect)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non-positive amounts before initiation`() {
        MoolrePaymentRequest(
            amount = BigDecimal.ZERO,
            email = "customer@example.com",
            reference = "order-zero"
        ).toPaymentParams(config)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects blank merchant credentials`() {
        MoolreConfig(
            apiUser = "api-user",
            publicKey = "",
            accountNumber = "account-number"
        )
    }
}
