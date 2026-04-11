package com.fleur.attendance.ui.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.fleur.attendance.data.repository.LeaveRepository
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class LeaveRequestActivity : AppCompatActivity() {

    private lateinit var repository: LeaveRepository
    private lateinit var rgLeaveType: RadioGroup
    private lateinit var etTanggalMulai: EditText
    private lateinit var etTanggalSelesai: EditText
    private lateinit var etJamMulai: EditText
    private lateinit var etJamSelesai: EditText
    private lateinit var etAlasan: EditText
    private lateinit var btnPickAttachment: Button
    private lateinit var tvAttachmentName: TextView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var listView: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutFormTab: LinearLayout
    private lateinit var layoutHistoryTab: LinearLayout
    private var selectedAttachmentUri: Uri? = null

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

        supportActionBar?.title = "Izin & Sakit"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = LeaveRepository(this)

        rgLeaveType = findViewById(R.id.rgLeaveType)
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        etTanggalSelesai = findViewById(R.id.etTanggalSelesai)
        etJamMulai = findViewById(R.id.etJamMulai)
        etJamSelesai = findViewById(R.id.etJamSelesai)
        etAlasan = findViewById(R.id.etAlasan)
        btnPickAttachment = findViewById(R.id.btnPickAttachment)
        tvAttachmentName = findViewById(R.id.tvAttachmentName)
        btnSubmit = findViewById(R.id.btnSubmitLeave)
        progressBar = findViewById(R.id.progressLeave)
        tvEmpty = findViewById(R.id.tvEmptyLeave)
        listView = findViewById(R.id.listLeaveHistory)
        tabLayout = findViewById(R.id.tabLeaveMode)
        layoutFormTab = findViewById(R.id.layoutFormTab)
        layoutHistoryTab = findViewById(R.id.layoutHistoryTab)

        setupForm()
        setupTabs()
        loadHistory()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupForm() {
        etTanggalMulai.setOnClickListener { showDatePicker(etTanggalMulai) }
        etTanggalSelesai.setOnClickListener { showDatePicker(etTanggalSelesai) }
        etJamMulai.setOnClickListener { showTimePicker(etJamMulai) }
        etJamSelesai.setOnClickListener { showTimePicker(etJamSelesai) }
        btnPickAttachment.setOnClickListener {
            attachmentPickerLauncher.launch("*/*")
        }

        btnSubmit.setOnClickListener {
            submitRequest()
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Ajukan"))
        tabLayout.addTab(tabLayout.newTab().setText("Riwayat"))
        showTab(0)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTab(tab?.position ?: 0)
                if ((tab?.position ?: 0) == 1) {
                    loadHistory()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTab(position: Int) {
        layoutFormTab.visibility = if (position == 0) View.VISIBLE else View.GONE
        layoutHistoryTab.visibility = if (position == 1) View.VISIBLE else View.GONE
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
            R.id.rbSakit -> "sakit"
            else -> "izin"
        }
    }

    private fun submitRequest() {
        val leaveType = getSelectedLeaveType()
        val payload = LeaveRequestPayload(
            jenis = leaveType,
            tanggalMulai = etTanggalMulai.text.toString().trim(),
            tanggalSelesai = etTanggalSelesai.text.toString().trim(),
            jamMulai = etJamMulai.text.toString().trim().ifBlank { null },
            jamSelesai = etJamSelesai.text.toString().trim().ifBlank { null },
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

        if (leaveType == "sakit" && selectedAttachmentUri == null) {
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

    private fun renderHistory(items: List<LeaveRequestItem>) {
        if (items.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            listView.visibility = View.GONE
            tvEmpty.text = "Belum ada pengajuan izin atau sakit"
            return
        }

        tvEmpty.visibility = View.GONE
        listView.visibility = View.VISIBLE
        listView.adapter = LeaveHistoryAdapter(items)
    }

    private fun clearForm() {
        etTanggalMulai.text?.clear()
        etTanggalSelesai.text?.clear()
        etJamMulai.text?.clear()
        etJamSelesai.text?.clear()
        etAlasan.text?.clear()
        rgLeaveType.check(R.id.rbIzin)
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
        return when (status.lowercase(Locale.getDefault())) {
            "approved" -> android.graphics.Color.parseColor("#14532D")
            "rejected" -> android.graphics.Color.parseColor("#7F1D1D")
            "cancelled" -> android.graphics.Color.parseColor("#374151")
            else -> android.graphics.Color.parseColor("#78350F")
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
        private val items: List<LeaveRequestItem>
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
            val tvAlasan = view.findViewById<TextView>(R.id.tvAlasanValue)
            val tvLampiran = view.findViewById<TextView>(R.id.tvLampiranValue)

            tvJenis.text = item.jenis.uppercase(Locale.getDefault())
            tvStatus.text = item.status.uppercase(Locale.getDefault())
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
            tvJam.text = if (!item.jamMulai.isNullOrBlank() || !item.jamSelesai.isNullOrBlank()) {
                "Jam: $jamMulai - $jamSelesai"
            } else {
                "Jam: full day"
            }

            tvAlasan.text = item.alasan

            if (!item.lampiran.isNullOrBlank()) {
                tvLampiran.text = "Lampiran: tersedia (ketuk untuk buka)"
                tvLampiran.setTextColor(android.graphics.Color.parseColor("#A67C52"))
                tvLampiran.setOnClickListener {
                    val normalized = item.lampiran.replace("\\", "/").removePrefix("public/")
                    val baseUrl = ApiConfig.getBaseUrl().trimEnd('/')
                    val fullUrl = "$baseUrl/$normalized"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                }
            } else {
                tvLampiran.text = "Lampiran: tidak ada"
                tvLampiran.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                tvLampiran.setOnClickListener(null)
            }

            return view
        }
    }
}
