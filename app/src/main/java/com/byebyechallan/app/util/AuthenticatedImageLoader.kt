package com.byebyechallan.app.util

import android.content.Context
import coil.ImageLoader
import okhttp3.OkHttpClient

object AuthenticatedImageLoader {

    fun create(context: Context, authToken: String?): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                if (!authToken.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .build()
    }
}
