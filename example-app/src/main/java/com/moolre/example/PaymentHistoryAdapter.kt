package com.moolre.example

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.moolre.example.databinding.ItemHistoryEntryBinding // Import view binding class
import java.math.RoundingMode

class PaymentHistoryAdapter :
    ListAdapter<PaymentHistoryItem, PaymentHistoryAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding =
            ItemHistoryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val paymentItem = getItem(position)
        holder.bind(paymentItem)
    }

    inner class PaymentViewHolder(private val binding: ItemHistoryEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentItem: PaymentHistoryItem) {
            binding.textViewPaymentAmount.text = "GHS ${paymentItem.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
            binding.textViewPaymentDate.text = paymentItem.date
            binding.textViewPaymentStatus.text = paymentItem.status
            // TODO: Set up click listeners if needed, e.g., for item details
            // binding.root.setOnClickListener { /* Handle item click */ }
        }
    }

    // DiffUtil helps RecyclerView efficiently update items
    class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentHistoryItem>() {
        override fun areItemsTheSame(oldItem: PaymentHistoryItem, newItem: PaymentHistoryItem): Boolean {
            return oldItem.transactionId == newItem.transactionId // Use a unique ID
        }

        override fun areContentsTheSame(oldItem: PaymentHistoryItem, newItem: PaymentHistoryItem): Boolean {
            return oldItem == newItem // Relies on data class `equals`
        }
    }
}
