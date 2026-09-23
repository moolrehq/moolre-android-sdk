# Moolre Android Checkout Runtime

`android-sdk-checkout` owns the shared HTTPS checkout Activity, redirect
validation, state restoration, and Activity Result contract used by the XML
and Compose integrations.

Checkout is launched in a Chrome Custom Tab rather than an embedded
`WebView`. Android `WebView` unconditionally attaches an
`X-Requested-With: <package-name>` header to every request, which checkout
and other fraud-sensitive backends commonly use to detect and block embedded
WebView traffic (there is currently no supported API to suppress that header
- Google reverted the opt-in removal it shipped in earlier `androidx.webkit`
previews). Custom Tabs run in the user's actual browser and are not subject
to this, so a checkout URL that 404s in a raw WebView but works when opened
directly in Chrome will also work through this runtime.

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
- If expected merchant or Moolre-generated references were prepared, the
  redirect reference must match one of them exactly - this is what prevents a
  malicious app from hijacking the flow by firing a spoofed redirect at the
  checkout Activity's intent-filter, since `reference` values are random
  UUIDs (`MoolreReferenceGenerator`) and not guessable.
- The checkout Activity is `singleTask` and catches the redirect via its own
  intent-filter (`onNewIntent`), not through in-app WebView navigation. If the
  user backs out of the Custom Tab without completing checkout, that's
  reported as `USER_CANCELLED`.

### Required host-app manifest configuration

The checkout Activity's intent-filter scheme/host come from the
`moolreRedirectScheme` / `moolreRedirectHost` manifest placeholders. **Every**
consuming app must set both in its own `build.gradle`(`.kts`) - there is no
built-in default, and the build fails at manifest-merge time
(`Attribute data@host ... requires a placeholder substitution but no value ...
is provided`) if either is missing. If you use the SDK's default
`redirectUrl` (`moolre://payment-callback`, i.e. `Constants.DEFAULT_REDIRECT_URL`),
set the placeholders to match that; if you pass a custom `redirectUrl` to
`MoolreCheckoutSession`, set the placeholders to match it instead:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["moolreRedirectScheme"] = "your-app-scheme" // "moolre" for the default redirectUrl
        manifestPlaceholders["moolreRedirectHost"] = "payment-callback"
    }
}
```

Do not also declare your own intent-filter for that scheme/host elsewhere in
your app - that creates a disambiguation dialog when the redirect fires.

## Diagnosing checkout page errors

Because checkout now runs in a Custom Tab rather than an embedded WebView,
this SDK can no longer observe the checkout page's HTTP status or network
errors directly - `CHECKOUT_PAGE_NOT_FOUND` and `WEBVIEW_ERROR` are unused by
this Activity today (kept for `MoolreCheckoutFailurePolicy`'s existing test
coverage). A checkout page that fails to load shows the browser's own error
page to the user; if they then back out, the SDK reports `USER_CANCELLED`,
not the specific underlying failure.

Debuggable builds log the exact `authorization_url` that was opened:

```bash
adb logcat -s MoolreCheckout
```

This value may contain sensitive session information. Never commit or share
it, and do not enable this logging in a release build. If a generated sandbox
payment fails to open at all, provide Moolre support the environment,
transaction reference, host, and UTC timestamp.

The module manifest contributes the checkout Activity,
`android.permission.INTERNET`, and the redirect intent-filter described
above.
