package com.moolre.example.ui.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.moolre.example.navigation.NavGraph // Assuming your NavGraph object [3]
import com.moolre.sdk.MoolreCheckoutContract
import com.moolre.sdk.MoolrePayButton
import com.moolre.example.databinding.FragmentCheckoutBinding
import java.text.NumberFormat
import java.util.Locale

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CheckoutViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private val moolreCheckoutLauncher = registerForActivityResult(MoolreCheckoutContract()) { result ->
        _binding?.moolrePayButton?.handleCheckoutResult(result)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupMoolreButton()

        binding.addRandomItemButton.setOnClickListener {
            viewModel.addRandomProduct()
        }

    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            viewModel.removeProduct(product.id)
        }
        binding.productsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
            if (products.isEmpty()) {
                // Optionally show an empty state message
            }
        }

        viewModel.totalAmount.observe(viewLifecycleOwner) { total ->
            val format = NumberFormat.getCurrencyInstance(Locale("en", "GH"))
            binding.totalPrice.text = format.format(total)
            // Update MoolrePayButton amount
            binding.moolrePayButton.amount = total
        }
    }

    private fun setupMoolreButton() {
        binding.moolrePayButton.apply {
            setCheckoutLauncher(moolreCheckoutLauncher)
            environment = viewModel.environment
            apiUser = viewModel.apiUser
            publicKey = viewModel.publicKey
            accountNumber = viewModel.accountNumber
            email = viewModel.email
            reference = viewModel.customReference // This will generate a new one
            webhookUrl = viewModel.webhookUrl
            redirectUrl = viewModel.redirectUrl
            // Amount is set via observer

            setOnPaymentSuccessListener { ref ->
                Log.d("CheckoutFragment", "Payment Success! Reference: $ref")
                // Using NavGraph to navigate as per your existing setup [3]
                NavGraph.navigateToPaymentResult(
                    navController = findNavController(),
                    isSuccess = true,
                    reference = ref
                )
            }

            setOnPaymentErrorListener { errorCode, errorMessage ->
                Log.e("CheckoutFragment", "Payment Error! Code: $errorCode, Message: $errorMessage")
                NavGraph.navigateToPaymentResult(
                    navController = findNavController(),
                    isSuccess = false,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                )
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
