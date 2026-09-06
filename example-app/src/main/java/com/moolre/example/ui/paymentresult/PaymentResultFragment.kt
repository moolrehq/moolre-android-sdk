package com.moolre.example.ui.paymentresult

import com.moolre.example.ui.paymentresult.PaymentResultFragmentArgs
import com.moolre.example.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.moolre.example.databinding.FragmentPaymentResultBinding

class PaymentResultFragment : Fragment() {
    private var _binding: FragmentPaymentResultBinding? = null
    private val binding get() = _binding!!
    private val args: PaymentResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.isSuccess) {
            setupSuccessUI()
        } else {
            setupErrorUI()
        }

        binding.homeButton.setOnClickListener {
            findNavController().popBackStack(R.id.checkoutFragment, false)
        }
    }

    private fun setupSuccessUI() {
        val color = ContextCompat.getColor(requireContext(), R.color.payment_result_text_success)
        binding.root.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.payment_result_background_success)
        )

        binding.resultIcon.setImageResource(R.drawable.ic_success)
        binding.resultTitle.setTextColor(color)
        binding.resultTitle.text = getString(R.string.payment_successful)

        binding.resultMessage.setTextColor(color)
        binding.resultMessage.text = getString(R.string.reference_label, args.reference)
    }

    private fun setupErrorUI() {
        val color = ContextCompat.getColor(requireContext(), R.color.payment_result_text_error)
        binding.root.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.payment_result_background_error)
        )

        binding.resultIcon.setImageResource(R.drawable.ic_error)
        binding.resultTitle.setTextColor(color)
        binding.resultTitle.text = getString(R.string.payment_failed)

        binding.resultMessage.setTextColor(color)
        binding.resultMessage.text = getString(
            R.string.error_message_format,
            args.errorCode,
            args.errorMessage
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}