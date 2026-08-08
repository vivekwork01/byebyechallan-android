package com.byebyechallan.app.util

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object FileUrlBuilder {

    fun downloadUrl(baseUrl: String, userId: Long, fileName: String): String {
        // Encode each path segment separately so spaces become %20, not "+".
        val encodedName = fileName.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        }
        return "${baseUrl.trimEnd('/')}/api/v1/file/$userId/$encodedName"
    }
}
