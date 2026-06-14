package com.baxter.schedulaizer.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import com.baxter.schedulaizer.databinding.FragmentTransferDetailsBinding
import com.baxter.schedulaizer.transfer.TransferWorkScheduler
import java.util.Date

class TransferDetailsFragment : Fragment() {

    private var _binding: FragmentTransferDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TransferDetailsViewModel
    private var attachmentId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachmentId = requireArguments().getLong("attachmentId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = SchedulaizerApp.get(requireContext())
        val repo = app.attachmentRepository

        viewModel = ViewModelProvider(
            this,
            TransferDetailsVMFactory(repo)
        )[TransferDetailsViewModel::class.java]

        viewModel.load(attachmentId)

        viewModel.attachment.observe(viewLifecycleOwner) { att ->
            if (att == null) return@observe

            binding.textState.text = "State: ${att.state}"
            binding.textFileName.text = "File: ${att.fileName}"
            binding.textMime.text = "MIME: ${att.mimeType}"
            binding.textSize.text = "Size: ${att.fileSizeBytes} bytes"
            binding.textTimestamp.text = "Updated: ${Date(att.updatedAt)}"
        }

        binding.buttonRetry.setOnClickListener {
            TransferWorkScheduler.enqueueTransfer(requireContext(), attachmentId)
            Toast.makeText(requireContext(), "Retry scheduled", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSendInBackground.setOnClickListener {
            TransferWorkScheduler.enqueueTransfer(requireContext(), attachmentId)
            Toast.makeText(requireContext(), "Background transfer scheduled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
