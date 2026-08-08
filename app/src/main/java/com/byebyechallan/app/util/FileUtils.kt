package com.byebyechallan.app.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

enum class DocumentFileType {
    IMAGE,
    PDF,
    OFFICE,
    TEXT,
    OTHER
}

object FileUtils {

    private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    private val OFFICE_EXTENSIONS = listOf(
        ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".odt", ".ods", ".odp", ".rtf"
    )

    /**
     * Document/photo pickers return a content:// Uri, not a direct file path.
     * Retrofit's multipart upload needs an actual java.io.File, so this copies
     * the picked file's bytes into the app's cache directory and returns that.
     */
    fun copyUriToCacheFile(context: Context, uri: Uri): File? {
        return try {
            val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}"
            val outputFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    fun getDisplayFileName(context: Context, uri: Uri): String? {
        return getFileName(context, uri)
    }

    fun isImageFile(fileName: String?): Boolean {
        val lower = fileName?.lowercase().orEmpty()
        return listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp").any { lower.endsWith(it) }
    }

    fun isPdfFile(fileName: String?): Boolean {
        return fileName?.lowercase()?.endsWith(".pdf") == true
    }

    fun isImageUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }

    fun isOfficeFile(fileName: String?): Boolean {
        val lower = fileName?.lowercase().orEmpty()
        return OFFICE_EXTENSIONS.any { lower.endsWith(it) }
    }

    fun isXlsxFile(fileName: String?): Boolean {
        return fileName?.lowercase()?.endsWith(".xlsx") == true
    }

    fun isDocxFile(fileName: String?, mimeType: String? = null): Boolean {
        if (fileName?.lowercase()?.endsWith(".docx") == true) return true
        return mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }

    fun isTextFile(fileName: String?): Boolean {
        val lower = fileName?.lowercase().orEmpty()
        return lower.endsWith(".txt") || lower.endsWith(".csv")
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    fun guessMimeType(fileName: String?): String? {
        val lower = fileName?.lowercase().orEmpty()
        return when {
            isImageFile(fileName) -> when {
                lower.endsWith(".png") -> "image/png"
                lower.endsWith(".gif") -> "image/gif"
                lower.endsWith(".webp") -> "image/webp"
                lower.endsWith(".bmp") -> "image/bmp"
                else -> "image/jpeg"
            }
            isPdfFile(fileName) -> "application/pdf"
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") -> "application/vnd.ms-excel"
            lower.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            lower.endsWith(".ppt") -> "application/vnd.ms-powerpoint"
            lower.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".rtf") -> "application/rtf"
            else -> null
        }
    }

    fun resolveDocumentFileType(
        context: Context,
        fileName: String?,
        uri: Uri? = null
    ): DocumentFileType {
        if (isImageFile(fileName)) return DocumentFileType.IMAGE
        if (isPdfFile(fileName)) return DocumentFileType.PDF
        if (isOfficeFile(fileName)) return DocumentFileType.OFFICE
        if (isTextFile(fileName)) return DocumentFileType.TEXT

        uri?.let {
            val mimeType = getMimeType(context, it)
            if (mimeType?.startsWith("image/") == true) return DocumentFileType.IMAGE
            if (mimeType == "application/pdf") return DocumentFileType.PDF
            if (mimeType?.startsWith("text/") == true) return DocumentFileType.TEXT
            if (mimeType in OFFICE_MIME_TYPES) return DocumentFileType.OFFICE
        }
        return DocumentFileType.OTHER
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun openDocument(context: Context, uri: Uri, mimeType: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("document", uri)
        }
        val chooser = Intent.createChooser(intent, "Open document")
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private val OFFICE_MIME_TYPES = setOf(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/rtf"
    )

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
