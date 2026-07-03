package com.fleur.attendance.ui.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fleur.attendance.R
import com.fleur.attendance.databinding.ActivityClockOutBinding
import com.fleur.attendance.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClockOutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClockOutBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null
    private val timeHandler = Handler(Looper.getMainLooper())
    private val locationHandler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable
    private lateinit var locationRunnable: Runnable
    private var faceDetected = false
    private var locationRepository: com.fleur.attendance.data.repository.LocationRepository? = null
    
    // Office location from API
    private var officeLatitude: Double? = null
    private var officeLongitude: Double? = null
    private var officeRadius: Double = 100.0
    
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
        binding = ActivityClockOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        locationRepository = com.fleur.attendance.data.repository.LocationRepository(this)
        
        setupUI()
        setupClickListeners()
        loadAttendanceStatus()
        fetchOfficeLocation()
        
        // Request permissions
        if (allPermissionsGranted()) {
            startCamera()
            setupLocationServices()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        startTimeUpdater()
    }
    
    private fun setupUI() {
        updateCurrentTime()
    }
    
    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Circle sebagai tombol clock out
        binding.btnClockOutCircle.setOnClickListener {
            performClockOut()
        }
    }
    
    private fun fetchOfficeLocation() {
        locationRepository?.getOfficeLocation(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    officeLatitude = response.data.latitude
                    officeLongitude = response.data.longitude
                    officeRadius = response.data.radiusMeters
                    
                    Log.d("ClockOut", "Office location loaded: lat=$officeLatitude, lng=$officeLongitude, radius=$officeRadius")
                    
                    // Refresh location check with new office location
                    currentLocation?.let { checkLocationRadius(it) }
                }
            },
            onError = { error ->
                Log.e("ClockOut", "Failed to fetch office location: $error")
                Toast.makeText(this, "Gagal memuat lokasi kantor: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun loadAttendanceStatus() {
        val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)
        
        apiAdapter.getAttendanceStatus(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    updateWorkSummary(response.data)
                }
            },
            onError = { _ ->
                // Silently fail
            }
        )
    }
    
    private fun updateWorkSummary(status: com.fleur.attendance.data.model.AttendanceStatus) {
        val checkInTime = status.checkIn?.time
        binding.tvClockInTime.text = formatTimeFromISO(checkInTime)
        
        // Don't show work duration here - only show in home screen
        // status.workDuration?.let { duration ->
        //     binding.tvWorkDuration.text = duration
        //     
        //     // Check for overtime (assuming 8 hours is standard)
        //     if (duration.contains("9h") || duration.contains("10h") || duration.contains("11h") || duration.contains("12h")) {
        //         binding.overtimeSection.visibility = View.VISIBLE
        //         // Calculate overtime (simplified)
        //         val hours = duration.substringBefore("h").toIntOrNull() ?: 0
        //         if (hours > 8) {
        //             val overtime = hours - 8
        //             binding.tvOvertime.text = "${overtime}h 0m"
        //         }
        //     }
        // }
    }
    
    private fun formatTimeFromISO(isoTime: String?): String {
        if (isoTime == null) return "--:--"
        
        return try {
            // Handle multiple formats:
            // 1. "2026-01-16T16:58:22" (correct format)
            // 2. "Fri Jan 16 2026 00:00:00 GMT+0700 (...)T16:58:22" (buggy format)
            
            val parts = isoTime.split("T")
            val timePart = parts.lastOrNull() ?: ""
            
            if (timePart.length >= 5) {
                timePart.substring(0, 5) // "16:58:22" -> "16:58"
            } else {
                "--:--"
            }
        } catch (e: Exception) {
            Log.e("ClockOut", "Error formatting time: $isoTime", e)
            "--:--"
        }
    }
    
    private fun startTimeUpdater() {
        timeRunnable = object : Runnable {
            override fun run() {
                updateCurrentTime()
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }
    
    private fun updateCurrentTime() {
        val witaTimeZone = java.util.TimeZone.getTimeZone("Asia/Makassar")
        val dateFormat = SimpleDateFormat("HH mm", Locale.getDefault())
        dateFormat.timeZone = witaTimeZone
        val currentTime = dateFormat.format(Date())
        binding.tvCurrentTime.text = currentTime
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
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
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
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("ClockOut", "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        startLocationPolling()
    }
    
    private fun startLocationPolling() {
        locationRunnable = object : Runnable {
            override fun run() {
                validateLocationWithAPI()
                locationHandler.postDelayed(this, 2000) // Poll every 2 seconds
            }
        }
        locationHandler.post(locationRunnable)
    }
    
    private fun validateLocationWithAPI() {
        val location = currentLocation ?: run {
            getCurrentLocation()
            return
        }
        
        val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)
        
        apiAdapter.validateLocation(
            location.latitude,
            location.longitude,
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    val data = response.data
                    val distance = data.distance.toInt()
                    val isValid = data.isValid
                    
                    runOnUiThread {
                        // Update location status
                        val statusText = if (isValid) {
                            "Onsite"
                        } else {
                            "Outside punch area (${distance}m)"
                        }
                        
                        // Only update if not already showing location status
                        binding.tvLocationStatus.text = statusText
                        binding.tvLocationStatus.setTextColor(
                            ContextCompat.getColor(
                                this,
                                if (isValid) R.color.success_green else R.color.warning_yellow
                            )
                        )
                        
                        // Update button state
                        binding.btnClockOutCircle.alpha = if (isValid) 1.0f else 0.7f
                    }
                }
            },
            onError = { error ->
                Log.e("ClockOut", "Location validation error: $error")
            }
        )
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            binding.tvLocationStatus.text = "Izin lokasi diperlukan"
            binding.tvLocationDetails.text = "Mohon izinkan akses lokasi"
            return
        }
        
        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                updateLocationUI(location)
                checkLocationRadius(location)
            } else {
                // Try to request fresh location
                binding.tvLocationStatus.text = "Mendapatkan lokasi..."
                binding.tvLocationDetails.text = "Mohon tunggu..."
                
                // Request fresh location update
                requestFreshLocation()
            }
        }?.addOnFailureListener {
            binding.tvLocationStatus.text = "Error mendapatkan lokasi"
            binding.tvLocationDetails.text = "Periksa pengaturan GPS"
        }
    }
    
    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }
        
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocationUI(location)
                    checkLocationRadius(location)
                }
            }
        }
        
        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
    
    private fun updateLocationUI(location: Location) {
        binding.tvLocationDetails.text = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
    }
    
    private fun checkLocationRadius(location: Location) {
        // Check if office location is loaded
        if (officeLatitude == null || officeLongitude == null) {
            binding.tvLocationStatus.text = "Loading office location..."
            binding.tvLocationStatus.setTextColor(
                ContextCompat.getColor(this, R.color.warning_yellow)
            )
            binding.btnClockOutCircle.isEnabled = false
            binding.btnClockOutCircle.alpha = 0.5f
            return
        }
        
        val officeLocation = Location("office").apply {
            latitude = officeLatitude!!
            longitude = officeLongitude!!
        }
        
        val distance = location.distanceTo(officeLocation)
        val isInRadius = distance <= officeRadius
        
        binding.tvLocationStatus.text = if (isInRadius) {
            "Already within punch area"
        } else {
            "Outside punch area (${distance.toInt()}m)"
        }
        
        binding.tvLocationStatus.setTextColor(
            ContextCompat.getColor(this, if (isInRadius) R.color.success_green else R.color.warning_yellow)
        )
        
        // Enable clock out circle
        binding.btnClockOutCircle.isEnabled = true
        binding.btnClockOutCircle.alpha = if (isInRadius) 1.0f else 0.7f
    }
    
    private fun performClockOut() {
        val location = currentLocation ?: run {
            showError(getString(R.string.location_not_available))
            return
        }
        
        // Check if office location is loaded
        if (officeLatitude == null || officeLongitude == null) {
            showError("Lokasi kantor belum dimuat. Silakan tunggu...")
            return
        }
        
        // Check location before proceeding
        val officeLocation = android.location.Location("office").apply {
            latitude = officeLatitude!!
            longitude = officeLongitude!!
        }
        val distance = location.distanceTo(officeLocation)
        val isInRadius = distance <= officeRadius
        
        if (!isInRadius) {
            // Show explanation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lokasi Di Luar Jangkauan")
                .setMessage("Anda berada ${distance.toInt()}m dari kantor. Absensi hanya bisa dilakukan dalam radius ${officeRadius.toInt()}m dari kantor.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }
        
        // Capture multiple frames; the server matches all and keeps the best (multi-frame).
        captureFramesForClockOut { photoFiles ->
            uploadClockOut(photoFiles, location)
        }
    }

    private fun captureFramesForClockOut(onPhotosTaken: (List<File>) -> Unit) {
        captureFrame(mutableListOf(), 3, onPhotosTaken)
    }

    // Capture frames sequentially; each takePicture gives a slightly different moment.
    private fun captureFrame(frames: MutableList<File>, target: Int, onDone: (List<File>) -> Unit) {
        val imageCapture = imageCapture ?: run {
            if (frames.isNotEmpty()) onDone(frames) else showError("Kamera belum siap")
            return
        }
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(getExternalFilesDir(null), "${name}_${frames.size}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("ClockOut", "Photo capture failed: ${exception.message}", exception)
                    if (frames.isNotEmpty()) onDone(frames)
                    else showError("Gagal mengambil foto: ${exception.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    frames.add(photoFile)
                    if (frames.size >= target) {
                        Log.d("ClockOut", "Captured ${frames.size} frame(s)")
                        onDone(frames)
                    } else {
                        captureFrame(frames, target, onDone)
                    }
                }
            }
        )
    }
    
    private fun uploadClockOut(photoFiles: List<File>, location: Location) {
        showLoading(true)

        fun cleanupFrames() {
            photoFiles.forEach { f ->
                try { if (f.exists()) f.delete() } catch (e: Exception) { Log.e("ClockOut", "Error deleting frame", e) }
            }
        }

        val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)

        apiAdapter.clockOut(location.latitude, location.longitude, photoFiles,
            onSuccess = { response ->
                showLoading(false)

                if (response.success && response.data != null) {
                    showClockOutSuccess(response.data)
                } else {
                    showError(response.message)
                }
                cleanupFrames()
            },
            onError = { error ->
                showLoading(false)
                showError(error)
                cleanupFrames()
            }
        )
    }
    
    private fun showClockOutSuccess(result: com.fleur.attendance.data.model.ClockOutData) {
        val message = buildString {
            append(getString(R.string.clock_out_success))
            append("\n\n")
            append("Status: ${result.status}")
            append("\n")
            append("Distance: ${result.location.distance.toInt()}m")
            append("\n")
            append("Face Match: ${if (result.faceMatch.isMatch) "Ya" else "Tidak"}")
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.success))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) com.fleur.attendance.utils.LoadingOverlay.show(this, "Memproses absen...")
        else com.fleur.attendance.utils.LoadingOverlay.hide(this)
        binding.btnClockOutCircle.isEnabled = !show
        
        // Show/hide progress bar in button
        binding.btnProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnContent.visibility = if (show) View.GONE else View.VISIBLE
        
        // Add loading animation to button
        if (show) {
            binding.btnClockOutCircle.alpha = 0.8f
            // Animate button scale
            binding.btnClockOutCircle.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .start()
        } else {
            binding.btnClockOutCircle.alpha = 1.0f
            binding.btnClockOutCircle.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
        locationHandler.removeCallbacks(locationRunnable)
        cameraExecutor.shutdown()
    }
}