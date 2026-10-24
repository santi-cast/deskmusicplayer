package com.reproductor.kiosk

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Settings → Logos de emisoras: the FM station logos shown behind the clock, added from the
 * phone with Android's image picker (allowed through the lock while picking).
 */
class LogosActivity : Activity() {
    private lateinit var logos: StationLogos
    private lateinit var list: LinearLayout

    /** Frequency waiting for the picked image. */
    private var pendingFrequency: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logos = StationLogos(this)
        pendingFrequency = savedInstanceState?.getString(STATE_FREQUENCY)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        setContentView(ScrollView(this).apply { addView(list) })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FREQUENCY, pendingFrequency)
    }

    override fun onResume() {
        super.onResume()
        // Back from the image picker: restore the normal allow list and lock.
        Kiosk.lockIfEnabled(this)
        render()
    }

    private fun render() {
        list.removeAllViews()
        list.addView(text(getString(R.string.logos_title), 24f, 0xFFE0E0E0.toInt()))
        list.addView(text(getString(R.string.logos_intro), 15f, 0xFF9A9A9A.toInt()))
        list.addView(button(R.string.logos_add) { askFrequency() })
        val frequencies = logos.frequencies()
        if (frequencies.isEmpty()) list.addView(text(getString(R.string.logos_none), 15f, 0xFF9A9A9A.toInt()))
        frequencies.forEach { frequency ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(ImageView(this).apply {
                setImageBitmap(logos.logo(frequency))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(64), dp(64)))
            row.addView(text("$frequency MHz", 18f, 0xFFE0E0E0.toInt()).apply { setPadding(dp(16), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(button(R.string.logos_change) { pickImage(frequency) })
            row.addView(button(R.string.app_remove) {
                logos.remove(frequency)
                render()
            })
            list.addView(row)
        }
        list.addView(button(R.string.close) { finish() })
    }

    /** Asks for the frequency, prefilled with the station the radio is playing now. */
    private fun askFrequency() {
        val playing = NowPlaying(this) {}.run { if (start()) radioFrequency.also { stop() } else null }
        val field = EditText(this).apply {
            // The plain decimal keyboard drops the comma, the usual decimal mark in Spain.
            keyListener = DigitsKeyListener.getInstance("0123456789.,")
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            hint = "93.9"
            setText(playing.orEmpty())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.logos_frequency_title)
            .setView(field)
            .setPositiveButton(R.string.logos_pick) { _, _ ->
                val frequency = StationLogos.normalize(field.text.toString())
                if (frequency == null) Toast.makeText(this, R.string.logos_bad_frequency, Toast.LENGTH_LONG).show()
                else pickImage(frequency)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickImage(frequency: String) {
        pendingFrequency = frequency
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
        Kiosk.allowTemporarily(this, picker)
        startActivityForResult(picker, REQUEST_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val frequency = pendingFrequency
        val uri = data?.data
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK || frequency == null || uri == null) return
        pendingFrequency = null
        val image = decode(uri)
        if (image == null) {
            Toast.makeText(this, R.string.logos_bad_image, Toast.LENGTH_LONG).show()
            return
        }
        logos.save(frequency, image)
        Toast.makeText(this, getString(R.string.logos_saved, frequency), Toast.LENGTH_SHORT).show()
    }

    /** Decodes at a reduced size first, so a large photo does not run out of memory. */
    private fun decode(uri: Uri) = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 512) sample *= 2
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    private fun button(label: Int, onClick: () -> Unit) = Button(this).apply {
        setText(label)
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun text(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun dp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        const val REQUEST_IMAGE = 1
        const val STATE_FREQUENCY = "frequency"
    }
}
