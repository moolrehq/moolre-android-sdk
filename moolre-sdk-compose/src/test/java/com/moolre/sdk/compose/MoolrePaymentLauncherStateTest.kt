package com.moolre.sdk.compose

import androidx.compose.runtime.saveable.SaverScope
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoolrePaymentLauncherStateTest {
    private val saverScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    @Test
    fun saver_restoresPendingPaymentAcrossRecreation() {
        val state = MoolrePaymentLauncherState(
            isProcessing = true,
            retryReference = "order-42",
            pendingPaymentParams = PaymentParams(
                amount = BigDecimal("12.50"),
                environment = MoolreEnvironment.SANDBOX,
                apiUser = "api-user",
                publicKey = "public-key",
                accountNumber = "account-number",
                currency = "GHS",
                email = "customer@example.com",
                reference = "order-42",
                callback = "https://example.com/webhook",
                redirect = "moolre://payment-callback"
            )
        )

        val saved = with(MoolrePaymentLauncherStateSaver) {
            with(saverScope) { save(state) }
        }
        val restored = MoolrePaymentLauncherStateSaver.restore(saved!!)

        assertNotNull(restored)
        assertTrue(restored!!.isProcessing)
        assertEquals(BigDecimal("12.50"), restored.pendingPaymentParams?.amount)
        assertEquals(MoolreEnvironment.SANDBOX, restored.pendingPaymentParams?.environment)
        assertEquals("order-42", restored.pendingPaymentParams?.reference)
        assertEquals("order-42", restored.retryReference)
    }

    @Test
    fun saver_doesNotRestoreProcessingWithoutPendingPayment() {
        val restored = MoolrePaymentLauncherStateSaver.restore(
            listOf(true, "", "", "", "", "", "", "")
        )

        assertNotNull(restored)
        assertTrue(!restored!!.isProcessing)
        assertEquals(null, restored.pendingPaymentParams)
    }
}
