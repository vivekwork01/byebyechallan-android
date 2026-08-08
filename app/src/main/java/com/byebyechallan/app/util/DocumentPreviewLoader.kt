package com.byebyechallan.app.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DocumentPreviewLoader {

    suspend fun loadFile(
        context: Context,
        previewUri: Uri?,
        previewUrl: String?,
        displayFileName: String?,
        authToken: String?
    ): File? = withContext(Dispatchers.IO) {
        previewUri?.let { uri ->
            FileUtils.copyUriToCacheFile(context, uri)
        } ?: previewUrl?.let { url ->
            AuthenticatedFileDownloader.downloadToCache(
                context = context,
                url = url,
                authToken = authToken,
                preferredFileName = displayFileName
            )
        }
    }

    suspend fun loadBytes(
        context: Context,
        previewUri: Uri?,
        previewUrl: String?,
        displayFileName: String?,
        authToken: String?
    ): ByteArray? = withContext(Dispatchers.IO) {
        previewUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: loadFile(context, previewUri, previewUrl, displayFileName, authToken)?.readBytes()
    }
}
