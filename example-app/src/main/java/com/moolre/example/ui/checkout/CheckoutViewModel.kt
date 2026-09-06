package com.moolre.example.ui.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.moolre.example.data.Product
import com.moolre.sdk.model.MoolreEnvironment
import java.math.BigDecimal
import java.util.Date
import kotlin.random.Random

class CheckoutViewModel : ViewModel() {

    private val _products = MutableLiveData<List<Product>>(
        listOf(
            Product(id = "1", name = "Wireless Headphones", price = BigDecimal("0.50")),
            Product(id = "2", name = "Bluetooth Speaker", price = BigDecimal("0.20"))
        )
    )
    val products: LiveData<List<Product>> = _products
    private val _totalAmount = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalAmount: LiveData<BigDecimal> = _totalAmount
    val customReference: String
        get() = "my-txn-${Date().time}" // Generates a new one each time it's accessed

    val environment = MoolreEnvironment.SANDBOX
    val apiUser = "replace-with-your-api-user"
    val publicKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyaWQiOjUyNzgzLCJleHAiOjE5MjUwMDk5OTl9.E34EwypYvzniaEoOoRKfkdFgCWvkU7SHo6jfOsoW0O8"
    val accountNumber = "527830503234"

    val email = "customer@example.com"
    val webhookUrl: String? = null
    val redirectUrl = "moolre-example://payment-callback" // Your app's return URI

    private val sampleProductNames = listOf(
        "Smart Watch", "USB Charger", "Gaming Mouse",
        "LED Bulb", "Water Bottle", "Backpack"
    )

    init {
        calculateTotal()
    }

    private fun calculateTotal() {
        val currentTotal = _products.value?.fold(BigDecimal.ZERO) { total, product -> total + product.price }
            ?: BigDecimal.ZERO
        _totalAmount.value = currentTotal
    }

    fun removeProduct(productId: String) {
        _products.value = _products.value?.filterNot { it.id == productId }
        calculateTotal()
    }

    fun addRandomProduct() {
        val name = sampleProductNames.random()
        val price = BigDecimal.valueOf(Random.nextInt(10, 111).toLong(), 2)
        val newProduct = Product(
            id = "${Date().time}-${Random.nextInt()}",
            name = name,
            price = price
        )
        _products.value = (_products.value ?: emptyList()) + newProduct
        calculateTotal()
    }
}
