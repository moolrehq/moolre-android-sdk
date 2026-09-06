package com.moolre.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.moolre.sdk.checkout.databinding.ActivityMoolreCheckoutBinding
import com.moolre.sdk.utils.Constants

class MoolreCheckoutActivity : AppCompatActivity() {

    companion object {
        fun newIntent(
            context: Context,
            checkoutUrl: String,
            redirectUrl: String,
            expectedReference: String? = null
        ): Intent {
            return Intent(context, MoolreCheckoutActivity::class.java).apply {
                putExtra(MoolreCheckoutExtras.CHECKOUT_URL, checkoutUrl)
                putExtra(MoolreCheckoutExtras.REDIRECT_URL, redirectUrl)
                putExtra(MoolreCheckoutExtras.EXPECTED_REFERENCE, expectedReference)
            }
        }
    }

    private lateinit var binding: ActivityMoolreCheckoutBinding
    private lateinit var redirectUri: Uri
    private var expectedReference: String? = null
    private var resultDelivered = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoolreCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val checkoutUrl = intent.getStringExtra(MoolreCheckoutExtras.CHECKOUT_URL)
        val checkoutUri = checkoutUrl?.let(Uri::parse)
        if (checkoutUri == null || checkoutUri.scheme != "https" || checkoutUri.host.isNullOrBlank()) {
            finishWithFailure(
                Constants.ERROR_INVALID_CHECKOUT_URL,
                "Checkout URL must use HTTPS and include a host."
            )
            return
        }

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
        expectedReference = intent.getStringExtra(MoolreCheckoutExtras.EXPECTED_REFERENCE)
        resultDelivered = savedInstanceState?.getBoolean(MoolreCheckoutExtras.RESULT_DELIVERED) ?: false

        configureBackHandling()
        binding.toolbar.setNavigationOnClickListener { finishAsCancelled() }
        configureWebView(
            checkoutUrl = checkoutUri.toString(),
            savedWebViewState = savedInstanceState?.getBundle(MoolreCheckoutExtras.WEB_VIEW_STATE)
        )
    }

    private fun configureBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        finishAsCancelled()
                    }
                }
            }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(checkoutUrl: String, savedWebViewState: Bundle?) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    url?.let { handleNavigation(Uri.parse(it)) }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame != false) {
                        finishWithFailure(
                            Constants.ERROR_WEBVIEW,
                            error?.description?.toString() ?: "Payment page failed to load."
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true && errorResponse != null && errorResponse.statusCode >= 400) {
                        finishWithFailure(
                            Constants.ERROR_WEBVIEW,
                            "Payment page returned HTTP ${errorResponse.statusCode}."
                        )
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    return handleNavigation(uri)
                }
            }
            if (savedWebViewState == null || restoreState(savedWebViewState) == null) {
                loadUrl(checkoutUrl)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webViewState = Bundle()
        binding.webView.saveState(webViewState)
        outState.putBundle(MoolreCheckoutExtras.WEB_VIEW_STATE, webViewState)
        outState.putBoolean(MoolreCheckoutExtras.RESULT_DELIVERED, resultDelivered)
    }

    private fun handleNavigation(uri: Uri): Boolean {
        if (matchesRedirect(uri)) {
            completeFromCallback(uri)
            return true
        }

        return when (uri.scheme?.lowercase()) {
            "https" -> false
            else -> {
                finishWithFailure(
                    Constants.ERROR_UNSUPPORTED_REDIRECT,
                    "Checkout redirected to an unsupported URL."
                )
                true
            }
        }
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
        if (!expectedReference.isNullOrBlank() && reference != expectedReference) {
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
        binding.webView.stopLoading()
        setResult(resultCode, resultIntent)
        finish()
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }
}
