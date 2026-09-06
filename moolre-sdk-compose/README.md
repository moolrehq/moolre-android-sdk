# Moolre Compose SDK

`android-sdk-compose` is the Jetpack Compose entry point for Moolre hosted
checkout. It provides `MoolrePayButton`,
`rememberMoolrePaymentLauncher`, and the optional `MoolreTheme`; payment
initiation, checkout, and verification are shared with the XML integration.

## Installation

```kotlin
dependencies {
    implementation("com.moolre:android-sdk-compose:1.0.0")
}
```

The consuming application must already be Compose-enabled. The artifact brings
the core and Android checkout runtime transitively.

## Checkout button

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

Use a unique reference for every order and construct amounts with
`BigDecimal("25.00")`, not a `Double`. `MoolrePayButton` disables itself while
initiation or verification runs, and the launcher saves pending parameters
across host Activity recreation.

Set `MoolreEnvironment.SANDBOX` for testing and `LIVE` for production. The
environment controls both the payment-link and status endpoints; credentials
and references must belong to the selected environment.

## Results

```kotlin
when (val paymentResult = result) {
    is MoolrePaymentResult.Success -> {
        val reference = paymentResult.reference
        val verification = paymentResult.verification
    }
    MoolrePaymentResult.Cancelled -> Unit
    is MoolrePaymentResult.Failure -> {
        val code = paymentResult.code
        val message = paymentResult.message
    }
    null -> Unit
}
```

Success requires API `status == 1`, a successful transaction status, and an
exact transaction-reference match. The V1 client intentionally does not
compare amount or currency. Check those values against your own pending order
on the server before delivering value.

## Theming and redirect URL

`MoolreTheme` is optional. You can use your app's `MaterialTheme` and override
the button's `containerColor`, `contentColor`, `borderColor`, `borderWidth`, or
`shape`. The default styling matches the XML sample: white background, black
content, a 1dp `#FDB93C` border, and a light-grey disabled background.

`redirectUrl` defaults to `moolre://payment-callback` and must contain a URI
scheme and host. The SDK consumes this redirect inside its checkout WebView, so
the host application does not need to add a deep-link intent filter.
`webhookUrl` is an optional server-side callback and should normally be HTTPS.
