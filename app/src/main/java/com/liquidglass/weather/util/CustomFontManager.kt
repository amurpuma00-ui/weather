package com.liquidglass.weather.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import java.io.File

object CustomFontManager {
    data class ImportedFont(val path: String, val displayName: String)

    fun importFont(context: Context, uri: Uri): ImportedFont? {
        return runCatching {
            val dir = File(context.filesDir, "fonts").apply { mkdirs() }
            val name = queryName(context, uri) ?: "custom.ttf"
            val safe = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val dest = File(dir, "weather_${System.currentTimeMillis()}_$safe")
            context.contentResolver.openInputStream(uri)?.use { inp ->
                dest.outputStream().use { out -> inp.copyTo(out) }
            } ?: return null
            Typeface.createFromFile(dest)
            ImportedFont(dest.absolutePath, name.substringBeforeLast("."))
        }.getOrNull()
    }

    fun resolveFontFamily(path: String): FontFamily {
        val f = File(path)
        if (path.isNotBlank() && f.exists()) {
            return runCatching { FontFamily(Typeface.createFromFile(f)) }.getOrDefault(FontFamily.SansSerif)
        }
        return FontFamily.SansSerif
    }

    private fun queryName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull()
}
