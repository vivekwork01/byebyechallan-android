package com.byebyechallan.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object AuthenticatedFileDownloader {

    suspend fun downloadToCache(
        context: Context,
        url: String,
        authToken: String?,
        preferredFileName: String?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    if (!authToken.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Bearer $authToken")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@withContext null

            val safeName = preferredFileName
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "preview_${System.currentTimeMillis()}"

            val previewDir = File(context.cacheDir, "document_previews").apply { mkdirs() }
            val outputFile = File(previewDir, safeName)
            response.body?.byteStream()?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.takeIf { it.exists() && it.length() > 0 }
        } catch (e: Exception) {
            null
        }
    }
}
