# Moolre Android SDK

Moolre Android SDK V1 provides hosted Moolre checkout for Android applications
using either XML/Views or Jetpack Compose. Both entry points share the same
payment-link, WebView checkout, and transaction-verification implementation.

## Requirements

- Android API 24 (Android 7.0) or newer
- Kotlin/JVM target 11 or newer
- A Moolre API user, public key, and account number
- `android.permission.INTERNET` (added by the checkout artifact)

The Compose artifact requires a Compose-enabled application. The XML artifact
does not require Compose.

## Installation

```kotlin
dependencies {
    // XML/View application
    implementation("com.moolre:android-sdk-views:1.0.0")

    // Or Jetpack Compose application
    implementation("com.moolre:android-sdk-compose:1.0.0")
}
```

Each UI artifact brings the shared core and checkout runtime transitively. The
coordinates above assume the artifacts have been published to your Maven
repository; the sample apps use project dependencies while developing.

## Example applications

Two complete checkout examples are included:

- `app` — Compose checkout with a branded `MoolrePayButton`.
- `example-app` — XML/View checkout with Activity Result handling and a result
  screen.

Open the project in Android Studio, update the sample merchant values, and run
the selected app on an Android 7.0+ device or emulator.

### Replace demo credentials

- Compose: edit the `MoolreConfig` block in
  `app/src/main/java/com/moolre/moolre_android_sdk/MainActivity.kt`.
- XML: edit `environment`, `apiUser`, `publicKey`, and `accountNumber` in
  `example-app/src/main/java/com/moolre/example/ui/checkout/CheckoutViewModel.kt`.

Keep credentials in local configuration or a secret-injection mechanism in
real applications; do not commit them to source control.

## Environment selection

The SDK defaults to `MoolreEnvironment.SANDBOX` so a new integration cannot
accidentally create live payments. Set `LIVE` only with live credentials:

| Environment | Payment-link endpoint | Status endpoint |
| --- | --- | --- |
| `SANDBOX` | `https://sandbox.moolre.com/embed/link` | `https://sandbox.moolre.com/open/transact/status` |
| `LIVE` | `https://api.moolre.com/embed/link` | `https://api.moolre.com/open/transact/status` |

Do not mix credentials, payment references, or test data between environments.
The request uses `X-API-USER` and `X-API-PUBKEY` headers in both environments.

### Switch the included samples

For the Compose sample, edit `app/src/main/java/com/moolre/moolre_android_sdk/MainActivity.kt`:

```kotlin
environment = MoolreEnvironment.SANDBOX // testing
// environment = MoolreEnvironment.LIVE // production
```

For the XML sample, edit
`example-app/src/main/java/com/moolre/example/ui/checkout/CheckoutViewModel.kt`:

```kotlin
val environment = MoolreEnvironment.SANDBOX // testing
// val environment = MoolreEnvironment.LIVE // production
```

When switching to `LIVE`, replace the API user, public key, account number,
and payment references with live values. A sandbox credential cannot be used
against the live endpoints.

## Payment flow

1. Create a `MoolrePaymentRequest` with a positive amount, customer email, and
   unique order reference.
2. The SDK posts a payment-link request and receives an authorization URL.
3. The checkout Activity loads that HTTPS URL in a WebView.
4. Moolre redirects to the configured in-app `redirectUrl` with a reference.
   An optional `webhookUrl` receives the server-side callback.
5. The SDK verifies the reference with the matching environment's status API.

The V1 client reports success only when the status response envelope is
`status == 1`, the transaction status is successful, and the verified
`externalref` exactly matches the reference returned by checkout. The client
does not compare amount or currency. Your server should still validate the
expected amount, currency, account ownership, and order state before fulfilment.

## Configuration

```kotlin
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment

val config = MoolreConfig(
    environment = MoolreEnvironment.SANDBOX,
    apiUser = "your-api-user",
    publicKey = "your-public-key",
    accountNumber = "your-account-number",
    webhookUrl = "https://merchant.example.com/moolre/webhook",
    redirectUrl = "moolre://payment-callback"
)
```

`redirectUrl` defaults to `moolre://payment-callback` and must contain a URI
scheme and host. The checkout Activity matches its scheme, host, port, and path,
then reads the `reference` query parameter. The SDK consumes the redirect
inside its WebView, so no host-app intent filter is required. `webhookUrl` is
optional and should be an HTTPS endpoint owned by your server.

Use a unique reference for every order and construct monetary values from
strings rather than `Double` values:

```kotlin
val amount = BigDecimal("25.00")
```

### `MoolrePaymentRequest`

- `amount`: positive `BigDecimal` in major currency units.
- `currency`: currency code, default `GHS`.
- `email`: required customer email.
- `reference`: required unique merchant reference.
- `webhookUrl` / `redirectUrl`: optional per-payment overrides.
- `reusable`: whether the payment link can be reused, default `false`.
- `expirationTimeMinutes`: optional expiry, minimum `1`.
- `metadata`: optional string key/value data.

## XML/View integration

Register the checkout contract before binding the button:

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

The same values can be supplied as XML attributes: `amount`, `environment`,
`apiUser`, `publicKey`, `accountNumber`, `currency`, `email`, `reference`,
`webhookUrl`, and `redirectUrl`. Appearance attributes include `buttonText`,
`buttonTextColor`, `buttonBackgroundColor`, `buttonBorderColor`,
`buttonBorderWidth`, `buttonCornerRadius`, `buttonIconTint`, and
`showAmountOnButton`. Keep the button ID stable for state restoration.

## Jetpack Compose integration

```kotlin
@Composable
fun CheckoutButton() {
    val config = remember {
        MoolreConfig(
            environment = MoolreEnvironment.SANDBOX,
            apiUser = "your-api-user",
            publicKey = "your-public-key",
            accountNumber = "your-account-number",
            webhookUrl = "https://merchant.example.com/moolre/webhook",
            redirectUrl = "moolre://payment-callback"
        )
    }
    val payment = MoolrePaymentRequest(
        amount = BigDecimal("25.00"),
        currency = "GHS",
        email = "customer@example.com",
        reference = "order-1001"
    )
    var result by remember { mutableStateOf<MoolrePaymentResult?>(null) }

    MoolreTheme {
        MoolrePayButton(
            config = config,
            payment = payment,
            modifier = Modifier.fillMaxWidth(),
            showAmount = true,
            onResult = { result = it }
        )
    }

    when (val paymentResult = result) {
        is MoolrePaymentResult.Success -> Text("Payment verified: ${paymentResult.reference}")
        MoolrePaymentResult.Cancelled -> Text("Payment cancelled.")
        is MoolrePaymentResult.Failure -> Text(
            "Payment failed (${paymentResult.code}): ${paymentResult.message}"
        )
        null -> Unit
    }
}
```

`MoolrePayButton` disables itself while initiation or verification runs, and
`rememberMoolrePaymentLauncher` saves pending parameters across Activity
recreation. Use `MoolreTheme` for Moolre colors, or override the button's
`containerColor`, `contentColor`, `borderColor`, `borderWidth`, or `shape`.

## Results and security

Both integrations expose `MoolrePaymentResult.Success`, `Cancelled`, and
`Failure(code, message)`. Common codes include `INVALID_CONFIG`,
`INVALID_AMOUNT`, `INITIATION_FAILED`, `VERIFICATION_FAILED`, `WEBVIEW_ERROR`,
`USER_CANCELLED`, `MISSING_REFERENCE`, `REFERENCE_MISMATCH`, and
`UNSUPPORTED_REDIRECT`.

Treat error messages as diagnostics rather than stable UI copy. Never treat a
browser redirect as proof of payment. Keep the order pending until your backend
verifies the reference and checks amount, currency, account number, and order
state. Make fulfilment idempotent and do not log private credentials or full
payment payloads in release builds.

## Testing and publishing

```powershell
.\gradlew.bat `
  :moolre-core:testDebugUnitTest `
  :moolre-checkout-android:testDebugUnitTest `
  :moolre-sdk-compose:testDebugUnitTest `
  :moolre-views:compileDebugKotlin `
  :app:assembleDebug `
  :example-app:assembleDebug
```

Before production, test success, cancellation, network failure, rotation or
process recreation, missing references, and mismatched references in sandbox.
Build and consume release AARs from a clean Android application before
publishing.

## Modules

| Artifact | Purpose |
| --- | --- |
| `android-sdk-core` | Models, gateway, payment-link service, and verification coordinator |
| `android-sdk-checkout` | HTTPS WebView checkout Activity and Activity Result contract |
| `android-sdk-views` | XML/custom View `MoolrePayButton` |
| `android-sdk-compose` | Jetpack Compose `MoolrePayButton` and `MoolreTheme` |

Most applications should depend on `android-sdk-views` or
`android-sdk-compose`, not the lower-level artifacts directly.

## Related documentation

- [XML/View module guide](moolre-views/README.md)
- [Compose module guide](moolre-sdk-compose/README.md)
- [Core module guide](moolre-core/README.md)
- [Checkout runtime guide](moolre-checkout-android/README.md)
- [Moolre PHP SDK](https://github.com/moolrehq/moolre-php)
