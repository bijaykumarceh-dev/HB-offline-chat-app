package com.hb.offlinechat.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale

/** Utilities for working with SAF content URIs without broad storage access. */
object FileUtils {

    data class FileInfo(val name: String, val size: Long, val mimeType: String)

    fun info(context: Context, uri: Uri): FileInfo {
        var name = "file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        if (size == 0L) {
            size = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L
        }
        val mime = context.contentResolver.getType(uri) ?: guessMime(name)
        return FileInfo(name, size, mime)
    }

    fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    fun readableSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** App-visible downloads directory; no storage permission required. */
    fun downloadsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Downloads")
        dir.mkdirs()
        return dir
    }

    /** Ensure a unique filename to avoid overwriting previously received files. */
    fun uniqueFile(dir: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var candidate = File(dir, name)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isBlank()) "$base ($counter)" else "$base ($counter).$ext")
            counter++
        }
        return candidate
    }
}
