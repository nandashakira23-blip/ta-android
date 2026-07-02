package com.fleur.attendance.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fleur.attendance.R
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityFaceEnrollmentBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceEnrollmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceEnrollmentBinding
    private lateinit var apiAdapter: LegacyApiAdapter
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var nik: String
    private lateinit var employeeName: String
    private lateinit var email: String
    private lateinit var pin: String
    private var isReactivation: Boolean = false
    private val capturedPhotoPaths = mutableListOf<String>()
    private val requiredReferencePhotos = 15  // burst target: many multi-pose frames captured in one tap
    private var handoffToNextStep = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceEnrollmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiAdapter = LegacyApiAdapter(this)
        nik = intent.getStringExtra("nik") ?: ""
        employeeName = intent.getStringExtra("employeeName") ?: "Karyawan"
        email = intent.getStringExtra("email") ?: ""
        pin = intent.getStringExtra("pin") ?: ""
        isReactivation = intent.getBooleanExtra("isReactivation", false)

        setupUI()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCapture.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupUI() {
        binding.tvEmployeeName.text = employeeName

        if (isReactivation) {
            binding.tvFaceEnrollmentTitle.text = getString(R.string.face_reenrollment_title)
            binding.tvFaceEnrollmentInstruction.text = getString(R.string.face_reenrollment_instruction)
        } else {
            binding.tvFaceEnrollmentTitle.text = getString(R.string.face_enrollment_title)
            binding.tvFaceEnrollmentInstruction.text = getString(R.string.face_enrollment_instruction)
        }

        updateCaptureProgress()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("FaceEnrollment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // One tap captures a BURST of frames while the user slowly rotates their head
    // (front -> right -> left -> up -> down). Many multi-pose references => higher accuracy.
    private fun takePhoto() {
        binding.btnCapture.isEnabled = false
        binding.tvFaceEnrollmentInstruction.text =
            "Tahan & putar wajah perlahan: depan → kanan → kiri → atas → bawah"
        captureBurstFrame()
    }

    private fun captureBurstFrame() {
        val capture = imageCapture ?: run {
            binding.btnCapture.isEnabled = true
            showError("Kamera belum siap")
            return
        }
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(filesDir, "${name}_${capturedPhotoPaths.size}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("FaceEnrollment", "Burst capture failed: ${exception.message}", exception)
                    // Tolerate occasional frame errors: proceed if we already have enough, else stop.
                    if (capturedPhotoPaths.size >= 3) {
                        activateAccount()
                    } else {
                        binding.btnCapture.isEnabled = true
                        showError("Gagal mengambil foto: ${exception.message}")
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedPhotoPaths.add(photoFile.absolutePath)
                    updateCaptureProgress()
                    if (capturedPhotoPaths.size >= requiredReferencePhotos) {
                        activateAccount()
                    } else {
                        captureBurstFrame()
                    }
                }
            }
        )
    }

    private fun activateAccount() {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            binding.btnCapture.isEnabled = true
            showError("PIN tidak valid. Silakan ulang dari langkah PIN.")
            return
        }

        if (capturedPhotoPaths.size < 3) {
            binding.btnCapture.isEnabled = true
            showError("Foto wajah kurang. Ulangi dan putar wajah perlahan.")
            return
        }
        val files = capturedPhotoPaths.map { File(it) }
        if (files.any { !it.exists() || !it.canRead() }) {
            binding.btnCapture.isEnabled = true
            showError("Sebagian foto tidak bisa dibaca. Silakan ulang.")
            return
        }

        showLoading(true)
        val photoFiles = files
        apiAdapter.activate(
            nik = nik,
            pin = pin,
            email = null,
            facePhotoFiles = photoFiles,
            onSuccess = { response ->
                runOnUiThread {
                    showLoading(false)
                    binding.btnCapture.isEnabled = true
                    photoFiles.forEach { it.delete() }

                    if (response.success) {
                        // Remember (device-level) that activation happened -> next launch starts at Login.
                        com.fleur.attendance.utils.SessionManager(this).setEverActivated()
                        Toast.makeText(
                            this,
                            "Aktivasi berhasil. Silakan login dengan NIK dan PIN Anda.",
                            Toast.LENGTH_LONG
                        ).show()
                        handoffToNextStep = true
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        showError(response.message)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    binding.btnCapture.isEnabled = true
                    showError(error)
                }
            }
        )
    }

    private fun updateCaptureProgress() {
        binding.tvStatus.text = getString(
            R.string.face_enrollment_progress,
            capturedPhotoPaths.size,
            requiredReferencePhotos
        )
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCapture.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!handoffToNextStep) {
            capturedPhotoPaths.forEach {
                try {
                    File(it).delete()
                } catch (_: Exception) {
                }
            }
        }
        cameraExecutor.shutdown()
    }
}
