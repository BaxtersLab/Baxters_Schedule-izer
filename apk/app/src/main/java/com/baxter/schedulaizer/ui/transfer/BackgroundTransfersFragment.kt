package com.baxter.schedulaizer.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import com.baxter.schedulaizer.databinding.FragmentBackgroundTransfersBinding
import com.baxter.schedulaizer.transfer.TransferWorkScheduler

class BackgroundTransfersFragment : Fragment() {

    private var _binding: FragmentBackgroundTransfersBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BackgroundTransfersViewModel
    private lateinit var adapter: BackgroundTransfersAdapter
    private var hasAutoScrolled = false
    private var hasAutoOpenedDetails = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackgroundTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = SchedulaizerApp.get(requireContext())
        val repo = app.attachmentRepository

        viewModel = ViewModelProvider(
            this,
            BackgroundTransfersVMFactory(repo)
        )[BackgroundTransfersViewModel::class.java]

        adapter = BackgroundTransfersAdapter(
            onOpen = { id ->
                val bundle = Bundle().apply { putLong("attachmentId", id) }
                findNavController().navigate(com.baxter.schedulaizer.R.id.transferDetailsFragment, bundle)
            },
            onRetry = { id ->
                TransferWorkScheduler.enqueueTransfer(requireContext(), id)
                Toast.makeText(requireContext(), "Retry scheduled", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerTransfers.adapter = adapter

        viewModel.transfers.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            // Auto-scroll to first active transfer (one-time per view) if available
            if (!hasAutoScrolled) {
                val main = activity as? com.baxter.schedulaizer.ui.MainActivity
                val activeIds = main?.getActiveTransferIds() ?: emptyList()
                if (activeIds.isNotEmpty() && list.isNotEmpty()) {
                    // find the first active id that exists in the current list
                    val targetId = activeIds.firstOrNull { aid -> list.any { it.id == aid } }
                    if (targetId != null) {
                        val index = list.indexOfFirst { it.id == targetId }
                        if (index >= 0) {
                            binding.recyclerTransfers.scrollToPosition(index)
                            hasAutoScrolled = true
                        }

                    }

                    
                }
            }

            // One-time auto-open when exactly one active transfer exists and it's in the list
            if (!hasAutoOpenedDetails) {
                val main = activity as? com.baxter.schedulaizer.ui.MainActivity
                val activeIds = main?.getActiveTransferIds() ?: emptyList()
                if (activeIds.size == 1) {
                    val activeId = activeIds.first()
                    val idx = adapter.indexOfId(activeId)
                    if (idx != -1) {
                        hasAutoOpenedDetails = true
                        binding.recyclerTransfers.postDelayed({
                            openTransferDetails(activeId)
                        }, 120)
                    }
                }
            }
        }

        // initial load handled by LiveData
    }

    private fun openTransferDetails(id: Long) {
        val bundle = Bundle().apply { putLong("attachmentId", id) }
        findNavController().navigate(com.baxter.schedulaizer.R.id.transferDetailsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
