package com.moolre.sdk

import android.app.Activity
import com.moolre.sdk.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoolreCheckoutContractTest {
    @Test
    fun parseResult_returnsCompletedReference() {
        val result = MoolreCheckoutResultParser.parse(Activity.RESULT_OK, "ref-123", null, null)

        assertEquals(MoolreCheckoutResult.Completed("ref-123"), result)
    }

    @Test
    fun parseResult_rejectsSuccessfulResultWithoutReference() {
        val result = MoolreCheckoutResultParser.parse(Activity.RESULT_OK, null, null, null)

        assertEquals(Constants.ERROR_MISSING_REFERENCE, (result as MoolreCheckoutResult.Failed).code)
    }

    @Test
    fun parseResult_mapsUserCancellation() {
        val result = MoolreCheckoutResultParser.parse(
            Activity.RESULT_CANCELED,
            null,
            Constants.ERROR_USER_CANCELLED,
            "Payment was cancelled by the user."
        )

        assertTrue(result is MoolreCheckoutResult.Cancelled)
    }

    @Test
    fun parseResult_preservesCheckoutFailure() {
        val result = MoolreCheckoutResultParser.parse(
            Activity.RESULT_CANCELED,
            null,
            Constants.ERROR_WEBVIEW,
            "Payment page failed to load."
        )

        assertEquals("Payment page failed to load.", (result as MoolreCheckoutResult.Failed).message)
    }

    @Test
    fun http404_isClassifiedAsMissingCheckoutPage() {
        val failure = MoolreCheckoutFailurePolicy.fromHttpStatus(404)

        assertEquals(Constants.ERROR_CHECKOUT_PAGE_NOT_FOUND, failure?.code)
        assertEquals("Payment checkout could not be loaded (HTTP 404).", failure?.message)
    }

    @Test
    fun statusBelow400_doesNotCreateFailure() {
        assertNull(MoolreCheckoutFailurePolicy.fromHttpStatus(304))
    }

    @Test
    fun otherHttpErrors_remainWebViewFailures() {
        assertEquals(Constants.ERROR_WEBVIEW, MoolreCheckoutFailurePolicy.fromHttpStatus(500)?.code)
    }
}
