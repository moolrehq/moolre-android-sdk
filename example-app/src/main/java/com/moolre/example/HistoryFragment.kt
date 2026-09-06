package com.moolre.example // Your package name

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager // Import LinearLayoutManager
import com.moolre.example.databinding.FragmentHistoryBinding
import java.math.BigDecimal

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var paymentHistoryAdapter: PaymentHistoryAdapter // Declare adapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPaymentHistory() // Method to load data
    }

    private fun setupRecyclerView() {
        paymentHistoryAdapter = PaymentHistoryAdapter() // Initialize adapter
        binding.recyclerViewHistory.apply {
            adapter = paymentHistoryAdapter
            layoutManager = LinearLayoutManager(context) // Set LayoutManager
            // You can also add ItemDecorations for dividers, etc.
        }
    }

    private fun loadPaymentHistory() {
        // TODO: Replace this with your actual data fetching logic (e.g., from ViewModel, database, API)
        val dummyHistory = listOf(
            PaymentHistoryItem("txn_123", BigDecimal("50.00"), "2023-07-20", "Successful"),
            PaymentHistoryItem("txn_124", BigDecimal("25.50"), "2023-07-21", "Failed"),
            PaymentHistoryItem("txn_125", BigDecimal("75.20"), "2023-07-22", "Pending"),
            PaymentHistoryItem("txn_126", BigDecimal("10.00"), "2023-07-23", "Successful")
        )

        if (dummyHistory.isEmpty()) {
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewEmptyHistory.visibility = View.VISIBLE
        } else {
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.textViewEmptyHistory.visibility = View.GONE
            paymentHistoryAdapter.submitList(dummyHistory) // Submit data to ListAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
