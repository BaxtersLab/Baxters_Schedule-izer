package com.baxter.schedulaizer.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.baxter.schedulaizer.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureFragment : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) startCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_capture, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val captureButton = view.findViewById<ImageButton>(R.id.captureButton)

        // Request permission if needed
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }

        captureButton.setOnClickListener {
            val imgCapture = imageCapture
            if (imgCapture == null) {
                Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val capturesDir = File(requireContext().cacheDir, "captures")
            if (!capturesDir.exists()) capturesDir.mkdirs()
            val photoFile = File(capturesDir, "capture_${System.currentTimeMillis()}.jpg")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imgCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri: Uri = Uri.fromFile(photoFile)
                    activity?.runOnUiThread {
                        // Save attachment to DB and navigate with attachmentId
                        val app = requireActivity().application as com.baxter.schedulaizer.SchedulaizerApp
                        lifecycleScope.launch {
                            // Use metadata extractor
                            val meta = com.baxter.schedulaizer.data.attachment.AttachmentMetadataExtractor.extract(requireContext(), Uri.fromFile(photoFile))

                            val entity = com.baxter.schedulaizer.data.db.entity.AttachmentEntity(
                                localPath = meta.localPath ?: photoFile.absolutePath,
                                mimeType = meta.mimeType ?: "image/jpeg",
                                attachmentType = "image",
                                fileName = meta.fileName ?: photoFile.name,
                                fileSizeBytes = meta.fileSize ?: photoFile.length(),
                                capturedMs = System.currentTimeMillis(),
                                createdMs = System.currentTimeMillis()
                            )

                            val id = try {
                                app.attachmentRepository.save(entity)
                            } catch (e: Exception) {
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Failed to save attachment", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            val bundle = bundleOf("attachmentId" to id)
                            findNavController().navigate(com.baxter.schedulaizer.R.id.transferFragment, bundle)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                val previewView = view?.findViewById<PreviewView>(R.id.previewView)
                preview.setSurfaceProvider(previewView?.surfaceProvider)
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) {
                // ignore
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { cameraExecutor.shutdown() } catch (_: Exception) {}
    }
}
