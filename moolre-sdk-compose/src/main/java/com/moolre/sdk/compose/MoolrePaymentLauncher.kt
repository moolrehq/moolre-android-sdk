package com.moolre.sdk.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.moolre.sdk.MoolreCheckoutContract
import com.moolre.sdk.MoolreCheckoutResult
import com.moolre.sdk.MoolrePaymentCoordinator
import com.moolre.sdk.MoolrePaymentException
import com.moolre.sdk.MoolrePaymentResult
import com.moolre.sdk.MoolrePaymentService
import com.moolre.sdk.model.MoolreCheckoutSession
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.MoolrePaymentRequest
import com.moolre.sdk.model.MoolreReferenceGenerator
import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.model.toPaymentParams
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal class MoolrePaymentLauncherState(
    var isProcessing: Boolean = false,
    var pendingPaymentParams: PaymentParams? = null,
    var retryReference: String? = null
)

internal val MoolrePaymentLauncherStateSaver: Saver<MoolrePaymentLauncherState, Any> =
    listSaver<MoolrePaymentLauncherState, Any>(
        save = { state ->
            val params = state.pendingPaymentParams
            listOf(
                state.isProcessing,
                params?.amount?.toPlainString().orEmpty(),
                params?.environment?.name.orEmpty(),
                params?.apiUser.orEmpty(),
                params?.publicKey.orEmpty(),
                params?.accountNumber.orEmpty(),
                params?.currency.orEmpty(),
                params?.email.orEmpty(),
                params?.reference.orEmpty(),
                params?.callback.orEmpty(),
                params?.redirect.orEmpty(),
                params?.reusable ?: false,
                params?.expirationTimeMinutes ?: -1,
                state.retryReference.orEmpty()
            )
        },
        restore = { values ->
            val amount = (values.getOrNull(1) as? String)?.takeIf { it.isNotBlank() }
            val params = if (amount == null) {
                null
            } else {
                runCatching {
                    PaymentParams(
                        amount = amount.toBigDecimal(),
                        environment = MoolreEnvironment.valueOf(values.getOrNull(2) as? String ?: ""),
                        apiUser = values.getOrNull(3) as? String ?: "",
                        publicKey = values.getOrNull(4) as? String ?: "",
                        accountNumber = values.getOrNull(5) as? String ?: "",
                        currency = values.getOrNull(6) as? String ?: "",
                        email = values.getOrNull(7) as? String ?: "",
                        reference = values.getOrNull(8) as? String ?: "",
                        callback = (values.getOrNull(9) as? String)?.takeIf { it.isNotEmpty() },
                        redirect = values.getOrNull(10) as? String ?: "",
                        reusable = values.getOrNull(11) as? Boolean == true,
                        expirationTimeMinutes = (values.getOrNull(12) as? Int)?.takeIf { it >= 1 }
                    )
                }.getOrNull()
            }
            MoolrePaymentLauncherState(
                isProcessing = (values.getOrNull(0) as? Boolean == true) && params != null,
                pendingPaymentParams = params,
                retryReference = (values.getOrNull(13) as? String)?.takeIf { it.isNotBlank() }
                    ?: params?.reference
            )
        }
    )

/**
 * State-holder that prepares, launches, and verifies one checkout at a time.
 */
@Stable
class MoolrePaymentLauncher internal constructor(
    private val config: MoolreConfig,
    private val coordinator: MoolrePaymentCoordinator,
    private val scope: CoroutineScope,
    private val launchCheckout: (MoolreCheckoutSession) -> Unit,
    private val emitResult: (MoolrePaymentResult) -> Unit,
    private val savedState: MoolrePaymentLauncherState
) {
    private var processingState by mutableStateOf(
        savedState.isProcessing && savedState.pendingPaymentParams != null
    )

    var isProcessing: Boolean
        get() = processingState
        private set(value) {
            processingState = value
            savedState.isProcessing = value
        }

    private var pendingPaymentParams: PaymentParams?
        get() = savedState.pendingPaymentParams
        set(value) {
            savedState.pendingPaymentParams = value
        }

    /**
     * Starts payment preparation and opens the SDK checkout activity.
     */
    fun launch(request: MoolrePaymentRequest) {
        if (isProcessing) return

        val effectiveReference = request.reference?.takeIf { it.isNotBlank() }
            ?: savedState.retryReference
            ?: MoolreReferenceGenerator.generate()
        val params = try {
            request.copy(reference = effectiveReference).toPaymentParams(config)
        } catch (error: IllegalArgumentException) {
            emitResult(
                MoolrePaymentResult.Failure(
                    code = if (request.amount <= BigDecimal.ZERO) Constants.ERROR_INVALID_AMOUNT else Constants.ERROR_INVALID_CONFIG,
                    message = error.message ?: "Payment request is invalid."
                )
            )
            return
        }

        pendingPaymentParams = params
        savedState.retryReference = params.reference
        isProcessing = true
        scope.launch {
            try {
                coordinator.initiatePayment(params).fold(
                    onSuccess = { session ->
                        pendingPaymentParams = params
                        launchCheckout(session)
                    },
                    onFailure = { error ->
                        finishInitiation(
                            MoolrePaymentResult.Failure(
                                code = (error as? MoolrePaymentException)?.code
                                    ?: Constants.ERROR_INITIATION_FAILED,
                                message = error.message ?: "Payment initiation failed."
                            )
                        )
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                finishInitiation(
                    MoolrePaymentResult.Failure(
                        code = Constants.ERROR_LAUNCH_FAILED,
                        message = error.message ?: "Could not open payment checkout."
                    )
                )
            }
        }
    }

    internal fun handleCheckoutResult(result: MoolreCheckoutResult) {
        scope.launch {
            when (result) {
                is MoolreCheckoutResult.Completed -> {
                    val params = pendingPaymentParams
                    if (params == null) {
                        complete(
                            MoolrePaymentResult.Failure(
                                Constants.ERROR_INVALID_CONFIG,
                                "No pending payment was found for this checkout result."
                            )
                        )
                    } else {
                        complete(coordinator.verifyPayment(params, result.reference))
                    }
                }
                MoolreCheckoutResult.Cancelled -> complete(MoolrePaymentResult.Cancelled)
                is MoolreCheckoutResult.Failed -> complete(
                    MoolrePaymentResult.Failure(result.code, result.message)
                )
            }
        }
    }

    private fun complete(result: MoolrePaymentResult) {
        pendingPaymentParams = null
        savedState.retryReference = null
        isProcessing = false
        emitResult(result)
    }

    private fun finishInitiation(result: MoolrePaymentResult) {
        isProcessing = false
        emitResult(result)
    }
}

/**
 * Remembers a Compose payment launcher across recompositions.
 */
@Composable
fun rememberMoolrePaymentLauncher(
    config: MoolreConfig,
    onResult: (MoolrePaymentResult) -> Unit
): MoolrePaymentLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnResult = rememberUpdatedState(onResult)
    val savedState = rememberSaveable(
        config.environment,
        config.apiUser,
        config.publicKey,
        config.accountNumber,
        config.webhookUrl,
        config.redirectUrl,
        saver = MoolrePaymentLauncherStateSaver
    ) { MoolrePaymentLauncherState() }
    val coordinator = remember(context) {
        MoolrePaymentCoordinator(MoolrePaymentService.create())
    }

    lateinit var paymentLauncher: MoolrePaymentLauncher
    val checkoutActivityLauncher = rememberLauncherForActivityResult(MoolreCheckoutContract()) { result ->
        paymentLauncher.handleCheckoutResult(result)
    }

    paymentLauncher = remember(config, coordinator, scope, checkoutActivityLauncher) {
        MoolrePaymentLauncher(
            config = config,
            coordinator = coordinator,
            scope = scope,
            launchCheckout = { session -> checkoutActivityLauncher.launch(session) },
            emitResult = { result -> latestOnResult.value(result) },
            savedState = savedState
        )
    }
    return paymentLauncher
}
