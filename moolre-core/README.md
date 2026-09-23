# Moolre Core SDK

`android-sdk-core` contains the UI-independent payment models, environment
selection, OkHttp transport, gateway contract, and verification coordinator.
The XML, Compose, and checkout artifacts share this implementation.

Most applications should depend on `android-sdk-views` or
`android-sdk-compose` instead of adding this artifact directly.

## Installation

```kotlin
dependencies {
    implementation("com.moolre:android-sdk-core:1.0.0")
}
```

## Credentials and environments

Choose one environment for the entire request. The API user, public key,
account number, payment references, and status checks must all belong to that
environment:

```kotlin
val config = MoolreConfig(
    environment = MoolreEnvironment.SANDBOX, // use LIVE for production
    apiUser = "your-sandbox-api-user",
    publicKey = "your-sandbox-public-key",
    accountNumber = "your-sandbox-account-number"
)
```

`local.properties` is only used by this repository's sample apps. A published
library does not read it; your host app supplies these values through
`MoolreConfig` or the View button properties. Never commit real credentials.

## Advanced usage

```kotlin
import com.moolre.sdk.MoolrePaymentCoordinator
import com.moolre.sdk.MoolrePaymentService
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import java.math.BigDecimal

val coordinator = MoolrePaymentCoordinator(MoolrePaymentService.create())
val params = PaymentParams(
    amount = BigDecimal("25.00"),
    environment = MoolreEnvironment.SANDBOX,
    apiUser = "your-api-user",
    publicKey = "your-public-key",
    accountNumber = "your-account-number",
    currency = "GHS",
    email = "customer@example.com",
    reference = "order-1001",
    callback = "https://merchant.example.com/moolre/webhook",
    redirect = "moolre://payment-callback"
)

val session = coordinator.initiatePayment(params).getOrThrow()
// Launch session.authorizationUrl in your checkout UI, then:
val result = coordinator.verifyPayment(params, referenceFromCheckout)
```

`MoolrePaymentService` posts payment links to the environment-specific
`/embed/link` endpoint and verifies them at `/open/transact/status`. Requests
send `X-API-USER` and `X-API-PUBKEY` headers. Inject a custom
`MoolrePaymentGateway` into the coordinator for tests or an application-owned
transport.

## Verification policy

The coordinator sends the original `params.reference` as the status API's
external-reference lookup. It reports success only when the status envelope is
`status == 1`, the transaction status is successful, and the returned
`externalref` exactly matches `params.reference`. The redirect reference is
validated by the checkout runtime and may be Moolre's generated transaction
reference. Amount and currency are exposed in `VerificationResponse` but are
not compared by this V1 client. Merchants must perform order-level checks on
their server before fulfilment.
