package com.fleur.attendance.ui.main

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fleur.attendance.R
import com.fleur.attendance.databinding.ActivityMainBinding
import com.fleur.attendance.ui.auth.LoginActivity
import com.fleur.attendance.ui.attendance.ClockInActivity
import com.fleur.attendance.ui.attendance.ClockOutActivity
import com.fleur.attendance.utils.SessionManager
import com.fleur.attendance.data.model.AttendanceStatus
import com.fleur.attendance.data.model.AttendanceStatusResponse
import com.fleur.attendance.data.model.BreakInfo
import com.fleur.attendance.data.model.WorkScheduleInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    
    // UI Components
    private var binding: ActivityMainBinding? = null
    
    // Managers
    private var sessionManager: SessionManager? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    
    // Location
    private var currentLocation: Location? = null
    private val locationHandler = Handler(Looper.getMainLooper())
    private lateinit var locationRunnable: Runnable
    
    // Attendance State
    private var hasCheckedInToday = false
    private var hasCheckedOutToday = false
    private var canClockInNow = false
    private var canClockOutNow = false
    private var clockInDisabledReason: String? = null
    private var clockOutDisabledReason: String? = null
    private var canStartBreakNow = false
    private var canEndBreakNow = false
    private var lastBreakInfo: BreakInfo? = null
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "=== STARTING MAIN ACTIVITY ===")
        
        initializeComponents()
        checkLoginStatus()
        setupUI()
        loadData()
        
        Log.d("MainActivity", "=== MAIN ACTIVITY INITIALIZED ===")
    }
    
    // ========== INITIALIZATION ==========
    
    private fun initializeComponents() {
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
            sessionManager = SessionManager(this)
            Log.d("MainActivity", "Components initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing components: ${e.message}")
            // Fallback - try to continue without crashing
            try {
                sessionManager = SessionManager(this)
            } catch (ex: Exception) {
                Log.e("MainActivity", "Critical error - cannot initialize SessionManager: ${ex.message}")
            }
        }
    }
    
    private fun checkLoginStatus() {
        val isLoggedIn = sessionManager?.isLoggedIn() ?: false
        if (!isLoggedIn) {
            redirectToLogin()
            return
        }
    }
    
    private fun setupUI() {
        setupBasicUI()
        setupClickListeners()
        setupSwipeRefresh()
        checkAndRequestPermissions()
    }
    
    private fun loadData() {
        setupLocationServices()
        loadAttendanceData()
    }
    
    // ========== UI SETUP ==========
    
    private fun setupBasicUI() {
        val userName = sessionManager?.getEmployeeName() ?: "Karyawan"
        
        binding?.tvUserName?.text = userName
        binding?.tvWelcome?.text = "Selamat datang,"
        binding?.tvAttendanceStatus?.text = "Memuat..."
        binding?.tvLocationStatus?.text = "Checking location..."
        
        loadProfilePicture()
        Log.d("MainActivity", "Basic UI setup completed for: $userName")
    }
    
    private fun setupClickListeners() {
        // Clock In/Out Button
        binding?.btnClockIn?.setOnClickListener {
            handleClockButtonClick()
        }

        binding?.btnBreakAction?.setOnClickListener {
            handleBreakButtonClick()
        }
        
        // Profile Photo
        binding?.ivProfilePhoto?.setOnClickListener {
            openProfile()
        }
        
        // Bottom Navigation
        binding?.bottomNavigation?.setOnItemSelectedListener { item ->
            handleBottomNavigation(item.itemId)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding?.swipeRefresh?.setOnRefreshListener {
            refreshAllData()
        }
        
        binding?.swipeRefresh?.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        
        binding?.scrollView?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding?.swipeRefresh?.isEnabled = scrollY == 0
        }
    }
    
    // ========== CLICK HANDLERS ==========
    
    private fun handleClockButtonClick() {
        when {
            hasCheckedInToday && !hasCheckedOutToday -> {
                if (canClockOutNow) {
                    startActivity(Intent(this, ClockOutActivity::class.java))
                } else {
                    showToast(clockOutDisabledReason ?: "Belum waktunya clock out")
                }
            }
            !hasCheckedInToday -> {
                if (canClockInNow) {
                    startActivity(Intent(this, ClockInActivity::class.java))
                } else {
                    showToast(clockInDisabledReason ?: "Belum waktunya clock in")
                }
            }
            else -> {
                showToast("Presensi hari ini sudah selesai")
            }
        }
    }

    private fun handleBreakButtonClick() {
        when {
            canStartBreakNow -> openBreakStart()
            canEndBreakNow -> openBreakEnd()
            else -> showToast("Fitur istirahat belum tersedia untuk kondisi presensi saat ini")
        }
    }

    private fun openBreakStart() {
        val intent = Intent(this, com.fleur.attendance.ui.attendance.BreakStartActivity::class.java)
        intent.putExtra("allowance", lastBreakInfo?.breakAllowanceMinutes ?: 60)
        startActivity(intent)
    }

    private fun openBreakEnd() {
        val location = currentLocation
        if (location == null) {
            getCurrentLocation()
            showToast("Menunggu lokasi GPS untuk menyelesaikan istirahat")
            return
        }
        val intent = Intent(this, com.fleur.attendance.ui.attendance.BreakEndActivity::class.java)
        intent.putExtra("elapsed", lastBreakInfo?.activeDurationMinutes ?: lastBreakInfo?.runningTotalMinutes ?: 0)
        intent.putExtra("lat", location.latitude)
        intent.putExtra("lng", location.longitude)
        startActivity(intent)
    }

    private fun openProfile() {
        val intent = Intent(this, com.fleur.attendance.ui.profile.ProfileActivity::class.java)
        startActivity(intent)
    }
    
    private fun handleBottomNavigation(itemId: Int): Boolean {
        return when (itemId) {
            R.id.nav_home -> true
            R.id.nav_attendance -> {
                val intent = Intent(this, com.fleur.attendance.ui.attendance.AttendanceHistoryActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_profile -> {
                openProfile()
                true
            }
            else -> false
        }
    }
    
    // ========== DATA LOADING ==========
    
    private fun loadAttendanceData() {
        resetUIState()
        loadAttendanceFromAPI()
    }
    
    private fun resetUIState() {
        binding?.tvClockInTime?.text = "-"
        binding?.tvClockOutTime?.text = "-"
        binding?.tvWorkDuration?.text = "--:--"
        binding?.btnClockIn?.isEnabled = false
        binding?.btnClockIn?.alpha = 0.5f
        binding?.tvClockInfo?.text = "Memuat jadwal..."
        binding?.tvClockInfo?.visibility = android.view.View.VISIBLE
        binding?.cardBreakSection?.visibility = android.view.View.GONE
        canStartBreakNow = false
        canEndBreakNow = false
    }
    
    private fun loadAttendanceFromAPI() {
        try {
            Thread {
                try {
                    val attendanceRepository = com.fleur.attendance.data.repository.AttendanceRepository(this)
                    val employeeId = sessionManager?.getEmployeeId() ?: 0
                    
                    if (employeeId == 0) {
                        runOnUiThread { showNoScheduleState() }
                        return@Thread
                    }
                    
                    attendanceRepository.getAttendanceStatus(
                        employeeId = employeeId,
                        onSuccess = { response ->
                            try {
                                runOnUiThread {
                                    if (response.success && response.data != null) {
                                        handleAttendanceData(response.data)
                                    } else {
                                        showNoScheduleState()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error handling attendance data: ${e.message}")
                                runOnUiThread { showNoScheduleState() }
                            }
                        },
                        onError = { error ->
                            Log.e("MainActivity", "API Error: $error")
                            runOnUiThread { showNoScheduleState() }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in loadAttendanceFromAPI thread: ${e.message}")
                    runOnUiThread { showNoScheduleState() }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting loadAttendanceFromAPI thread: ${e.message}")
            showNoScheduleState()
        }
    }
    
    private fun handleAttendanceData(data: AttendanceStatus) {
        updateAttendanceTimes(data)
        updateAttendanceState(data)
        
        if (data.hasCheckedIn && data.hasCheckedOut) {
            // Update work schedule card first, then show completed state
            if (data.workSchedule != null) {
                updateWorkScheduleCard(data.workSchedule)
            }
            showCompletedState()
        } else if (data.workSchedule != null) {
            showWorkScheduleState(data.workSchedule)
            updateButtonState(data)
        } else {
            showNoScheduleState()
        }

        updateBreakCard(data.breakInfo, data.hasCheckedIn, data.hasCheckedOut)
    }
    
    private fun updateAttendanceTimes(data: AttendanceStatus) {
        // Update Clock In Time
        data.checkIn?.let { checkIn ->
            val time = parseTimeFromDateTime(checkIn.time)
            binding?.tvClockInTime?.text = time
        }
        
        // Update Clock Out Time
        data.checkOut?.let { checkOut ->
            val time = parseTimeFromDateTime(checkOut.time)
            binding?.tvClockOutTime?.text = time
        }
        
        // Update Work Duration
        binding?.tvWorkDuration?.text = data.workDuration ?: "--:--"
    }
    
    private fun updateAttendanceState(data: AttendanceStatus) {
        hasCheckedInToday = data.hasCheckedIn
        hasCheckedOutToday = data.hasCheckedOut
        canClockInNow = data.canCheckIn
        canClockOutNow = data.canCheckOut
        
        val statusText = when {
            data.workSchedule == null -> "Tidak ada jadwal"
            data.hasCheckedIn -> "Sudah clock in"
            else -> "Belum absen"
        }
        binding?.tvAttendanceStatus?.text = statusText
    }
    
    private fun updateButtonState(data: AttendanceStatus) {
        val workSchedule = data.workSchedule ?: return
        
        when {
            hasCheckedInToday && !hasCheckedOutToday -> {
                setupClockOutButton(canClockOutNow, workSchedule)
            }
            !hasCheckedInToday -> {
                setupClockInButton(canClockInNow, workSchedule)
            }
            else -> {
                setupCompletedButton()
            }
        }
        
        updateDisabledReasons(data, workSchedule)
    }
    
    // ========== UI STATES ==========
    
    private fun showWorkScheduleState(workSchedule: WorkScheduleInfo) {
        if (!isWorkDay(workSchedule)) {
            showHolidayState(workSchedule)
            return
        }
        
        // Show all components
        binding?.circularButtonContainer?.visibility = android.view.View.VISIBLE
        binding?.bottomStatsSection?.visibility = android.view.View.VISIBLE
        binding?.cardWorkSchedule?.visibility = android.view.View.VISIBLE
        binding?.restMessageSection?.visibility = android.view.View.GONE
        
        updateWorkScheduleCard(workSchedule)
    }
    
    private fun showNoScheduleState() {
        hideAllComponents()
        
        binding?.restMessageSection?.visibility = android.view.View.VISIBLE
        binding?.tvRestEmoji?.visibility = android.view.View.VISIBLE
        binding?.tvRestEmoji?.text = "😴"
        binding?.tvRestTitle?.text = "Selamat Beristirahat"
        binding?.tvRestMessage?.text = "Tidak ada jadwal kerja untuk hari ini"
        
        Log.d("MainActivity", "No schedule state displayed")
    }
    
    private fun showHolidayState(workSchedule: WorkScheduleInfo) {
        hideAllComponents()
        
        binding?.restMessageSection?.visibility = android.view.View.VISIBLE
        binding?.tvRestEmoji?.visibility = android.view.View.VISIBLE
        binding?.tvRestEmoji?.text = "😴"
        binding?.tvRestTitle?.text = "Selamat Beristirahat"
        binding?.tvRestMessage?.text = "Hari ini adalah hari libur Anda. Nikmati waktu istirahat!"
        binding?.tvAttendanceStatus?.text = "Hari Libur"
        
        Log.d("MainActivity", "Holiday state displayed")
    }
    
    private fun showCompletedState() {
        binding?.circularButtonContainer?.visibility = android.view.View.VISIBLE
        binding?.bottomStatsSection?.visibility = android.view.View.VISIBLE
        binding?.cardWorkSchedule?.visibility = android.view.View.VISIBLE
        binding?.restMessageSection?.visibility = android.view.View.VISIBLE
        
        binding?.tvRestEmoji?.visibility = android.view.View.GONE
        binding?.tvRestTitle?.text = "Absensi Selesai"
        binding?.tvRestMessage?.text = "Selamat beristirahat! Anda sudah menyelesaikan absensi hari ini."
        
        // Hide the loading text and setup completed button
        binding?.tvClockInfo?.visibility = android.view.View.GONE
        setupCompletedButton()
        
        Log.d("MainActivity", "Completed state displayed")
    }
    
    private fun hideAllComponents() {
        binding?.cardWorkSchedule?.visibility = android.view.View.GONE
        binding?.circularButtonContainer?.visibility = android.view.View.GONE
        binding?.bottomStatsSection?.visibility = android.view.View.GONE
        hideBreakCard(animated = false)
    }

    private fun updateBreakCard(breakInfo: BreakInfo?, hasCheckedIn: Boolean, hasCheckedOut: Boolean) {
        lastBreakInfo = breakInfo
        val isBreakFinished = breakInfo?.status == "selesai" && breakInfo.isOnBreak.not()
        val shouldShow = hasCheckedIn && !hasCheckedOut && !isBreakFinished
        if (!shouldShow) {
            hideBreakCard(animated = isBreakFinished)
            return
        }
        showBreakCard()

        canStartBreakNow = breakInfo?.canStartBreak ?: true
        canEndBreakNow = breakInfo?.canEndBreak ?: false

        val totalMinutes = breakInfo?.runningTotalMinutes?.takeIf { it > 0 } ?: (breakInfo?.totalMenit ?: 0)
        val allowanceMinutes = breakInfo?.breakAllowanceMinutes ?: 60
        val countedMinutes = breakInfo?.countedBreakMinutes?.takeIf { it > 0 } ?: 0
        val overageMinutes = breakInfo?.breakOverageMinutes ?: 0
        val overageText = if (overageMinutes > 0) " (lebih ${formatMinutes(overageMinutes)})" else ""
        val countedText = if (countedMinutes > 0) " / Dihitung: ${formatMinutes(countedMinutes)}" else ""
        binding?.tvBreakDuration?.text = "Total: ${formatMinutes(totalMinutes)} / Jatah: ${formatMinutes(allowanceMinutes)}$countedText$overageText"

        when {
            canEndBreakNow -> {
                val start = breakInfo?.activeStartedAt?.let { formatDateTimeForDisplay(it) } ?: "-"
                binding?.tvBreakStatus?.text = "Istirahat berlangsung sejak $start"
                binding?.btnBreakAction?.text = "Selesai Istirahat"
                binding?.btnBreakAction?.isEnabled = true
            }
            canStartBreakNow -> {
                binding?.tvBreakStatus?.text = "Tidak sedang istirahat"
                binding?.btnBreakAction?.text = "Mulai Istirahat"
                binding?.btnBreakAction?.isEnabled = true
            }
            else -> {
                binding?.tvBreakStatus?.text = "Istirahat tidak tersedia"
                binding?.btnBreakAction?.text = "Istirahat"
                binding?.btnBreakAction?.isEnabled = false
            }
        }
    }

    private fun showBreakCard() {
        val card = binding?.cardBreakSection ?: return
        card.animate().cancel()
        if (card.visibility != View.VISIBLE) {
            card.alpha = 0f
            card.scaleY = 0.96f
            card.translationY = -12f
            card.visibility = View.VISIBLE
        }

        card.animate()
            .alpha(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(220L)
            .start()
    }

    private fun hideBreakCard(animated: Boolean) {
        val card = binding?.cardBreakSection ?: return
        canStartBreakNow = false
        canEndBreakNow = false
        card.animate().cancel()

        if (card.visibility != View.VISIBLE) {
            card.alpha = 1f
            card.scaleY = 1f
            card.translationY = 0f
            return
        }

        if (!animated) {
            card.visibility = View.GONE
            card.alpha = 1f
            card.scaleY = 1f
            card.translationY = 0f
            return
        }

        card.animate()
            .alpha(0f)
            .scaleY(0.96f)
            .translationY(-12f)
            .setDuration(240L)
            .withEndAction {
                card.visibility = View.GONE
                card.alpha = 1f
                card.scaleY = 1f
                card.translationY = 0f
            }
            .start()
    }
    
    // ========== BUTTON SETUP ==========
    
    private fun setupClockInButton(canClockIn: Boolean, workSchedule: WorkScheduleInfo) {
        binding?.tvClockInLabel?.text = "Clock In"
        binding?.btnClockIn?.isEnabled = canClockIn
        binding?.btnClockIn?.alpha = if (canClockIn) 1.0f else 0.5f
        
        if (!canClockIn) {
            val reason = "Belum waktunya clock in. Waktu presensi masuk: ${formatTime(workSchedule.clockInStart)} - ${formatTime(workSchedule.clockInEnd)}"
            binding?.tvClockInfo?.text = reason
            binding?.tvClockInfo?.visibility = android.view.View.VISIBLE
        } else {
            binding?.tvClockInfo?.visibility = android.view.View.GONE
        }
    }
    
    private fun setupClockOutButton(canClockOut: Boolean, workSchedule: WorkScheduleInfo) {
        binding?.tvClockInLabel?.text = "Clock Out"
        binding?.btnClockIn?.isEnabled = canClockOut
        binding?.btnClockIn?.alpha = if (canClockOut) 1.0f else 0.5f
        
        if (!canClockOut) {
            val reason = "Belum waktunya clock out. Waktu presensi pulang: ${formatTime(workSchedule.clockOutStart)} - ${formatTime(workSchedule.clockOutEnd)}"
            binding?.tvClockInfo?.text = reason
            binding?.tvClockInfo?.visibility = android.view.View.VISIBLE
        } else {
            binding?.tvClockInfo?.visibility = android.view.View.GONE
        }
    }
    
    private fun setupCompletedButton() {
        binding?.tvClockInLabel?.text = "Selesai"
        binding?.btnClockIn?.isEnabled = false
        binding?.btnClockIn?.alpha = 0.5f
        binding?.tvClockInfo?.visibility = android.view.View.GONE
    }
    
    // ========== WORK SCHEDULE CARD ==========
    
    private fun updateWorkScheduleCard(workSchedule: WorkScheduleInfo) {
        binding?.tvScheduleName?.text = workSchedule.nama ?: "Jadwal Kerja"
        binding?.tvStartTime?.text = formatTime(workSchedule.jamMasuk)
        binding?.tvEndTime?.text = formatTime(workSchedule.jamKeluar)
        
        updateWorkDaysChips(workSchedule.hariKerja)
    }
    
    private fun updateWorkDaysChips(workDays: List<String>?) {
        binding?.chipGroupWorkDays?.removeAllViews()
        
        workDays?.forEach { day ->
            val dayIndo = translateDayToIndonesian(day)
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = dayIndo
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(android.R.color.transparent)
                setChipStrokeColorResource(R.color.primary_brown)
                setChipStrokeWidth(2f)
                setTextColor(getColor(R.color.primary_brown))
                textSize = 11f
            }
            binding?.chipGroupWorkDays?.addView(chip)
        }
    }
    
    // ========== LOCATION SERVICES ==========
    
    private fun setupLocationServices() {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                getCurrentLocation()
                startLocationPolling()
            } else {
                Log.d("MainActivity", "Location permission not granted")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up location services: ${e.message}")
        }
    }
    
    private fun startLocationPolling() {
        locationRunnable = object : Runnable {
            override fun run() {
                validateLocationWithAPI()
                locationHandler.postDelayed(this, 5000)
            }
        }
        locationHandler.post(locationRunnable)
    }
    
    private fun validateLocationWithAPI() {
        val location = currentLocation ?: run {
            binding?.tvLocationStatus?.text = "Menunggu GPS..."
            getCurrentLocation()
            return
        }
        
        val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)
        apiAdapter.validateLocation(
            location.latitude,
            location.longitude,
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    updateLocationStatus(response.data.distance.toInt(), response.data.isValid)
                }
            },
            onError = {
                binding?.tvLocationStatus?.text = "Error validasi lokasi"
            }
        )
    }
    
    private fun updateLocationStatus(distance: Int, isValid: Boolean) {
        binding?.tvLocationStatus?.text = "$distance m dari kantor"
        
        val backgroundColor = if (isValid) R.color.success_green else R.color.error_red
        binding?.locationStatusBar?.setBackgroundColor(ContextCompat.getColor(this, backgroundColor))
    }
    
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) return
        
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                currentLocation = location ?: run {
                    requestFreshLocation()
                    return@addOnSuccessListener
                }
            }
        } catch (error: SecurityException) {
            Log.e("MainActivity", "Location permission rejected", error)
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun requestFreshLocation() {
        if (!hasLocationPermission()) return
        
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }
        
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                currentLocation = locationResult.lastLocation
            }
        }
        
        try {
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (error: SecurityException) {
            Log.e("MainActivity", "Fresh location permission rejected", error)
        }
    }
    
    // ========== PROFILE PICTURE ==========
    
    private fun loadProfilePicture() {
        try {
            val employeeId = sessionManager?.getEmployeeId() ?: return
            val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)
            
            apiAdapter.getEmployeeProfile(
                employeeId = employeeId,
                onSuccess = { profile ->
                    try {
                        val photoUrl = profile.employee.profilePicture?.let { picture ->
                            if (picture.startsWith("http://") || picture.startsWith("https://")) {
                                picture
                            } else {
                                "${apiAdapter.getBaseUrl().removeSuffix("/api/")}/${picture.removePrefix("/")}"
                            }
                        }
                        
                        photoUrl?.let { url ->
                            runOnUiThread {
                                try {
                                    com.bumptech.glide.Glide.with(this)
                                        .load(url)
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .circleCrop()
                                        .into(binding?.ivProfilePhoto!!)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error loading profile image: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing profile data: ${e.message}")
                    }
                },
                onError = { error ->
                    Log.e("MainActivity", "Failed to load profile picture: $error")
                }
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in loadProfilePicture: ${e.message}")
        }
    }
    
    // ========== PERMISSIONS ==========
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            showToast(if (allGranted) "Izin diberikan" else "Beberapa izin diperlukan untuk fitur lengkap")
        }
    }
    
    // ========== UTILITY FUNCTIONS ==========
    
    private fun refreshAllData() {
        binding?.swipeRefresh?.isRefreshing = true
        
        loadProfilePicture()
        loadAttendanceData()
        getCurrentLocation()
        
        binding?.swipeRefresh?.postDelayed({
            binding?.swipeRefresh?.isRefreshing = false
        }, 1500)
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun updateDisabledReasons(data: AttendanceStatus, workSchedule: WorkScheduleInfo) {
        clockInDisabledReason = when {
            data.hasCheckedIn -> "Anda sudah clock in hari ini"
            !data.canCheckIn -> "Belum waktunya clock in. Waktu presensi masuk: ${formatTime(workSchedule.clockInStart)} - ${formatTime(workSchedule.clockInEnd)}"
            else -> null
        }
        
        clockOutDisabledReason = when {
            !data.hasCheckedIn -> "Anda belum clock in"
            data.hasCheckedOut -> "Anda sudah clock out hari ini"
            !data.canCheckOut -> "Belum waktunya clock out. Waktu presensi pulang: ${formatTime(workSchedule.clockOutStart)} - ${formatTime(workSchedule.clockOutEnd)}"
            else -> null
        }
    }
    
    private fun parseTimeFromDateTime(dateTime: String): String {
        return try {
            val parts = dateTime.split("T")
            val timePart = parts.lastOrNull() ?: ""
            if (timePart.length >= 5) timePart.substring(0, 5) else "-"
        } catch (e: Exception) {
            "-"
        }
    }
    
    private fun formatTime(time: String?): String {
        return try {
            if (time != null && time.length >= 5) time.substring(0, 5) else time ?: "-"
        } catch (e: Exception) {
            time ?: "-"
        }
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "$hours jam $minutes menit"
        } else {
            "$minutes menit"
        }
    }

    private fun formatDateTimeForDisplay(value: String): String {
        return try {
            val normalized = value.replace('T', ' ')
            val timePart = normalized.split(' ').lastOrNull() ?: value
            if (timePart.length >= 5) timePart.substring(0, 5) else value
        } catch (e: Exception) {
            value
        }
    }
    
    private fun translateDayToIndonesian(day: String): String {
        return when (day.lowercase()) {
            "monday" -> "Senin"
            "tuesday" -> "Selasa"
            "wednesday" -> "Rabu"
            "thursday" -> "Kamis"
            "friday" -> "Jumat"
            "saturday" -> "Sabtu"
            "sunday" -> "Minggu"
            else -> day
        }
    }
    
    private fun isWorkDay(workSchedule: WorkScheduleInfo): Boolean {
        val workDays = workSchedule.hariKerja ?: return true
        
        // Get today's day name in WITA timezone
        val witaTimeZone = java.util.TimeZone.getTimeZone("Asia/Makassar")
        val calendar = java.util.Calendar.getInstance(witaTimeZone)
        val todayName = calendar.getDisplayName(
            java.util.Calendar.DAY_OF_WEEK,
            java.util.Calendar.LONG,
            java.util.Locale.ENGLISH
        ) ?: ""
        
        return workDays.any { it.equals(todayName, ignoreCase = true) }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // ========== LIFECYCLE ==========
    
    override fun onResume() {
        super.onResume()
        binding?.bottomNavigation?.selectedItemId = R.id.nav_home
        refreshAllData()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::locationRunnable.isInitialized) {
            locationHandler.removeCallbacks(locationRunnable)
        }
        binding = null
        sessionManager = null
    }
}
