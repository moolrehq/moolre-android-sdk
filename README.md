# Moolre Android SDK

Moolre Android SDK V1 provides hosted Moolre checkout for Android applications
using either XML/Views or Jetpack Compose. Both entry points share the same
payment-link, Custom Tabs checkout, and transaction-verification implementation.

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

Open the project in Android Studio, configure local sandbox credentials, and run
the selected app on an Android 7.0+ device or emulator.

### Configure credentials for the included samples

The two included sample apps use one project-level file:
`<sdk-root>/local.properties`. Gradle reads it when the project is configured
and exposes the values to the samples through generated `BuildConfig` fields.
The Compose sample is `app`; the XML sample is `example-app`.

1. Copy `local.properties.example` to `local.properties` in the SDK root.
2. Replace all four placeholder values with credentials from the same Moolre
   environment:

```properties
moolre.environment=SANDBOX
moolre.apiUser=your-moolre-username
moolre.publicKey=your-sandbox-public-key
moolre.accountNumber=your-sandbox-account-number
```

3. Sync Gradle and rebuild the sample you want to run.

The property names are:

| Property | Meaning |
| --- | --- |
| `moolre.environment` | `SANDBOX` or `LIVE`; controls payment-link and status endpoints |
| `moolre.apiUser` | Moolre API user for the selected environment |
| `moolre.publicKey` | Public API key for the selected environment |
| `moolre.accountNumber` | Moolre account/wallet number for the selected environment |

To test production, change the environment and every credential together:

```properties
moolre.environment=LIVE
moolre.apiUser=your-moolre-username
moolre.publicKey=your-live-public-key
moolre.accountNumber=your-live-account-number
```

Never mix a sandbox API user, public key, or account number with `LIVE`, or
live credentials with `SANDBOX`.

`local.properties` is ignored by Git. Keep real credentials there only for
local sample testing; never commit it or paste real credentials into Kotlin,
XML, README files, screenshots, or issue reports.

### Configure a published SDK integration

`local.properties` is a sample-app convenience and is not read by the
published library. In your own app, pass the same values through `MoolreConfig`
(Compose) or the XML/View button properties:

```kotlin
val config = MoolreConfig(
    environment = MoolreEnvironment.SANDBOX,
    apiUser = "your-moolre-username",
    publicKey = "your-sandbox-public-key",
    accountNumber = "your-sandbox-account-number"
)
```

For XML/View, set `environment`, `apiUser`, `publicKey`, and `accountNumber`
on `MoolrePayButton` before starting checkout. Use server-side secret
injection or a backend-owned payment flow for production credentials whenever
possible; an Android APK can be inspected by its user.

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

Set the same environment in `local.properties`:

```properties
moolre.environment=SANDBOX
```

Use `LIVE` only after replacing all credentials with live credentials. Never
mix sandbox and live credentials, references, or API responses.

When switching to `LIVE`, replace the API user, public key, account number,
and payment references with live values. A sandbox credential cannot be used
against the live endpoints. After changing `local.properties`, sync Gradle or
rebuild so the generated `BuildConfig` values are refreshed.

## Payment flow

1. Create a `MoolrePaymentRequest` with a positive amount and customer email.
   Omit `reference` for a fresh SDK-generated external reference, or provide a
   unique merchant order reference.
2. The SDK posts a payment-link request and receives an authorization URL.
3. The checkout Activity opens that HTTPS URL in a Chrome Custom Tab.
4. Moolre redirects to the configured in-app `redirectUrl` with a reference.
   An optional `webhookUrl` receives the server-side callback.
5. The SDK verifies the original merchant external reference with the matching
   environment's status API.

The V1 client reports success only when the status response envelope is
`status == 1`, the transaction status is successful, and the verified
`externalref` exactly matches the original payment request. The checkout
redirect may contain either the merchant external reference or Moolre's
generated transaction reference; both are validated against the prepared
session. The client does not compare amount or currency. Your server should
still validate the expected amount, currency, account ownership, and order
state before fulfilment.

If the status endpoint reports a successful API request with a pending
transaction status, the coordinator performs up to three status checks with a
short delay before returning verification failure. Network and authentication
errors are returned immediately so the host can decide how to reconcile them.

## Configuration

```kotlin
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment

val config = MoolreConfig(
    environment = MoolreEnvironment.SANDBOX,
    apiUser = "your-moolre-username",
    publicKey = "your-public-key",
    accountNumber = "your-account-number",
    webhookUrl = "https://merchant.example.com/moolre/webhook",
    redirectUrl = "moolre://payment-callback"
)
```

`redirectUrl` defaults to `moolre://payment-callback` and must contain a URI
scheme and host. The checkout Activity matches its scheme, host, port, and path,
then reads the `reference` query parameter. The SDK's checkout Activity
catches this redirect itself via its own intent-filter, whose scheme/host
come from the `moolreRedirectScheme` / `moolreRedirectHost` manifest
placeholders - **every app must set both** in its own
`build.gradle`(`.kts`) to match whatever `redirectUrl` it uses (the example
above needs `"moolre"` / `"payment-callback"`); there is no built-in default
and the build fails without them. See
[`moolre-checkout-android/README.md`](moolre-checkout-android/README.md) for
details. `webhookUrl` is optional and should be an HTTPS endpoint owned by
your server.

Use a unique reference for every order and construct monetary values from
strings rather than `Double` values:

```kotlin
val amount = BigDecimal("25.00")
```

### `MoolrePaymentRequest`

- `amount`: positive `BigDecimal` in major currency units.
- `currency`: currency code, default `GHS`.
- `email`: required customer email.
- `reference`: optional merchant reference. When omitted or blank, the SDK
  generates one `moolre-<UUID>` reference for the payment attempt and reuses it
  while retrying that attempt. An explicitly supplied reference is preserved
  and must be unique.
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
        apiUser = "your-moolre-username"
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
            apiUser = "your-moolre-username",
            publicKey = "your-public-key",
            accountNumber = "your-account-number",
            webhookUrl = "https://merchant.example.com/moolre/webhook",
            redirectUrl = "moolre://payment-callback"
        )
    }
    val payment = MoolrePaymentRequest(
        amount = BigDecimal("25.00"),
        currency = "GHS",
        email = "customer@example.com"
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

`MoolrePayButton` disables itself while initiation or verification runs,
preserves the external reference across transient initiation failures, and
`rememberMoolrePaymentLauncher` saves pending parameters across Activity
recreation. Use `MoolreTheme` for Moolre colors, or override the button's
`containerColor`, `contentColor`, `borderColor`, `borderWidth`, or `shape`.

## Results and security

Both integrations expose `MoolrePaymentResult.Success`, `Cancelled`, and
`Failure(code, message)`. Common codes include `INVALID_CONFIG`,
`INVALID_AMOUNT`, `INITIATION_FAILED`, `VERIFICATION_FAILED`, `WEBVIEW_ERROR`,
`USER_CANCELLED`, `MISSING_REFERENCE`, `REFERENCE_MISMATCH`, and
`UNSUPPORTED_REDIRECT`, `CHECKOUT_PAGE_NOT_FOUND`.

Treat error messages as diagnostics rather than stable UI copy. Never treat a
browser redirect as proof of payment. Keep the order pending until your backend
verifies the reference and checks amount, currency, account number, and order
state. Make fulfilment idempotent and do not log private credentials or full
payment payloads in release builds.

## GitHub and secret-safety checklist

Before pushing this repository:

- Commit `local.properties.example`, but never commit `local.properties`.
- Keep only placeholders in documentation and sample source.
- Confirm `local.properties` is ignored by `.gitignore`.
- Search the staged diff for API users, public keys, account numbers, full
  payment URLs, and debug logs containing credentials.
- If a credential was ever committed, rotate it in Moolre; deleting the file
  is not enough because Git history retains it.

The included debug samples can use `local.properties`. A released Android
application should prefer a backend-owned payment initiation and verification
flow for production secrets.

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
| `android-sdk-checkout` | HTTPS Custom Tabs checkout Activity and Activity Result contract |
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
