package com.fleur.attendance.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fleur.attendance.R

/**
 * Overlay loading FULL-SCREEN. Ditempel ke android.R.id.content, jadi SELALU memenuhi layar
 * apa pun struktur layout activity-nya (termasuk yang di dalam ScrollView).
 * Nuansa gelap + kartu rounded + spinner aksen coklat -> selaras tema.
 */
object LoadingOverlay {
    private const val TAG = "fleur_loading_overlay"

    fun show(activity: Activity, message: String = "Memproses...", subtitle: String = "Mohon tunggu sebentar") {
        activity.runOnUiThread {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread
            if (root.findViewWithTag<View>(TAG) != null) return@runOnUiThread
            val d = activity.resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()

            val overlay = FrameLayout(activity).apply {
                tag = TAG
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(ContextCompat.getColor(activity, R.color.dark_background))
                isClickable = true
                isFocusable = true
            }

            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(36), dp(30), dp(36), dp(30))
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(ContextCompat.getColor(activity, R.color.dark_surface))
                }
                elevation = dp(8).toFloat()
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
                )
            }

            val spinner = ProgressBar(activity).apply {
                indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary_brown))
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            }
            val title = TextView(activity).apply {
                text = message
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                textSize = 16f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(18) }
            }
            val sub = TextView(activity).apply {
                text = subtitle
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                textSize = 13f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }

            card.addView(spinner)
            card.addView(title)
            card.addView(sub)
            overlay.addView(card)
            root.addView(overlay)
        }
    }

    fun hide(activity: Activity) {
        activity.runOnUiThread {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread
            root.findViewWithTag<View>(TAG)?.let { root.removeView(it) }
        }
    }
}
