package com.fleur.attendance.ui.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.fleur.attendance.R
import com.fleur.attendance.data.api.ApiConfig
import com.fleur.attendance.data.model.LeaveRequestItem
import com.fleur.attendance.data.model.LeaveRequestPayload
import com.fleur.attendance.data.model.ReplacementCandidate
import com.fleur.attendance.data.repository.LeaveRepository
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class LeaveRequestActivity : AppCompatActivity() {

    private lateinit var repository: LeaveRepository
    private lateinit var rgLeaveType: RadioGroup
    private lateinit var rgLeaveMode: RadioGroup
    private lateinit var tvLeaveModeHint: TextView
    private lateinit var layoutPengganti: LinearLayout
    private lateinit var etTanggalMulai: EditText
    private lateinit var etTanggalSelesai: EditText
    private lateinit var etJamMulai: EditText
    private lateinit var etJamSelesai: EditText
    private lateinit var actvPengganti: AutoCompleteTextView
    private lateinit var btnClearPengganti: TextView
    private lateinit var etAlasan: EditText
    private lateinit var btnPickAttachment: Button
    private lateinit var tvAttachmentName: TextView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvEmptyReplacement: TextView
    private lateinit var listView: ListView
    private lateinit var listReplacement: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutFormTab: LinearLayout
    private lateinit var layoutHistoryTab: LinearLayout
    private lateinit var layoutReplacementTab: LinearLayout
    private var selectedAttachmentUri: Uri? = null
    private var replacementCandidates: List<ReplacementCandidateOption> = emptyList()
    private var selectedReplacementCandidate: ReplacementCandidateOption? = null

    private val attachmentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAttachmentUri = uri
        tvAttachmentName.text = if (uri != null) {
            "Lampiran: ${extractDisplayName(uri)}"
        } else {
            "Belum ada lampiran"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave_request)

        supportActionBar?.title = "Absensi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = LeaveRepository(this)

        rgLeaveType = findViewById(R.id.rgLeaveType)
        rgLeaveMode = findViewById(R.id.rgLeaveMode)
        tvLeaveModeHint = findViewById(R.id.tvLeaveModeHint)
        layoutPengganti = findViewById(R.id.layoutPengganti)
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        etTanggalSelesai = findViewById(R.id.etTanggalSelesai)
        etJamMulai = findViewById(R.id.etJamMulai)
        etJamSelesai = findViewById(R.id.etJamSelesai)
        actvPengganti = findViewById(R.id.actvPengganti)
        btnClearPengganti = findViewById(R.id.btnClearPengganti)
        etAlasan = findViewById(R.id.etAlasan)
        btnPickAttachment = findViewById(R.id.btnPickAttachment)
        tvAttachmentName = findViewById(R.id.tvAttachmentName)
        btnSubmit = findViewById(R.id.btnSubmitLeave)
        progressBar = findViewById(R.id.progressLeave)
        tvEmpty = findViewById(R.id.tvEmptyLeave)
        tvEmptyReplacement = findViewById(R.id.tvEmptyReplacement)
        listView = findViewById(R.id.listLeaveHistory)
        listReplacement = findViewById(R.id.listReplacementRequests)
        tabLayout = findViewById(R.id.tabLeaveMode)
        layoutFormTab = findViewById(R.id.layoutFormTab)
        layoutHistoryTab = findViewById(R.id.layoutHistoryTab)
        layoutReplacementTab = findViewById(R.id.layoutReplacementTab)

        setupForm()
        setupTabs()
        loadReplacementCandidates()
        loadHistory()

        // Dibuka dari notif beranda -> langsung ke tab Pengganti
        if (intent.getStringExtra("open_tab") == "pengganti") {
            tabLayout.getTabAt(2)?.select()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh saat kembali ke layar ini (mis. setelah manager approve) tanpa buka ulang app
        if (!::tabLayout.isInitialized) return
        when (tabLayout.selectedTabPosition) {
            1 -> loadHistory()
            2 -> loadReplacementRequests()
        }
    }

    private fun setupForm() {
        etTanggalMulai.setOnClickListener { showDatePicker(etTanggalMulai) }
        etTanggalSelesai.setOnClickListener { showDatePicker(etTanggalSelesai) }
        etJamMulai.setOnClickListener { showTimePicker(etJamMulai) }
        etJamSelesai.setOnClickListener { showTimePicker(etJamSelesai) }
        actvPengganti.threshold = 0
        actvPengganti.setOnClickListener { actvPengganti.showDropDown() }
        actvPengganti.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) actvPengganti.showDropDown()
        }
        actvPengganti.setOnItemClickListener { _, _, position, _ ->
            val selected = actvPengganti.adapter.getItem(position) as? ReplacementCandidateOption
            if (selected != null) {
                lockReplacementCandidate(selected)
            }
        }
        btnClearPengganti.setOnClickListener {
            unlockReplacementCandidate(showDropdown = true)
        }
        btnPickAttachment.setOnClickListener {
            attachmentPickerLauncher.launch("*/*")
        }

        btnSubmit.setOnClickListener {
            submitRequest()
        }

        rgLeaveMode.setOnCheckedChangeListener { _, _ -> applyLeaveMode() }
        applyLeaveMode()
    }

    private fun getSelectedLeaveMode(): String {
        return if (rgLeaveMode.checkedRadioButtonId == R.id.rbUrgent) "urgent" else "planned"
    }

    private fun applyLeaveMode() {
        val urgent = getSelectedLeaveMode() == "urgent"
        // Mendadak: pengganti ditentukan Manager -> sembunyikan & kosongkan field pengganti.
        layoutPengganti.visibility = if (urgent) View.GONE else View.VISIBLE
        if (urgent) unlockReplacementCandidate(showDropdown = false)
        // Mendadak: tidak pakai lampiran -> sembunyikan tombol lampiran & kosongkan pilihannya.
        btnPickAttachment.visibility = if (urgent) View.GONE else View.VISIBLE
        tvAttachmentName.visibility = if (urgent) View.GONE else View.VISIBLE
        if (urgent) {
            selectedAttachmentUri = null
            tvAttachmentName.text = "Belum ada lampiran"
        }
        tvLeaveModeHint.text = if (urgent) {
            "Mendadak: langsung ke Manager. Pengganti ditentukan Manager setelah disetujui."
        } else {
            "Terencana: pilih pengganti dulu (perlu ACC pengganti lalu Manager)."
        }
        actvPengganti.hint = if (urgent) "Pengganti ditentukan Manager" else "Pilih karyawan pengganti (wajib)"
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Ajukan"))
        tabLayout.addTab(tabLayout.newTab().setText("Riwayat"))
        tabLayout.addTab(tabLayout.newTab().setText("Pengganti"))
        showTab(0)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTab(tab?.position ?: 0)
                if ((tab?.position ?: 0) == 1) {
                    loadHistory()
                } else if ((tab?.position ?: 0) == 2) {
                    loadReplacementRequests()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Ketuk tab yang sama = refresh data tanpa buka ulang app
                when (tab?.position ?: 0) {
                    1 -> loadHistory()
                    2 -> loadReplacementRequests()
                }
            }
        })
    }

    private fun showTab(position: Int) {
        layoutFormTab.visibility = if (position == 0) View.VISIBLE else View.GONE
        layoutHistoryTab.visibility = if (position == 1) View.VISIBLE else View.GONE
        layoutReplacementTab.visibility = if (position == 2) View.VISIBLE else View.GONE
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(target: EditText) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                target.setText(String.format("%02d:%02d", hourOfDay, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun getSelectedLeaveType(): String {
        return when (rgLeaveType.checkedRadioButtonId) {
            R.id.rbCuti -> "cuti"
            R.id.rbSakit -> "sakit"
            else -> "izin"
        }
    }

    private fun submitRequest() {
        val leaveType = getSelectedLeaveType()      // jenis: cuti/izin/sakit
        val leaveMode = getSelectedLeaveMode()       // tipe: planned/urgent
        val replacementSelection = getSelectedReplacement()
        if (!replacementSelection.isValid) return

        // Terencana wajib pengganti; Mendadak tidak boleh pilih pengganti (ditentukan Manager).
        val idPengganti: Int? = if (leaveMode == "urgent") null else replacementSelection.id
        if (leaveMode == "planned" && idPengganti == null) {
            Toast.makeText(this, "Pengajuan Terencana wajib memilih karyawan pengganti", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = LeaveRequestPayload(
            jenis = leaveType,
            leaveType = leaveMode,
            tanggalMulai = etTanggalMulai.text.toString().trim(),
            tanggalSelesai = etTanggalSelesai.text.toString().trim(),
            jamMulai = etJamMulai.text.toString().trim().ifBlank { null },
            jamSelesai = etJamSelesai.text.toString().trim().ifBlank { null },
            idPengganti = idPengganti,
            alasan = etAlasan.text.toString().trim()
        )

        if (payload.jenis.isBlank() ||
            payload.tanggalMulai.isBlank() ||
            payload.tanggalSelesai.isBlank() ||
            payload.alasan.isBlank()
        ) {
            Toast.makeText(this, "Lengkapi data pengajuan terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (leaveType == "sakit" && leaveMode != "urgent" && selectedAttachmentUri == null) {
            Toast.makeText(this, "Jenis sakit wajib lampiran surat", Toast.LENGTH_SHORT).show()
            return
        }

        val attachmentFile = selectedAttachmentUri?.let { uri ->
            createTempAttachmentFile(uri)
        }

        setLoading(true)
        repository.createLeaveRequest(
            payload = payload,
            attachmentFile = attachmentFile,
            onSuccess = { response ->
                runOnUiThread {
                    setLoading(false)
                    if (response.success) {
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                        clearForm()
                        tabLayout.getTabAt(1)?.select()
                        loadHistory()
                    } else {
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun loadHistory() {
        setLoading(true)
        repository.getLeaveRequests(
            onSuccess = { response ->
                runOnUiThread {
                    setLoading(false)
                    renderHistory(response.data)
                }
            },
            onError = { error ->
                runOnUiThread {
                    setLoading(false)
                    tvEmpty.visibility = View.VISIBLE
                    listView.visibility = View.GONE
                    tvEmpty.text = error
                }
            }
        )
    }

    private fun loadReplacementCandidates() {
        repository.getReplacementCandidates(
            onSuccess = { response ->
                runOnUiThread {
                    replacementCandidates = response.data.map { ReplacementCandidateOption(it) }
                    bindReplacementCandidateDropdown()
                    unlockReplacementCandidate(showDropdown = false)
                }
            },
            onError = { error ->
                runOnUiThread {
                    replacementCandidates = emptyList()
                    bindReplacementCandidateDropdown()
                    unlockReplacementCandidate(showDropdown = false)
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun bindReplacementCandidateDropdown() {
        actvPengganti.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                replacementCandidates
            )
        )
    }

    private fun lockReplacementCandidate(candidate: ReplacementCandidateOption) {
        selectedReplacementCandidate = candidate
        actvPengganti.setText(candidate.toString(), false)
        actvPengganti.dismissDropDown()
        actvPengganti.clearFocus()
        actvPengganti.inputType = InputType.TYPE_NULL
        actvPengganti.isFocusable = false
        actvPengganti.isFocusableInTouchMode = false
        actvPengganti.isCursorVisible = false
        actvPengganti.isLongClickable = false
        btnClearPengganti.visibility = View.VISIBLE
    }

    private fun unlockReplacementCandidate(showDropdown: Boolean) {
        selectedReplacementCandidate = null
        actvPengganti.setText("", false)
        actvPengganti.inputType = InputType.TYPE_CLASS_TEXT
        actvPengganti.isFocusable = true
        actvPengganti.isFocusableInTouchMode = true
        actvPengganti.isCursorVisible = true
        actvPengganti.isLongClickable = true
        btnClearPengganti.visibility = View.GONE

        if (showDropdown) {
            actvPengganti.requestFocus()
            actvPengganti.post { actvPengganti.showDropDown() }
        }
    }

    private fun getSelectedReplacement(): ReplacementSelection {
        val replacementText = actvPengganti.text.toString().trim()
        if (replacementText.isBlank()) {
            selectedReplacementCandidate = null
            return ReplacementSelection(isValid = true, id = null)
        }

        val selected = selectedReplacementCandidate
            ?.takeIf { it.toString().equals(replacementText, ignoreCase = true) }

        if (selected == null) {
            Toast.makeText(this, "Pilih karyawan pengganti dari dropdown", Toast.LENGTH_SHORT).show()
            actvPengganti.requestFocus()
            actvPengganti.showDropDown()
            return ReplacementSelection(isValid = false, id = null)
        }

        selectedReplacementCandidate = selected
        return ReplacementSelection(isValid = true, id = selected.id)
    }

    private fun loadReplacementRequests() {
        setLoading(true)
        repository.getReplacementRequests(
            onSuccess = { response ->
                runOnUiThread {
                    setLoading(false)
                    renderReplacementRequests(response.data)
                }
            },
            onError = { error ->
                runOnUiThread {
                    setLoading(false)
                    tvEmptyReplacement.visibility = View.VISIBLE
                    listReplacement.visibility = View.GONE
                    tvEmptyReplacement.text = error
                }
            }
        )
    }

    private fun renderHistory(items: List<LeaveRequestItem>) {
        if (items.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            listView.visibility = View.GONE
            tvEmpty.text = "Belum ada pengajuan Absensi"
            return
        }

        tvEmpty.visibility = View.GONE
        listView.visibility = View.VISIBLE
        listView.adapter = LeaveHistoryAdapter(items)
    }

    private fun renderReplacementRequests(items: List<LeaveRequestItem>) {
        if (items.isEmpty()) {
            tvEmptyReplacement.visibility = View.VISIBLE
            listReplacement.visibility = View.GONE
            tvEmptyReplacement.text = "Tidak ada permintaan pengganti"
            return
        }

        tvEmptyReplacement.visibility = View.GONE
        listReplacement.visibility = View.VISIBLE
        listReplacement.adapter = LeaveHistoryAdapter(items, showReplacementActions = true)
    }

    private fun decideReplacementRequest(item: LeaveRequestItem, approve: Boolean) {
        setLoading(true)
        val note = if (approve) "Bersedia menjadi pengganti" else "Tidak bersedia menjadi pengganti"
        val onSuccess: (com.fleur.attendance.data.model.ApiResponse<Any>) -> Unit = { response ->
            runOnUiThread {
                setLoading(false)
                Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                loadReplacementRequests()
            }
        }
        val onError: (String) -> Unit = { error ->
            runOnUiThread {
                setLoading(false)
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        if (approve) {
            repository.approveReplacementRequest(item.id, note, onSuccess, onError)
        } else {
            repository.rejectReplacementRequest(item.id, note, onSuccess, onError)
        }
    }

    private fun isCancellableStatus(status: String): Boolean {
        return when (status.lowercase(Locale.getDefault())) {
            "pending", "menunggu_manager", "menunggu_pengganti" -> true
            else -> false
        }
    }

    private fun confirmCancelLeaveRequest(item: LeaveRequestItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Batalkan Pengajuan")
            .setMessage("Yakin membatalkan pengajuan ${item.jenis.uppercase(Locale.getDefault())} ini?")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                setLoading(true)
                repository.cancelLeaveRequest(
                    requestId = item.id,
                    onSuccess = { response ->
                        runOnUiThread {
                            setLoading(false)
                            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                            loadHistory()
                        }
                    },
                    onError = { error ->
                        runOnUiThread {
                            setLoading(false)
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun clearForm() {
        etTanggalMulai.text?.clear()
        etTanggalSelesai.text?.clear()
        etJamMulai.text?.clear()
        etJamSelesai.text?.clear()
        unlockReplacementCandidate(showDropdown = false)
        etAlasan.text?.clear()
        rgLeaveType.check(R.id.rbIzin)
        rgLeaveMode.check(R.id.rbPlanned)
        applyLeaveMode()
        selectedAttachmentUri = null
        tvAttachmentName.text = "Belum ada lampiran"
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !isLoading
    }

    private fun extractDisplayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "lampiran"
            }
        }
        return "lampiran"
    }

    private fun createTempAttachmentFile(uri: Uri): File? {
        return try {
            val fileName = extractDisplayName(uri)
            val extension = fileName.substringAfterLast('.', "tmp")
            val tempFile = File.createTempFile("leave-attachment-", ".$extension", cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membaca lampiran", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun getStatusColor(status: String): Int {
        val resId = when (status.lowercase(Locale.getDefault())) {
            "approved", "disetujui" -> R.color.leave_status_approved
            "rejected", "ditolak", "ditolak_pengganti" -> R.color.leave_status_rejected
            "cancelled", "dibatalkan" -> R.color.leave_status_cancelled
            "menunggu_pengganti" -> R.color.leave_status_pending_replacement
            else -> R.color.leave_status_pending_manager
        }
        return androidx.core.content.ContextCompat.getColor(this, resId)
    }

    private fun formatStatusLabel(status: String): String {
        return when (status.lowercase(Locale.getDefault())) {
            "approved", "disetujui" -> "DISETUJUI"
            "rejected", "ditolak" -> "DITOLAK"
            "ditolak_pengganti" -> "DITOLAK PENGGANTI"
            "cancelled", "dibatalkan" -> "DIBATALKAN"
            "menunggu_pengganti" -> "MENUNGGU PENGGANTI"
            "pending", "menunggu_manager" -> "MENUNGGU MANAGER"
            else -> status.uppercase(Locale.getDefault())
        }
    }

    private fun formatApiDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val value = raw.trim()
        return when {
            value.contains('T') -> value.substringBefore('T')
            value.length >= 10 -> value.substring(0, 10)
            else -> value
        }
    }

    private fun formatApiTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val value = raw.trim()
        return when {
            value.contains('T') -> {
                val timePart = value.substringAfter('T').substringBefore('Z')
                if (timePart.length >= 5) timePart.substring(0, 5) else timePart
            }
            value.length >= 5 -> value.substring(0, 5)
            else -> value
        }
    }

    private inner class LeaveHistoryAdapter(
        private val items: List<LeaveRequestItem>,
        private val showReplacementActions: Boolean = false
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = items[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@LeaveRequestActivity)
                .inflate(R.layout.item_leave_history, parent, false)

            val item = items[position]
            val tvJenis = view.findViewById<TextView>(R.id.tvJenisValue)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatusValue)
            val tvPeriode = view.findViewById<TextView>(R.id.tvPeriodeValue)
            val tvJam = view.findViewById<TextView>(R.id.tvJamValue)
            val tvPengganti = view.findViewById<TextView>(R.id.tvPenggantiValue)
            val tvAlasan = view.findViewById<TextView>(R.id.tvAlasanValue)
            val tvLampiran = view.findViewById<TextView>(R.id.tvLampiranValue)
            val layoutActions = view.findViewById<LinearLayout>(R.id.layoutReplacementActions)
            val btnApprove = view.findViewById<Button>(R.id.btnApproveReplacement)
            val btnReject = view.findViewById<Button>(R.id.btnRejectReplacement)
            val btnCancel = view.findViewById<Button>(R.id.btnCancelLeave)

            tvJenis.text = if (showReplacementActions && !item.namaKaryawan.isNullOrBlank()) {
                "${item.jenis.uppercase(Locale.getDefault())} - ${item.namaKaryawan}"
            } else {
                item.jenis.uppercase(Locale.getDefault())
            }
            tvStatus.text = formatStatusLabel(item.status)
            tvStatus.setBackgroundColor(getStatusColor(item.status))

            val tanggalMulai = formatApiDate(item.tanggalMulai)
            val tanggalSelesai = formatApiDate(item.tanggalSelesai)
            tvPeriode.text = if (item.tanggalMulai == item.tanggalSelesai) {
                tanggalMulai
            } else {
                "$tanggalMulai s/d $tanggalSelesai"
            }

            val jamMulai = formatApiTime(item.jamMulai)
            val jamSelesai = formatApiTime(item.jamSelesai)
            val kategoriLabel = when (item.kategori?.lowercase(Locale.getDefault())) {
                "half_day" -> "Setengah Hari"
                "hourly" -> "Per Jam"
                else -> "Seharian"
            }
            tvJam.text = if (!item.jamMulai.isNullOrBlank() || !item.jamSelesai.isNullOrBlank()) {
                "$kategoriLabel ($jamMulai - $jamSelesai)"
            } else {
                kategoriLabel
            }

            tvPengganti.text = if (showReplacementActions) {
                "Pemohon: ${item.namaKaryawan ?: "-"}"
            } else when {
                !item.namaPengganti.isNullOrBlank() && !item.approvedPenggantiAt.isNullOrBlank() ->
                    "Pengganti: ${item.namaPengganti} (sudah setuju)"
                !item.namaPengganti.isNullOrBlank() ->
                    "Pengganti: ${item.namaPengganti}"
                item.idPengganti != null ->
                    "Pengganti: ID ${item.idPengganti}"
                else -> "Pengganti: -"
            }

            tvAlasan.text = item.alasan

            if (!item.lampiran.isNullOrBlank()) {
                tvLampiran.text = "Lampiran: tersedia (ketuk untuk buka)"
                tvLampiran.setTextColor(androidx.core.content.ContextCompat.getColor(this@LeaveRequestActivity, R.color.primary_brown))
                tvLampiran.setOnClickListener {
                    val normalized = item.lampiran.replace("\\", "/").removePrefix("public/")
                    val baseUrl = ApiConfig.getBaseUrl().trimEnd('/')
                    val fullUrl = if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                        normalized
                    } else {
                        "$baseUrl/${normalized.removePrefix("/")}"
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                }
            } else {
                tvLampiran.text = "Lampiran: tidak ada"
                tvLampiran.setTextColor(androidx.core.content.ContextCompat.getColor(this@LeaveRequestActivity, R.color.text_secondary))
                tvLampiran.setOnClickListener(null)
            }

            if (showReplacementActions && item.status.equals("menunggu_pengganti", ignoreCase = true)) {
                layoutActions.visibility = View.VISIBLE
                btnApprove.setOnClickListener { decideReplacementRequest(item, approve = true) }
                btnReject.setOnClickListener { decideReplacementRequest(item, approve = false) }
            } else {
                layoutActions.visibility = View.GONE
                btnApprove.setOnClickListener(null)
                btnReject.setOnClickListener(null)
            }

            // Tombol batalkan hanya di tab Riwayat untuk pengajuan yang belum diputus manager
            if (!showReplacementActions && isCancellableStatus(item.status)) {
                btnCancel.visibility = View.VISIBLE
                btnCancel.setOnClickListener { confirmCancelLeaveRequest(item) }
            } else {
                btnCancel.visibility = View.GONE
                btnCancel.setOnClickListener(null)
            }

            return view
        }
    }

    private data class ReplacementCandidateOption(
        val id: Int,
        val nik: String,
        val nama: String,
        val jabatan: String?
    ) {
        constructor(candidate: ReplacementCandidate) : this(
            id = candidate.id,
            nik = candidate.nik,
            nama = candidate.nama,
            jabatan = candidate.namaJabatan
        )

        override fun toString(): String {
            val suffix = jabatan?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
            return "$nama ($nik)$suffix"
        }
    }

    private data class ReplacementSelection(
        val isValid: Boolean,
        val id: Int?
    )
}
