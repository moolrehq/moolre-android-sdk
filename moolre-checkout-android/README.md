# Moolre Android Checkout Runtime

`android-sdk-checkout` owns the shared HTTPS WebView checkout Activity,
redirect validation, state restoration, and Activity Result contract used by
the XML and Compose integrations.

Most applications should depend on `android-sdk-views` or
`android-sdk-compose`, which include this runtime transitively.

## Installation

```kotlin
dependencies {
    implementation("com.moolre:android-sdk-checkout:1.0.0")
}
```

## Activity Result contract

The contract accepts a prepared `MoolreCheckoutSession` and returns a
`MoolreCheckoutResult`:

```kotlin
private val checkoutLauncher =
    registerForActivityResult(MoolreCheckoutContract()) { result ->
        when (result) {
            is MoolreCheckoutResult.Completed -> {
                val reference = result.reference
                // Pass the reference to the core verification coordinator.
            }
            MoolreCheckoutResult.Cancelled -> Unit
            is MoolreCheckoutResult.Failed -> {
                val code = result.code
                val message = result.message
            }
        }
    }
```

Most applications should use `MoolrePayButton`, which coordinates initiation,
launch, and verification for you.

## Redirect and lifecycle behavior

- Only HTTPS checkout URLs with a host are loaded.
- `redirectUrl` must include a scheme and host.
- Scheme, host, port, and path must match the configured redirect.
- The redirect query must include a non-blank `reference`.
- If an expected reference was prepared, it must match exactly.
- Back navigation first goes back in WebView history; otherwise it returns a
  cancellation result.
- WebView state is restored after configuration changes.

The module manifest contributes the checkout Activity and
`android.permission.INTERNET`.
