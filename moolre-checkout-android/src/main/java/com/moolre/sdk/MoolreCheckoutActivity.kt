package com.moolre.sdk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.moolre.sdk.checkout.databinding.ActivityMoolreCheckoutBinding
import com.moolre.sdk.checkout.R
import com.moolre.sdk.utils.Constants

/**
 * Launches checkout in a Chrome Custom Tab and catches the payment redirect via its
 * singleTask intent-filter (see the SDK's AndroidManifest.xml). A Custom Tab, rather than
 * an embedded WebView, is used because WebView unconditionally sends an X-Requested-With
 * header identifying the host app, which checkout/payment backends commonly use to block
 * embedded WebView traffic.
 *
 * Presented as a partial-height ("bottom sheet") Custom Tab, themed to the Moolre brand color,
 * so it reads as part of the checkout flow rather than a jump out to the browser.
 */
class MoolreCheckoutActivity : AppCompatActivity() {

    companion object {
        fun newIntent(
            context: Context,
            checkoutUrl: String,
            redirectUrl: String,
            expectedReferences: List<String> = emptyList()
        ): Intent {
            return Intent(context, MoolreCheckoutActivity::class.java).apply {
                putExtra(MoolreCheckoutExtras.CHECKOUT_URL, checkoutUrl)
                putExtra(MoolreCheckoutExtras.REDIRECT_URL, redirectUrl)
                putStringArrayListExtra(
                    MoolreCheckoutExtras.EXPECTED_REFERENCES,
                    ArrayList(expectedReferences.filter(String::isNotBlank).distinct())
                )
            }
        }

        private const val LOG_TAG = "MoolreCheckout"
        private const val PARTIAL_HEIGHT_FRACTION = 0.9
        private const val TOOLBAR_CORNER_RADIUS_DP = 16
    }

    private lateinit var binding: ActivityMoolreCheckoutBinding
    private lateinit var checkoutUri: Uri
    private lateinit var redirectUri: Uri
    private var expectedReferences: Set<String> = emptySet()
    private var resultDelivered = false
    private var customTabLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoolreCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val checkoutUrl = intent.getStringExtra(MoolreCheckoutExtras.CHECKOUT_URL)
        val parsedCheckoutUri = checkoutUrl?.let(Uri::parse)
        if (parsedCheckoutUri == null ||
            !parsedCheckoutUri.scheme.equals("https", ignoreCase = true) ||
            parsedCheckoutUri.host.isNullOrBlank()
        ) {
            finishWithFailure(
                Constants.ERROR_INVALID_CHECKOUT_URL,
                "Checkout URL must use HTTPS and include a host."
            )
            return
        }
        checkoutUri = parsedCheckoutUri

        redirectUri = intent.getStringExtra(MoolreCheckoutExtras.REDIRECT_URL)
            ?.let(Uri::parse)
            ?: Uri.parse(Constants.DEFAULT_REDIRECT_URL)
        if (redirectUri.scheme.isNullOrBlank() || redirectUri.host.isNullOrBlank()) {
            finishWithFailure(
                Constants.ERROR_INVALID_REDIRECT_URL,
                "Redirect URL must include a scheme and host."
            )
            return
        }
        expectedReferences = intent.getStringArrayListExtra(MoolreCheckoutExtras.EXPECTED_REFERENCES)
            ?.filter(String::isNotBlank)
            ?.toSet()
            .orEmpty()

        resultDelivered = savedInstanceState?.getBoolean(MoolreCheckoutExtras.RESULT_DELIVERED) ?: false
        customTabLaunched = savedInstanceState?.getBoolean(MoolreCheckoutExtras.CUSTOM_TAB_LAUNCHED) ?: false

        configureBackHandling()
    }

    private fun configureBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAsCancelled()
                }
            }
        )
    }

    /**
     * The Custom Tab is launched on the first onResume rather than onCreate: onCreate is
     * followed immediately by an onResume/onPause pair as the tab takes focus, and a *second*
     * onResume only happens once the user returns to this activity - either because the
     * redirect was already handled in onNewIntent (resultDelivered is true by then) or because
     * they backed out of the tab without completing checkout.
     */
    override fun onResume() {
        super.onResume()
        if (resultDelivered) return
        if (customTabLaunched) {
            finishAsCancelled()
        } else {
            customTabLaunched = true
            launchCheckout()
        }
    }

    private fun launchCheckout() {
        debugLog("authorization_url=$checkoutUri")
        try {
            buildCustomTabsIntent().launchUrl(this, checkoutUri)
        } catch (e: ActivityNotFoundException) {
            finishWithFailure(Constants.ERROR_LAUNCH_FAILED, "No browser available to open checkout.")
        }
    }

    private fun buildCustomTabsIntent(): CustomTabsIntent {
        val toolbarColor = ContextCompat.getColor(this, R.color.moolre_checkout_toolbar)
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .setNavigationBarColor(toolbarColor)
            .build()
        val closeButtonIcon = ContextCompat.getDrawable(this, R.drawable.ic_checkout_close)?.toBitmap()
        val partialHeightPx = (resources.displayMetrics.heightPixels * PARTIAL_HEIGHT_FRACTION).toInt()

        val builder = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .setToolbarCornerRadiusDp(TOOLBAR_CORNER_RADIUS_DP)
            .setInitialActivityHeightPx(partialHeightPx, CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)
        closeButtonIcon?.let(builder::setCloseButtonIcon)
        return builder.build()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data ?: return
        if (matchesRedirect(uri)) {
            completeFromCallback(uri)
        } else {
            finishWithFailure(
                Constants.ERROR_UNSUPPORTED_REDIRECT,
                "Checkout redirected to an unsupported URL."
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(MoolreCheckoutExtras.RESULT_DELIVERED, resultDelivered)
        outState.putBoolean(MoolreCheckoutExtras.CUSTOM_TAB_LAUNCHED, customTabLaunched)
    }

    private fun matchesRedirect(uri: Uri): Boolean {
        return uri.scheme.equals(redirectUri.scheme, ignoreCase = true) &&
            uri.host.equals(redirectUri.host, ignoreCase = true) &&
            uri.port == redirectUri.port &&
            (uri.path ?: "") == (redirectUri.path ?: "")
    }

    private fun completeFromCallback(uri: Uri) {
        val reference = uri.getQueryParameter("reference")
        if (reference.isNullOrBlank()) {
            finishWithFailure(
                Constants.ERROR_MISSING_REFERENCE,
                "Payment reference not found in checkout redirect."
            )
            return
        }
        if (expectedReferences.isNotEmpty() && reference !in expectedReferences) {
            finishWithFailure(
                Constants.ERROR_REFERENCE_MISMATCH,
                "Checkout redirect reference did not match the prepared payment."
            )
            return
        }

        finishWithResult(
            resultCode = RESULT_OK,
            resultIntent = Intent().putExtra(MoolreCheckoutExtras.REFERENCE, reference)
        )
    }

    private fun finishAsCancelled() {
        finishWithFailure(Constants.ERROR_USER_CANCELLED, "Payment was cancelled by the user.")
    }

    private fun debugLog(message: String) {
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Log.d(LOG_TAG, message)
        }
    }

    private fun finishWithFailure(code: String, message: String) {
        finishWithResult(
            resultCode = RESULT_CANCELED,
            resultIntent = Intent()
                .putExtra(MoolreCheckoutExtras.ERROR_CODE, code)
                .putExtra(MoolreCheckoutExtras.ERROR_MESSAGE, message)
        )
    }

    private fun finishWithResult(resultCode: Int, resultIntent: Intent) {
        if (resultDelivered) return
        resultDelivered = true
        setResult(resultCode, resultIntent)
        finish()
    }
}
