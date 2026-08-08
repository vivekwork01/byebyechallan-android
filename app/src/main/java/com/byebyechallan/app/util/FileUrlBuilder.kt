package com.byebyechallan.app.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object FileUrlBuilder {

    fun downloadUrl(baseUrl: String, userId: Long, fileName: String): String {
        val encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
        return "${baseUrl.trimEnd('/')}/api/v1/file/$userId/$encodedName"
    }
}
