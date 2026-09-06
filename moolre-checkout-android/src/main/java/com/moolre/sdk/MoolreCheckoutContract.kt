package com.moolre.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.moolre.sdk.model.MoolreCheckoutSession
import com.moolre.sdk.utils.Constants

internal object MoolreCheckoutExtras {
    const val CHECKOUT_URL = "extra_checkout_url"
    const val REDIRECT_URL = "extra_redirect_url"
    const val EXPECTED_REFERENCE = "extra_expected_reference"
    const val WEB_VIEW_STATE = "extra_web_view_state"
    const val RESULT_DELIVERED = "extra_result_delivered"
    const val REFERENCE = "reference"
    const val ERROR_CODE = "error_code"
    const val ERROR_MESSAGE = "error_message"
}

/**
 * Type-safe contract for launching checkout and receiving its raw result.
 */
class MoolreCheckoutContract :
    ActivityResultContract<MoolreCheckoutSession, MoolreCheckoutResult>() {

    override fun createIntent(context: Context, input: MoolreCheckoutSession): Intent {
        return MoolreCheckoutActivity.newIntent(
            context = context,
            checkoutUrl = input.authorizationUrl,
            redirectUrl = input.redirectUrl,
            expectedReference = input.reference
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MoolreCheckoutResult {
        return MoolreCheckoutResultParser.parse(
            resultCode = resultCode,
            reference = intent?.getStringExtra(MoolreCheckoutExtras.REFERENCE),
            errorCode = intent?.getStringExtra(MoolreCheckoutExtras.ERROR_CODE),
            errorMessage = intent?.getStringExtra(MoolreCheckoutExtras.ERROR_MESSAGE)
        )
    }
}

internal object MoolreCheckoutResultParser {
    fun parse(
        resultCode: Int,
        reference: String?,
        errorCode: String?,
        errorMessage: String?
    ): MoolreCheckoutResult {
        if (resultCode == Activity.RESULT_OK) {
            return if (reference.isNullOrBlank()) {
                MoolreCheckoutResult.Failed(
                    code = Constants.ERROR_MISSING_REFERENCE,
                    message = "Payment reference not found in checkout result."
                )
            } else {
                MoolreCheckoutResult.Completed(reference)
            }
        }

        val code = errorCode ?: Constants.ERROR_USER_CANCELLED
        val message = errorMessage ?: "Payment was cancelled by the user."
        return if (code == Constants.ERROR_USER_CANCELLED) {
            MoolreCheckoutResult.Cancelled
        } else {
            MoolreCheckoutResult.Failed(code, message)
        }
    }
}
