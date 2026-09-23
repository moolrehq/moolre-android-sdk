package com.moolre.sdk

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.BaseSavedState
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.moolre.sdk.views.databinding.ViewMoolreButtonBinding
import com.moolre.sdk.views.R
import com.moolre.sdk.model.MoolreCheckoutSession
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.MoolrePaymentRequest
import com.moolre.sdk.model.MoolreReferenceGenerator
import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.model.toPaymentParams
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MoolrePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: ViewMoolreButtonBinding =
        ViewMoolreButtonBinding.inflate(LayoutInflater.from(context), this, true)

    // Public properties for payment, settable via XML or programmatically
    var amount: BigDecimal = BigDecimal.ZERO
        set(value) {
            field = value
            updateButtonLabel()
        }
    var environment: MoolreEnvironment = MoolreEnvironment.SANDBOX
    var apiUser: String = ""
    var publicKey: String = ""
    var accountNumber: String = ""
    var currency: String = "GHS" // Default currency
        set(value) {
            field = value
            updateButtonLabel()
        }
    var email: String? = null
    var reference: String? = null
    var webhookUrl: String? = null
    var redirectUrl: String? = null
    var reusable: Boolean = false
    var expirationTimeMinutes: Int? = null
    var showAmountOnButton: Boolean = false
        set(value) {
            field = value
            updateButtonLabel()
        }

    private var buttonLabel: CharSequence = binding.payButton.text

    private val paymentCoordinator = MoolrePaymentCoordinator(MoolrePaymentService.create())

    private var paymentSuccessListener: ((reference: String) -> Unit)? = null
    private var paymentErrorListener: ((errorCode: String, errorMessage: String) -> Unit)? = null
    private var checkoutLauncher: ActivityResultLauncher<MoolreCheckoutSession>? = null
    private var pendingPaymentParams: PaymentParams? = null
    private var generatedReference: String? = null

    init {
        setupAttributes(context, attrs)
        setupInternalClickListener()
    }

    private fun setupAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MoolrePayButton,
            0, 0
        ).apply {
            try {
                amount = getString(R.styleable.MoolrePayButton_amount)
                    ?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO
                environment = if (
                    getInt(R.styleable.MoolrePayButton_environment, 0) == 1
                ) {
                    MoolreEnvironment.LIVE
                } else {
                    MoolreEnvironment.SANDBOX
                }
                apiUser = getString(R.styleable.MoolrePayButton_apiUser) ?: ""
                publicKey = getString(R.styleable.MoolrePayButton_publicKey) ?: ""
                accountNumber = getString(R.styleable.MoolrePayButton_accountNumber) ?: ""
                currency = getString(R.styleable.MoolrePayButton_currency) ?: "GHS"
                email = getString(R.styleable.MoolrePayButton_email)
                reference = getString(R.styleable.MoolrePayButton_reference)
                webhookUrl = getString(R.styleable.MoolrePayButton_webhookUrl)
                redirectUrl = getString(R.styleable.MoolrePayButton_redirectUrl)
                showAmountOnButton = getBoolean(R.styleable.MoolrePayButton_showAmountOnButton, false)

                // Appearance Attributes (THESE WILL BE RED IF NOT IN attrs.xml AND PROJECT REBUILT)
                val borderWidth = getDimensionPixelSize(R.styleable.MoolrePayButton_buttonBorderWidth, 0)
                val customCornerRadius = if (hasValue(R.styleable.MoolrePayButton_buttonCornerRadius)) {
                    getDimensionPixelSize(R.styleable.MoolrePayButton_buttonCornerRadius, resources.getDimensionPixelSize(R.dimen.moolre_button_default_corner_radius_fallback)).toFloat()
                } else {
                    try {
                        resources.getDimensionPixelSize(R.dimen.moolre_button_corner_radius).toFloat()
                    } catch (e: Exception) {
                        resources.getDimensionPixelSize(R.dimen.moolre_button_default_corner_radius_fallback).toFloat() // Ensure this fallback dimen exists
                    }
                }
                val buttonBorderColor = getColor(R.styleable.MoolrePayButton_buttonBorderColor, Color.TRANSPARENT)

                val buttonTextAttr = getString(R.styleable.MoolrePayButton_buttonText)
                buttonLabel = buttonTextAttr
                    ?: getString(R.styleable.MoolrePayButton_android_text)
                    ?: buttonLabel

                val defaultTextColor = ContextCompat.getColor(context, R.color.moolre_button_default_text_color) // Ensure this color exists
                val textColor = getColor(R.styleable.MoolrePayButton_buttonTextColor, defaultTextColor)
                binding.payButton.setTextColor(textColor)

                if (hasValue(R.styleable.MoolrePayButton_buttonIconTint)) {
                    val iconTint = getColor(R.styleable.MoolrePayButton_buttonIconTint, defaultTextColor)
                    binding.payButton.compoundDrawablesRelative.firstOrNull()?.let { icon ->
                        DrawableCompat.setTint(icon.mutate(), iconTint)
                    }
                }

                if (hasValue(R.styleable.MoolrePayButton_buttonBackgroundColor)) {
                    val buttonBgColor = getColor(R.styleable.MoolrePayButton_buttonBackgroundColor, ContextCompat.getColor(context, R.color.moolre_button_default_background_color)) // Ensure this color exists

                    val normalDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(buttonBgColor)
                        cornerRadius = customCornerRadius
                        if (borderWidth > 0) setStroke(borderWidth, buttonBorderColor)
                    }

                    val hsv = FloatArray(3)
                    Color.colorToHSV(buttonBgColor, hsv)
                    hsv[2] *= 0.8f // Darken by 20% for pressed state
                    val pressedDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.HSVToColor(hsv))
                        cornerRadius = customCornerRadius
                        if (borderWidth > 0) setStroke(borderWidth, buttonBorderColor)
                    }

                    val disabledDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(ContextCompat.getColor(context, R.color.moolre_button_disabled_background_color)) // Ensure this color exists
                        cornerRadius = customCornerRadius
                        // Optionally, different border for disabled state
                    }

                    binding.payButton.background = StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
                        addState(intArrayOf(-android.R.attr.state_enabled), disabledDrawable)
                        addState(intArrayOf(), normalDrawable)
                    }
                }
                // Consider what happens if buttonBackgroundColor is not set - apply border/corners to default?
                updateButtonLabel()
            } finally {
                recycle()
            }
        }
    }

    private fun setupInternalClickListener() {
        binding.payButton.setOnClickListener {
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                if (checkoutLauncher == null) {
                    paymentErrorListener?.invoke(
                        Constants.ERROR_INVALID_CONFIG,
                        "Register MoolreCheckoutContract before starting payment."
                    )
                    return@launch
                }
                showLoading(true)
                prepareCheckoutSession().fold(
                    onSuccess = { session ->
                        try {
                            val launcher = checkoutLauncher
                            launcher!!.launch(session)
                        } catch (e: Exception) {
                            Log.e("MoolrePayButton", "Failed to launch MoolreCheckoutActivity", e)
                            paymentErrorListener?.invoke("LAUNCH_FAILED", "Could not start payment activity: ${e.message}")
                            pendingPaymentParams = null
                            showLoading(false)
                        }
                    },
                    onFailure = { exception ->
                        val moolreEx = exception as? MoolrePaymentException
                        paymentErrorListener?.invoke(
                            moolreEx?.code ?: "PREPARATION_FAILED",
                            moolreEx?.message ?: "Failed to prepare checkout."
                        )
                        showLoading(false)
                    }
                )
            } ?: run {
                Log.e("MoolrePayButton", "LifecycleOwner not found. Cannot initiate payment.")
                paymentErrorListener?.invoke("LIFECYCLE_ERROR", "Could not find LifecycleOwner to start payment.")
                showLoading(false)
            }
        }
    }

    /**
     * Binds a host-registered [MoolreCheckoutContract] launcher to this button.
     */
    fun setCheckoutLauncher(launcher: ActivityResultLauncher<MoolreCheckoutSession>) {
        checkoutLauncher = launcher
    }

    /**
     * Verifies and dispatches a result returned by [MoolreCheckoutContract].
     */
    fun handleCheckoutResult(result: MoolreCheckoutResult) {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        if (lifecycleOwner == null) {
            paymentErrorListener?.invoke(
                Constants.ERROR_INVALID_CONFIG,
                "Could not find LifecycleOwner to process checkout result."
            )
            showLoading(false)
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            when (result) {
                is MoolreCheckoutResult.Completed -> {
                    val params = pendingPaymentParams
                    if (params == null) {
                        paymentErrorListener?.invoke(
                            Constants.ERROR_INVALID_CONFIG,
                            "No pending payment was found for this checkout result."
                        )
                    } else {
                        when (val paymentResult = paymentCoordinator.verifyPayment(params, result.reference)) {
                            is MoolrePaymentResult.Success -> paymentSuccessListener?.invoke(paymentResult.reference)
                            MoolrePaymentResult.Cancelled -> paymentErrorListener?.invoke(
                                Constants.ERROR_USER_CANCELLED,
                                "Payment was cancelled by the user."
                            )
                            is MoolrePaymentResult.Failure -> paymentErrorListener?.invoke(
                                paymentResult.code,
                                paymentResult.message
                            )
                        }
                    }
                }
                MoolreCheckoutResult.Cancelled -> paymentErrorListener?.invoke(
                    Constants.ERROR_USER_CANCELLED,
                    "Payment was cancelled by the user."
                )
                is MoolreCheckoutResult.Failed -> paymentErrorListener?.invoke(result.code, result.message)
            }
            pendingPaymentParams = null
            generatedReference = null
            showLoading(false)
        }
    }

    fun setOnPaymentSuccessListener(listener: (reference: String) -> Unit) {
        this.paymentSuccessListener = listener
    }

    fun setOnPaymentErrorListener(listener: (errorCode: String, errorMessage: String) -> Unit) {
        this.paymentErrorListener = listener
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState(), pendingPaymentParams, generatedReference)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            pendingPaymentParams = state.toPaymentParams()
            generatedReference = state.generatedReference()
            showLoading(pendingPaymentParams != null)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        Log.w("MoolrePayButton", "External OnClickListener set. This will override the default payment initiation. For payment results, use setOnPaymentSuccessListener/ErrorListener.")
        binding.payButton.setOnClickListener(l) // Allow override but log it
    }

    override fun performClick(): Boolean {
        return binding.payButton.performClick()
    }

    fun setText(text: CharSequence?) {
        buttonLabel = text ?: ""
        updateButtonLabel()
    }

    fun getText(): CharSequence? {
        return binding.payButton.text
    }

    fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.payButton.isEnabled = !isLoading
        binding.payButton.alpha = if (isLoading) 0.7f else 1.0f // Visual cue for disabled
    }

    /**
     * Prepares a checkout session for a host Activity Result launcher.
     */
    public suspend fun prepareCheckoutSession(): Result<MoolreCheckoutSession> {
        val config = try {
            MoolreConfig(
                environment = environment,
                apiUser = apiUser,
                publicKey = publicKey,
                accountNumber = accountNumber,
                webhookUrl = webhookUrl,
                redirectUrl = redirectUrl ?: Constants.DEFAULT_REDIRECT_URL
            )
        } catch (error: IllegalArgumentException) {
            return Result.failure(
                MoolrePaymentException(Constants.ERROR_INVALID_CONFIG, error.message ?: "Payment configuration is invalid.")
            )
        }

        val effectiveReference = reference?.takeIf { it.isNotBlank() }
            ?: generatedReference
            ?: MoolreReferenceGenerator.generate().also { generatedReference = it }
        val request = MoolrePaymentRequest(
            amount = amount,
            currency = currency,
            email = email ?: "",
            reference = effectiveReference,
            webhookUrl = webhookUrl,
            redirectUrl = redirectUrl,
            reusable = reusable,
            expirationTimeMinutes = expirationTimeMinutes
        )
        val params = try {
            request.toPaymentParams(config)
        } catch (error: IllegalArgumentException) {
            return Result.failure(
                MoolrePaymentException(
                    if (amount <= BigDecimal.ZERO) Constants.ERROR_INVALID_AMOUNT else Constants.ERROR_INVALID_CONFIG,
                    error.message ?: "Payment request is invalid."
                )
            )
        }
        return paymentCoordinator.initiatePayment(params).also { result ->
            if (result.isSuccess) pendingPaymentParams = params
        }
    }

    private fun updateButtonLabel() {
        binding.payButton.text = if (showAmountOnButton && amount > BigDecimal.ZERO) {
            listOf(buttonLabel, "$currency ${amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}")
                .joinToString(" " + Char(0x2022) + " ")
        } else {
            buttonLabel
        }
    }

    private fun legacyUpdateButtonLabel() {
        binding.payButton.text = if (showAmountOnButton && amount > BigDecimal.ZERO) {
            "$buttonLabel • $currency ${amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"
        } else {
            buttonLabel
        }
        return

        binding.payButton.text = if (showAmountOnButton && amount > BigDecimal.ZERO) {
            "$buttonLabel • $currency ${amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"
        } else {
            buttonLabel
        }
    }

    private class SavedState : BaseSavedState {
        private val paymentState: Bundle

        constructor(
            superState: Parcelable?,
            params: PaymentParams?,
            generatedReference: String?
        ) : super(superState) {
            paymentState = Bundle().apply {
                putString(KEY_GENERATED_REFERENCE, generatedReference)
                params?.let {
                    putString(KEY_AMOUNT, it.amount.toPlainString())
                    putString(KEY_ENVIRONMENT, it.environment.name)
                    putString(KEY_API_USER, it.apiUser)
                    putString(KEY_PUBLIC_KEY, it.publicKey)
                    putString(KEY_ACCOUNT_NUMBER, it.accountNumber)
                    putString(KEY_CURRENCY, it.currency)
                    putString(KEY_EMAIL, it.email)
                    putString(KEY_REFERENCE, it.reference)
                    putString(KEY_CALLBACK, it.callback)
                    putString(KEY_REDIRECT, it.redirect)
                    putBoolean(KEY_REUSABLE, it.reusable)
                    putInt(KEY_EXPIRATION, it.expirationTimeMinutes ?: -1)
                }
            }
        }

        private constructor(source: Parcel) : super(source) {
            paymentState = source.readBundle(javaClass.classLoader) ?: Bundle()
        }

        fun toPaymentParams(): PaymentParams? {
            val amount = paymentState.getString(KEY_AMOUNT)?.toBigDecimalOrNull() ?: return null
            return runCatching {
                PaymentParams(
                    amount = amount,
                    environment = MoolreEnvironment.valueOf(
                        paymentState.getString(KEY_ENVIRONMENT).orEmpty()
                    ),
                    apiUser = paymentState.getString(KEY_API_USER).orEmpty(),
                    publicKey = paymentState.getString(KEY_PUBLIC_KEY).orEmpty(),
                    accountNumber = paymentState.getString(KEY_ACCOUNT_NUMBER).orEmpty(),
                    currency = paymentState.getString(KEY_CURRENCY).orEmpty(),
                    email = paymentState.getString(KEY_EMAIL).orEmpty(),
                    reference = paymentState.getString(KEY_REFERENCE).orEmpty(),
                    callback = paymentState.getString(KEY_CALLBACK),
                    redirect = paymentState.getString(KEY_REDIRECT).orEmpty(),
                    reusable = paymentState.getBoolean(KEY_REUSABLE),
                    expirationTimeMinutes = paymentState.getInt(KEY_EXPIRATION).takeIf { it >= 1 }
                )
            }.getOrNull()
        }

        fun generatedReference(): String? = paymentState.getString(KEY_GENERATED_REFERENCE)

        override fun writeToParcel(destination: Parcel, flags: Int) {
            super.writeToParcel(destination, flags)
            destination.writeBundle(paymentState)
        }

        companion object {
            private const val KEY_AMOUNT = "amount"
            private const val KEY_ENVIRONMENT = "environment"
            private const val KEY_API_USER = "api_user"
            private const val KEY_PUBLIC_KEY = "public_key"
            private const val KEY_ACCOUNT_NUMBER = "account_number"
            private const val KEY_CURRENCY = "currency"
            private const val KEY_EMAIL = "email"
            private const val KEY_REFERENCE = "reference"
            private const val KEY_CALLBACK = "callback"
            private const val KEY_REDIRECT = "redirect"
            private const val KEY_REUSABLE = "reusable"
            private const val KEY_EXPIRATION = "expiration"
            private const val KEY_GENERATED_REFERENCE = "generated_reference"

            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
