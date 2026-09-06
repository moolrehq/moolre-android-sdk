package com.moolre.moolre_android_sdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moolre.sdk.MoolrePaymentResult
import com.moolre.sdk.compose.MoolrePayButton
import com.moolre.sdk.compose.MoolreTheme
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolreEnvironment
import com.moolre.sdk.model.MoolrePaymentRequest
import com.moolre.moolre_android_sdk.ui.theme.MoolreandroidsdkTheme
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoolreandroidsdkTheme {
                MoolreTheme {
                    ComposeCheckoutDemo()
                }
            }
        }
    }
}

@Composable
private fun ComposeCheckoutDemo() {
    var paymentResult by remember { mutableStateOf<MoolrePaymentResult?>(null) }
    val products = remember {
        mutableStateListOf(
            SampleProduct("1", "Wireless Headphones", BigDecimal("0.50")),
            SampleProduct("2", "Bluetooth Speaker", BigDecimal("0.20"))
        )
    }
    val totalAmount by remember {
        derivedStateOf {
            products.fold(BigDecimal.ZERO) { total, product -> total + product.price }
        }
    }
    val config = remember {
        MoolreConfig(
            environment = MoolreEnvironment.SANDBOX,
            apiUser = "danitogh",
            publicKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyaWQiOjUyNzgzLCJleHAiOjE5NTY1NDU5OTl9.kL1XpBENHyaEBi7Miq_e1KPmpdWVK-MgVuhmur0SRoU",
            accountNumber = "527830503234",
            redirectUrl = "moolre-compose://payment-callback"
        )
    }
    val request = MoolrePaymentRequest(
        amount = totalAmount,
        currency = "GHS",
        email = "customer@example.com",
        reference = "compose-demo-payment"
    )
    val sampleProductNames = remember {
        listOf("Smart Watch", "USB Charger", "Gaming Mouse", "LED Bulb", "Water Bottle", "Backpack")
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Moolre Compose checkout",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(text = "Add items, review the total, then pay with the shared Moolre checkout flow.")
                }
            }

            items(products, key = SampleProduct::id) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = formatGhs(product.price))
                        }
                        IconButton(
                            onClick = { products.remove(product) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove ${product.name}"
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val name = sampleProductNames.random()
                        val price = BigDecimal.valueOf(Random.nextInt(10, 111).toLong(), 2)
                        products += SampleProduct(
                            id = "${System.currentTimeMillis()}-${Random.nextInt()}",
                            name = name,
                            price = price
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(text = "Add random item")
                }
            }

            item { HorizontalDivider() }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total", style = MaterialTheme.typography.titleLarge)
                    Text(text = formatGhs(totalAmount), style = MaterialTheme.typography.titleLarge)
                }
            }

            item {
                MoolrePayButton(
                    config = config,
                    payment = request,
                    enabled = totalAmount > BigDecimal.ZERO,
                    modifier = Modifier.fillMaxWidth(),
                    showAmount = true,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    borderColor = Color(0xFFFDB93C),
                    borderWidth = 1.dp,
                    onResult = { paymentResult = it }
                )
            }

            item {
                Text(
                    text = "This demo uses the same payment flow as the XML sample.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            paymentResult?.let { result ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = result.asDisplayText(),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class SampleProduct(
    val id: String,
    val name: String,
    val price: BigDecimal
)

private fun formatGhs(amount: BigDecimal): String {
    return "GHS ${amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
}

private fun MoolrePaymentResult.asDisplayText(): String {
    return when (this) {
        is MoolrePaymentResult.Success -> "Payment verified: $reference"
        MoolrePaymentResult.Cancelled -> "Payment cancelled before verification."
        is MoolrePaymentResult.Failure -> "Payment failed (${code}): $message"
    }
}
