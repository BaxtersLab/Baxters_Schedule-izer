package com.baxter.schedulaizer.ui.transfer

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.baxter.schedulaizer.transfer.FileTransferManager.Event
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.databinding.FragmentTransferBinding
import com.baxter.schedulaizer.transfer.FileTransferManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.baxter.schedulaizer.transfer.TransferWorkScheduler
import android.content.Context
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.transfer.BudgetBlasterBridge
import com.baxter.schedulaizer.util.AppConstants

class TransferFragment : Fragment() {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!

    // navArgs not used; read raw argument bundle for compatibility

    private var transferJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val attachmentId = arguments?.getLong("attachmentId") ?: return

        // load attachment from DB and display preview
        lifecycleScope.launch {
            val app = SchedulaizerApp.get(requireContext())
            val attachment = app.attachmentRepository.get(attachmentId)
            if (attachment == null) {
                binding.statusText.text = "Attachment not found"
                return@launch
            }

            val file = java.io.File(attachment.localPath)
            val uri = android.net.Uri.fromFile(file)
            binding.previewImage.setImageURI(uri)

            setupNativeStatus()
            setupButtons(attachment, uri, attachmentId)
        }
    }

    private fun setupNativeStatus() {
        if (!com.baxter.schedulaizer.transfer.RemoteDexterNative.libraryLoaded) {
            binding.nativeStatus.text = "Native engine unavailable — using staged transfer"
        } else {
            binding.nativeStatus.text = "Native engine active — streaming enabled"
        }
    }

    private fun setupButtons(attachment: AttachmentEntity, uri: Uri, attachmentId: Long) {
        binding.sendButton.setOnClickListener {
            startTransfer(attachmentId, uri)
        }

        binding.retryButton.setOnClickListener {
            startTransfer(attachmentId, uri)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonSendInBackground.setOnClickListener {
            TransferWorkScheduler.enqueueTransfer(requireContext(), attachmentId)
            Toast.makeText(requireContext(), "Transfer scheduled in background", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.buttonSendBudgetBlaster.setOnClickListener {
            sendToBudgetBlaster(attachment)
        }
    }

    /**
     * Pushes the captured image to Budget Blaster's POST /receipt endpoint for OCR.
     * The endpoint is configured in Settings; Budget Blaster must be bound to the
     * LAN for the phone to reach it.
     */
    private fun sendToBudgetBlaster(attachment: AttachmentEntity) {
        val endpoint = requireContext()
            .getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(
                AppConstants.PREF_BUDGET_BLASTER_ENDPOINT,
                AppConstants.DEFAULT_BUDGET_BLASTER_ENDPOINT
            )
            ?.trim()
            .orEmpty()

        if (endpoint.isEmpty()) {
            Toast.makeText(requireContext(), "Set the Budget Blaster endpoint in Settings", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSendBudgetBlaster.isEnabled = false
        binding.statusText.text = "Sending to Budget Blaster…"

        lifecycleScope.launch {
            val ok = BudgetBlasterBridge.sendReceipt(attachment, endpoint)
            if (ok) {
                try {
                    SchedulaizerApp.get(requireContext()).attachmentRepository.markSentToBudgetBlaster(attachment.id)
                } catch (_: Throwable) {}
                binding.statusText.text = "Sent to Budget Blaster"
                Toast.makeText(requireContext(), "Sent to Budget Blaster", Toast.LENGTH_SHORT).show()
            } else {
                binding.statusText.text = "Budget Blaster send failed"
                Toast.makeText(
                    requireContext(),
                    "Budget Blaster send failed — check the endpoint and that the PC is reachable",
                    Toast.LENGTH_LONG
                ).show()
            }
            _binding?.buttonSendBudgetBlaster?.isEnabled = true
        }
    }

    private fun startTransfer(attachmentId: Long, uri: Uri) {
        binding.statusText.text = "Starting transfer…"
        binding.progressBar.progress = 0
        binding.progressBar.visibility = View.VISIBLE
        binding.retryButton.visibility = View.GONE

        transferJob?.cancel()
        transferJob = lifecycleScope.launch {
            FileTransferManager.sendFile(attachmentId, uri).collectLatest { event ->
                when (event) {
                    is Event.Staged -> {
                        binding.statusText.text = "File staged (native unavailable)"
                        binding.progressBar.visibility = View.GONE
                        binding.retryButton.visibility = View.VISIBLE
                    }

                    is Event.Progress -> {
                        binding.statusText.text = "Uploading… ${event.percent}%"
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = event.percent
                    }

                    is Event.Success -> {
                        binding.statusText.text = "Transfer complete"
                        binding.progressBar.progress = 100
                        binding.retryButton.visibility = View.GONE
                    }

                    is Event.Error -> {
                        binding.statusText.text = "Transfer failed: ${event.message}"
                        binding.progressBar.visibility = View.GONE
                        binding.retryButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        transferJob?.cancel()
        _binding = null
    }
}
