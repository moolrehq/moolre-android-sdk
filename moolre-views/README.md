# Moolre XML SDK

`android-sdk-views` is the XML/View entry point for Moolre hosted checkout. It
exposes a branded `MoolrePayButton`; networking, Custom Tabs checkout, and
verification remain shared with Compose through the core artifacts.

## Installation

```kotlin
dependencies {
    implementation("com.moolre:android-sdk-views:1.0.0")
}
```

The artifact brings `android-sdk-core` and `android-sdk-checkout` transitively
and does not require Jetpack Compose.

## Credentials and environment

For the included XML sample, put credentials in the SDK-root
`local.properties`; `example-app/build.gradle.kts` maps them to generated
`BuildConfig` values. Copy `local.properties.example`, then set all four
properties from the same environment:

```properties
moolre.environment=SANDBOX
moolre.apiUser=your-sandbox-api-user
moolre.publicKey=your-sandbox-public-key
moolre.accountNumber=your-sandbox-account-number
```

For a published app, set the equivalent button properties before checkout:

```kotlin
moolrePayButton.environment = MoolreEnvironment.SANDBOX
moolrePayButton.apiUser = "your-sandbox-api-user"
moolrePayButton.publicKey = "your-sandbox-public-key"
moolrePayButton.accountNumber = "your-sandbox-account-number"
```

Use `LIVE` only with live credentials, and never commit `local.properties`.

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
When `reference` is unset or blank, the button generates one
`moolre-<UUID>` external reference for the payment attempt and reuses it when
retrying a transient initiation failure. If you set it yourself, keep it mapped
to the matching order and reuse it for retries of that order.
Keep the button's `android:id` stable so pending parameters can be restored
after configuration changes.

## Appearance

Available attributes are `buttonText`, `buttonTextColor`,
`buttonBackgroundColor`, `buttonBorderColor`, `buttonBorderWidth`,
`buttonCornerRadius`, `buttonIconTint`, and `showAmountOnButton`.

## Result policy

The success listener runs only after the shared coordinator receives
`status == 1`, a successful transaction status, and an exact external-reference
match. The checkout redirect accepts either the merchant external reference or
Moolre's generated transaction reference. Amount and currency are intentionally
not compared by the V1 client; validate those values against your own pending
order on the server before fulfilment.
