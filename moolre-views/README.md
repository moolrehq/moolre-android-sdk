# Moolre XML SDK

`android-sdk-views` is the XML/View entry point for Moolre hosted checkout. It
exposes a branded `MoolrePayButton`; networking, WebView checkout, and
verification remain shared with Compose through the core artifacts.

## Installation

```kotlin
dependencies {
    implementation("com.moolre:android-sdk-views:1.0.0")
}
```

The artifact brings `android-sdk-core` and `android-sdk-checkout` transitively
and does not require Jetpack Compose.

## Layout

```xml
<com.moolre.sdk.MoolrePayButton
    android:id="@+id/moolrePayButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:environment="sandbox"
    app:buttonText="Pay with Moolre"
    app:buttonBorderColor="#FDB93C"
    app:buttonBorderWidth="1dp"
    app:buttonCornerRadius="10dp" />
```

Declare `xmlns:app="http://schemas.android.com/apk/res-auto"` on the parent
layout. Payment attributes are `amount`, `environment` (`sandbox` or `live`),
`apiUser`, `publicKey`, `accountNumber`, `currency`, `email`, `reference`,
`webhookUrl`, and `redirectUrl`. Programmatic properties are recommended for
cart totals and order references that change at runtime.

## Fragment/activity setup

```kotlin
private val checkoutLauncher =
    registerForActivityResult(MoolreCheckoutContract()) { result ->
        binding.moolrePayButton.handleCheckoutResult(result)
    }

private fun setupMoolreButton() {
    binding.moolrePayButton.apply {
        setCheckoutLauncher(checkoutLauncher)
        environment = MoolreEnvironment.SANDBOX
        apiUser = "your-api-user"
        publicKey = "your-public-key"
        accountNumber = "your-account-number"
        amount = BigDecimal("25.00")
        currency = "GHS"
        email = "customer@example.com"
        reference = "order-1001"
        webhookUrl = "https://merchant.example.com/moolre/webhook"
        redirectUrl = "moolre://payment-callback"

        setOnPaymentSuccessListener { reference ->
            // Refresh and fulfil the matching order on your backend.
        }
        setOnPaymentErrorListener { code, message ->
            // Map code to app copy and log diagnostics safely.
        }
    }
}
```

Use `SANDBOX` for testing and `LIVE` only with live credentials. The SDK
selects the corresponding payment-link and status endpoints automatically.
Keep the button's `android:id` stable so pending parameters can be restored
after configuration changes.

## Appearance

Available attributes are `buttonText`, `buttonTextColor`,
`buttonBackgroundColor`, `buttonBorderColor`, `buttonBorderWidth`,
`buttonCornerRadius`, `buttonIconTint`, and `showAmountOnButton`.

## Result policy

The success listener runs only after the shared coordinator receives
`status == 1`, a successful transaction status, and an exact transaction
reference match. Amount and currency are intentionally not compared by the V1
client; validate those values against your own pending order on the server
before fulfilment.
