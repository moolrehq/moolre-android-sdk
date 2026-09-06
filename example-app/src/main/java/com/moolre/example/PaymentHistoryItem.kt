package com.moolre.example

import java.math.BigDecimal

data class PaymentHistoryItem(
    val transactionId: String,
    val amount: BigDecimal,
    val date: String, // replace with  java.util.Date, kotlinx.datetime.Instant, or Long (for timestamp) for better date handling
    val status: String,
    // val paymentMethod: String,
    // val recipient: String
)
