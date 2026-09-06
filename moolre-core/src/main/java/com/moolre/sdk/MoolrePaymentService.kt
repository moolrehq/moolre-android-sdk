package com.moolre.sdk

import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.PaymentParams
import com.moolre.sdk.model.PaymentResponse
import com.moolre.sdk.model.VerificationResponse
import com.moolre.sdk.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal

class MoolrePaymentService private constructor(
    private val client: OkHttpClient
) : MoolrePaymentGateway {

    companion object {
        /**
         * Creates the default network-backed payment gateway.
         */
        @JvmStatic
        fun create(): MoolrePaymentService = MoolrePaymentService(OkHttpClient())

        internal fun create(client: OkHttpClient): MoolrePaymentService = MoolrePaymentService(client)

        private const val MEDIA_TYPE = "application/json"
    }

    override suspend fun initiatePayment(params: PaymentParams): PaymentResponse {
        val request = buildInitiationRequest(params)

        return try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use(::parseInitiationResponse)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: JSONException) {
            throw MoolrePaymentException(
                code = "JSON_PARSING_ERROR",
                message = "Failed to parse API response: ${error.message}",
                cause = error
            )
        } catch (error: MoolrePaymentException) {
            throw error
        } catch (error: Exception) {
            throw MoolrePaymentException(
                code = "NETWORK_OR_UNKNOWN_ERROR",
                message = "Payment initiation failed: ${error.message}",
                cause = error
            )
        }
    }

    override suspend fun verifyPayment(
        reference: String,
        environment: MoolreEnvironment,
        apiUser: String,
        publicKey: String,
        accountNumber: String
    ): VerificationResponse {
        val request = buildVerificationRequest(
            reference = reference,
            environment = environment,
            apiUser = apiUser,
            publicKey = publicKey,
            accountNumber = accountNumber
        )

        return try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use(::parseVerificationResponse)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: JSONException) {
            throw MoolrePaymentException(
                code = "JSON_PARSING_ERROR",
                message = "Failed to parse verification response: ${error.message}",
                cause = error
            )
        } catch (error: MoolrePaymentException) {
            throw error
        } catch (error: Exception) {
            throw MoolrePaymentException(
                code = "NETWORK_OR_UNKNOWN_ERROR",
                message = "Payment verification failed: ${error.message}",
                cause = error
            )
        }
    }

    internal fun buildInitiationRequest(params: PaymentParams): Request {
        val fields = mutableListOf(
            jsonField("type", Constants.PAYMENT_LINK_TYPE.toString(), quoted = false),
            jsonField("amount", params.amount.toPlainString()),
            jsonField("email", params.email),
            jsonField("externalref", params.reference),
            jsonField("reusable", if (params.reusable) "1" else "0"),
            jsonField("currency", params.currency),
            jsonField("accountnumber", params.accountNumber),
            jsonField("redirect", params.redirect)
        )
        params.callback?.let { fields += jsonField("callback", it) }
        params.expirationTimeMinutes?.let {
            fields += jsonField("expiration_time", it.toString(), quoted = false)
        }
        if (params.metadata.isNotEmpty()) {
            val metadata = params.metadata.entries.joinToString(",") { (key, value) ->
                "${quoteJson(key)}:${quoteJson(value)}"
            }
            fields += "${quoteJson("metadata")}:{${metadata}}"
        }
        return buildRequest(
            url = params.environment.paymentEndpoint,
            apiUser = params.apiUser,
            publicKey = params.publicKey,
            body = "{${fields.joinToString(",")}}"
        )
    }

    internal fun buildVerificationRequest(
        reference: String,
        environment: MoolreEnvironment,
        apiUser: String,
        publicKey: String,
        accountNumber: String
    ): Request {
        val body = "{" + listOf(
            jsonField("type", Constants.PAYMENT_LINK_TYPE.toString(), quoted = false),
            jsonField("idtype", Constants.STATUS_ID_TYPE_EXTERNAL_REFERENCE.toString(), quoted = false),
            jsonField("id", reference),
            jsonField("accountnumber", accountNumber)
        ).joinToString(",") + "}"
        return buildRequest(
            url = environment.statusEndpoint,
            apiUser = apiUser,
            publicKey = publicKey,
            body = body
        )
    }

    private fun buildRequest(url: String, apiUser: String, publicKey: String, body: String): Request {
        return Request.Builder()
            .url(url)
            .addHeader("Content-Type", MEDIA_TYPE)
            .addHeader("X-API-USER", apiUser)
            .addHeader("X-API-PUBKEY", publicKey)
            .post(body.toRequestBody(MEDIA_TYPE.toMediaType()))
            .build()
    }

    private fun jsonField(key: String, value: String, quoted: Boolean = true): String {
        return "${quoteJson(key)}:${if (quoted) quoteJson(value) else value}"
    }

    private fun quoteJson(value: String): String {
        val escaped = buildString {
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (character.code < 0x20) {
                        append("\\u%04x".format(character.code))
                    } else {
                        append(character)
                    }
                }
            }
        }
        return "\"$escaped\""
    }

    private fun parseInitiationResponse(response: Response): PaymentResponse {
        val responseBody = response.body?.string()
            ?: throw MoolrePaymentException(
                code = "EMPTY_RESPONSE",
                message = "Empty response body from server. HTTP Status: ${response.code}"
            )
        val jsonResponse = JSONObject(responseBody)
        val apiMessage = jsonResponse.optString("message", "No message from API.")
        val rawApiErrorCode = jsonResponse.optString("code", Constants.ERROR_INITIATION_FAILED)
        val apiErrorCode = if (rawApiErrorCode == "INP02") {
            Constants.ERROR_DUPLICATE_REFERENCE
        } else {
            rawApiErrorCode
        }

        if (!response.isSuccessful || jsonResponse.optStatus("status") != 1) {
            val suffix = if (response.isSuccessful) "" else " HTTP Status: ${response.code}."
            throw MoolrePaymentException(
                code = apiErrorCode,
                message = "Payment initiation failed. API Message: $apiMessage.$suffix"
            )
        }

        val data = jsonResponse.optJSONObject("data")
            ?: throw MoolrePaymentException(
                apiErrorCode,
                "Successful response but 'data' object is missing: $apiMessage"
            )
        val authorizationUrl = data.optString("authorization_url")
        val reference = data.optString("reference")
        if (authorizationUrl.isBlank() || reference.isBlank()) {
            throw MoolrePaymentException(
                apiErrorCode,
                "Successful response is missing checkout URL or reference: $apiMessage"
            )
        }
        return PaymentResponse(authorizationUrl, reference)
    }

    private fun parseVerificationResponse(response: Response): VerificationResponse {
        val responseBody = response.body?.string()
            ?: throw MoolrePaymentException(
                code = "EMPTY_RESPONSE",
                message = "Empty response body for verification. HTTP Status: ${response.code}"
            )
        val jsonResponse = JSONObject(responseBody)
        val envelopeStatus = jsonResponse.optStatus("status")
        if (!response.isSuccessful || envelopeStatus != 1) {
            val message = jsonResponse.optString("message", "Verification failed due to API error status.")
            val code = jsonResponse.optString("code", Constants.ERROR_VERIFICATION_FAILED)
            throw MoolrePaymentException(code, message)
        }

        val data = jsonResponse.optJSONObject("data")
            ?: throw MoolrePaymentException(
                Constants.ERROR_VERIFICATION_FAILED,
                "Verification response is missing 'data'."
            )
        val externalReference = data.optString("externalref")
        if (externalReference.isBlank()) {
            throw MoolrePaymentException(
                Constants.ERROR_MISSING_REFERENCE,
                "Verification response is missing 'externalref'."
            )
        }

        return VerificationResponse(
            status = envelopeStatus,
            transactionStatus = data.optStatus("txstatus"),
            reference = externalReference,
            amount = data.optBigDecimal("amount") ?: BigDecimal.ZERO,
            accountNumber = data.optString("accountnumber").takeIf { it.isNotBlank() },
            transactionId = data.optString("transactionid").takeIf { it.isNotBlank() },
            timestamp = data.optString("ts").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.optBigDecimal(key: String): BigDecimal? {
        val value = opt(key) ?: return null
        if (value == JSONObject.NULL) return null
        return when (value) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }

    private fun JSONObject.optStatus(key: String): Int {
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is Boolean -> if (value) 1 else 0
            is String -> when {
                value == "1" || value.equals("true", ignoreCase = true) -> 1
                else -> value.toIntOrNull() ?: 0
            }
            else -> 0
        }
    }
}
