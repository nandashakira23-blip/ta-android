package com.fleur.attendance

import android.app.Application
import android.content.Intent
import com.fleur.attendance.data.api.TokenManager
import com.fleur.attendance.ui.crash.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter

class AttendanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // === Penangkap crash global — dipasang PALING AWAL, sebelum apa pun ===
        installCrashCatcher()

        try {
            // Load saved token into ApiConfig on app start
            val tokenManager = TokenManager.getInstance(this)
            tokenManager.loadToken()
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("AttendanceApplication", "Error loading token on app start", e)
        }
    }

    /**
     * Tangkap semua exception yang tidak tertangani, tampilkan stack trace-nya lewat CrashActivity
     * (proses terpisah) supaya bisa difoto/disalin — dan simpan juga ke file sebagai cadangan.
     */
    private fun installCrashCatcher() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val info = buildString {
                    append("App    : ").append(packageName).append('\n')
                    append("Version: ").append(appVersion()).append('\n')
                    append("Android: ").append(android.os.Build.VERSION.RELEASE)
                        .append(" (SDK ").append(android.os.Build.VERSION.SDK_INT).append(")\n")
                    append("Device : ").append(android.os.Build.MANUFACTURER).append(' ')
                        .append(android.os.Build.MODEL).append('\n')
                    append("Thread : ").append(thread.name).append("\n\n")
                    append(sw.toString())
                }
                try {
                    openFileOutput("last_crash.txt", MODE_PRIVATE).use { it.write(info.toByteArray()) }
                } catch (_: Exception) {
                }
                val intent = Intent(this, CrashActivity::class.java)
                intent.putExtra(CrashActivity.EXTRA_TRACE, info)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            } catch (_: Throwable) {
                // abaikan — yang penting proses tetap dihentikan di bawah
            } finally {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }

    private fun appVersion(): String {
        return try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            "${pi.versionName} (${pi.versionCode})"
        } catch (e: Exception) {
            "?"
        }
    }
}
