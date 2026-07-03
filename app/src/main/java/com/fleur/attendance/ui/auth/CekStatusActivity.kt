package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityCekStatusBinding

/**
 * First screen after the Splash (when not logged in).
 * User enters their NIK; the app asks the server (/api/auth/check-nik) for the activation status,
 * then routes:
 *   - not registered -> show "Hubungi Admin"
 *   - registered & activated   -> Login (NIK prefilled)
 *   - registered & not activated -> Activation (NIK prefilled)
 */
class CekStatusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCekStatusBinding
    private lateinit var apiAdapter: LegacyApiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCekStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiAdapter = LegacyApiAdapter(this)
        binding.etNik.requestFocus()
        binding.btnCekStatus.setOnClickListener { cekStatus() }
    }

    private fun cekStatus() {
        val nik = binding.etNik.text.toString().trim()
        if (nik.length != 16) {
            binding.tilNik.error = "NIK harus 16 digit"
            return
        }
        binding.tilNik.error = null
        showLoading(true)

        apiAdapter.checkNik(
            nik,
            onSuccess = { response ->
                showLoading(false)
                val data = response.data
                if (response.success && data != null && data.exists) {
                    if (data.isActivated) {
                        // Sudah aktif -> Login (NIK prefilled)
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("nik", nik)
                        startActivity(intent)
                    } else {
                        // Belum aktif -> Aktivasi (NIK prefilled)
                        val intent = Intent(this, ActivationActivity::class.java)
                        intent.putExtra("nik", nik)
                        intent.putExtra("employeeName", data.employee?.nama ?: "Karyawan")
                        intent.putExtra("isReactivation", false)
                        startActivity(intent)
                    }
                } else {
                    binding.tilNik.error = "NIK tidak terdaftar. Silakan hubungi Admin."
                }
            },
            onError = { error ->
                showLoading(false)
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) com.fleur.attendance.utils.LoadingOverlay.show(this) else com.fleur.attendance.utils.LoadingOverlay.hide(this)
        binding.btnCekStatus.isEnabled = !show
    }
}
