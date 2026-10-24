package com.reproductor.kiosk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Logos of the FM stations, one image per frequency in the app's own folder
 * (/sdcard/Android/data/com.reproductor.kiosk/files/emisoras/93.9.png). They are added from
 * Settings → Logos de emisoras (or from a PC with adb push) and need no storage permission.
 */
class StationLogos(context: Context) {
    private val dir: File? = context.getExternalFilesDir(DIR)

    /** Decoded logos with the file date they came from, so a changed logo is read again. */
    private val cache = HashMap<String, Pair<Long, Bitmap?>>()

    fun logo(frequency: String): Bitmap? {
        val file = file(frequency) ?: return null.also { cache.remove(frequency) }
        cache[frequency]?.let { (modified, bitmap) -> if (modified == file.lastModified()) return bitmap }
        return BitmapFactory.decodeFile(file.path).also { cache[frequency] = file.lastModified() to it }
    }

    /** Frequencies that have a logo, in dial order. */
    fun frequencies(): List<String> =
        dir?.listFiles().orEmpty()
            .filter { it.isFile }
            .mapNotNull { normalize(it.nameWithoutExtension)?.takeIf { f -> it.name in fileNames(f) } }
            .distinct()
            .sortedBy { it.toDouble() }

    /** Saves [image] as the logo of [frequency] (longest side 512 px), replacing any other. */
    fun save(frequency: String, image: Bitmap) {
        val target = dir ?: return
        target.mkdirs()
        remove(frequency)
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(image.width, image.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(image, (image.width * scale).toInt(), (image.height * scale).toInt(), true)
        } else image
        File(target, "$frequency.png").outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun remove(frequency: String) {
        fileNames(frequency).forEach { name -> dir?.let { File(it, name) }?.delete() }
        cache.remove(frequency)
    }

    private fun file(frequency: String): File? =
        fileNames(frequency).asSequence().mapNotNull { name -> dir?.let { File(it, name) }?.takeIf { it.isFile } }.firstOrNull()

    companion object {
        const val DIR = "emisoras"
        private const val MAX_SIDE = 512

        fun fileNames(frequency: String): List<String> =
            listOf("png", "jpg", "jpeg", "webp").map { "$frequency.$it" }

        private val typed = Regex("""\d{2,3}([.,]\d{1,2})?""")

        /** "93,9" or " 98 " as typed → "93.9" / "98.0", as the radio publishes it; null outside the FM band. */
        fun normalize(text: String): String? {
            val clean = text.trim()
            if (!typed.matches(clean)) return null
            val mhz = clean.replace(',', '.').toDouble()
            if (mhz < 87.5 || mhz > 108.0) return null
            return String.format(java.util.Locale.ROOT, "%.1f", mhz)
        }
    }
}
