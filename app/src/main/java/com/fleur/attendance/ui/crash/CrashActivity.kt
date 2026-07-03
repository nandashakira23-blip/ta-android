package com.fleur.attendance.ui.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Layar penangkap crash: menampilkan stack trace di layar supaya bisa difoto / disalin.
 * UI dibuat programatik (tanpa XML / tema custom) agar tetap tampil walau resource/tema yang bermasalah.
 * Dijalankan di proses terpisah (:crash) sehingga muncul walau proses utama sudah mati.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "(tidak ada detail crash)"

        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(TextView(this).apply {
            text = "APLIKASI CRASH\nFoto atau salin teks error di bawah, lalu kirim ke developer:"
            setTextColor(Color.parseColor("#B00020"))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(Button(this).apply {
            text = "SALIN TEKS ERROR"
            setOnClickListener {
                try {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                    Toast.makeText(this@CrashActivity, "Tersalin ke clipboard", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                }
            }
        })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        scroll.addView(TextView(this).apply {
            text = trace
            setTextColor(Color.BLACK)
            textSize = 11f
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
            setPadding(0, pad, 0, pad)
        })
        root.addView(scroll)

        setContentView(root)
    }

    companion object {
        const val EXTRA_TRACE = "trace"
    }
}
