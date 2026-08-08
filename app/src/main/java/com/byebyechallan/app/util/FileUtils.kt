package com.byebyechallan.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

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
