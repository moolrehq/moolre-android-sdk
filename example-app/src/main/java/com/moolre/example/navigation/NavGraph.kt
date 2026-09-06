package com.moolre.example.navigation
import android.os.Bundle
import androidx.navigation.NavController
import com.moolre.example.R

object NavGraph {
    fun navigateToPaymentResult(
        navController: NavController,
        isSuccess: Boolean,
        reference: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null
    ) {
        // Create a Bundle to hold the arguments
        val args = Bundle().apply {
            putBoolean("isSuccess", isSuccess)
            putString("reference", reference) // Use putString for nullable strings
            putString("errorCode", errorCode)
            putString("errorMessage", errorMessage)
        }

        navController.navigate(
            resId = R.id.action_checkoutFragment_to_paymentResultFragment,
            args = args // Pass the Bundle here
        )
    }
}