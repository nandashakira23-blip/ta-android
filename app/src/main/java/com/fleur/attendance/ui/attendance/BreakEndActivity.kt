package com.fleur.attendance.ui.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fleur.attendance.R
import com.fleur.attendance.data.repository.AttendanceRepository
import com.fleur.attendance.databinding.ActivityBreakEndBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * "Selesai Istirahat" — kini dengan Verifikasi Wajah + Lokasi (sesuai flowchart).
 * Mengambil beberapa frame wajah dari kamera depan + lokasi terkini, lalu mengirim ke API break/end.
 * Server memvalidasi wajah & lokasi; jika gagal, pengguna bisa mengulang (tombol aktif kembali).
 */
class BreakEndActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBreakEndBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreakEndBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val elapsed = intent.getIntExtra("elapsed", 0)
        binding.tvDuration.text = "Durasi: $elapsed menit"

        binding.tvBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener { onConfirm() }
        setupBottomNav()

        if (allPermissionsGranted()) {
            startCamera()
            setupLocationServices()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                setupLocationServices()
            } else {
                Toast.makeText(this, "Izin kamera & lokasi diperlukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("BreakEnd", "Camera bind failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.tvLocationStatus.text = "Izin lokasi diperlukan"
            return
        }
        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                setLocationReady()
            } else {
                requestFreshLocation()
            }
        }?.addOnFailureListener {
            binding.tvLocationStatus.text = "Gagal mendapatkan lokasi"
        }
    }

    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }
        val cb = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let {
                    currentLocation = it
                    setLocationReady()
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(locationRequest, cb, Looper.getMainLooper())
    }

    private fun setLocationReady() {
        binding.tvLocationStatus.text = "Lokasi siap"
        binding.tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green))
    }

    private fun onConfirm() {
        val location = currentLocation ?: run {
            Toast.makeText(this, "Lokasi belum tersedia, mohon tunggu", Toast.LENGTH_SHORT).show()
            getCurrentLocation()
            return
        }
        binding.btnConfirm.isEnabled = false
        binding.loadingOverlay.visibility = android.view.View.VISIBLE
        com.fleur.attendance.utils.LoadingOverlay.show(this, "Memverifikasi wajah...")
        binding.tvLocationStatus.text = "Memverifikasi wajah..."
        captureFrames(mutableListOf(), 3) { frames ->
            if (frames.isEmpty()) {
                binding.loadingOverlay.visibility = android.view.View.GONE
                com.fleur.attendance.utils.LoadingOverlay.hide(this)
                binding.btnConfirm.isEnabled = true
                Toast.makeText(this, "Gagal mengambil foto, coba lagi", Toast.LENGTH_SHORT).show()
                return@captureFrames
            }
            endBreak(location.latitude, location.longitude, frames)
        }
    }

    // Ambil beberapa frame berurutan; server mencocokkan semua & memilih yang terbaik (multi-frame).
    private fun captureFrames(frames: MutableList<File>, target: Int, onDone: (List<File>) -> Unit) {
        val capture = imageCapture ?: run { onDone(frames); return }
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(getExternalFilesDir(null), "break_${name}_${frames.size}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("BreakEnd", "Capture failed: ${exception.message}", exception)
                    onDone(frames)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    frames.add(photoFile)
                    if (frames.size >= target) onDone(frames)
                    else captureFrames(frames, target, onDone)
                }
            }
        )
    }

    private fun endBreak(lat: Double, lng: Double, photoFiles: List<File>) {
        fun cleanup() {
            photoFiles.forEach { f -> try { if (f.exists()) f.delete() } catch (e: Exception) { } }
        }
        AttendanceRepository(this).endBreak(
            latitude = lat,
            longitude = lng,
            facePhotoFiles = photoFiles,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                    cleanup()
                    finish()
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.loadingOverlay.visibility = android.view.View.GONE
                    com.fleur.attendance.utils.LoadingOverlay.hide(this)
                    binding.btnConfirm.isEnabled = true
                    binding.tvLocationStatus.text = "Verifikasi gagal, coba lagi"
                    binding.tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow))
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    cleanup()
                }
            }
        )
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { goHome(); true }
                R.id.nav_attendance -> {
                    startActivity(Intent(this, AttendanceHistoryActivity::class.java)); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, com.fleur.attendance.ui.profile.ProfileActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    private fun goHome() {
        val i = Intent(this, com.fleur.attendance.ui.main.MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(i)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }
}
