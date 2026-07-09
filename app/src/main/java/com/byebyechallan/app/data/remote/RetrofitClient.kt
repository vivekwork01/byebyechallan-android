package com.byebyechallan.app.data.remote

import com.byebyechallan.app.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit

/**
 * Builds the single shared Retrofit instance for the app.
 *
 * BASE_URL comes from BuildConfig (set in app/build.gradle.kts). For the Android
 * emulator talking to a backend on your laptop, 10.0.2.2 is the special alias
 * that means "the machine running the emulator" - see README for real-device setup.
 */
object RetrofitClient {

    private class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = runBlocking { sessionManager.getToken() }
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }.build()
            return chain.proceed(request)
        }
    }

    fun create(sessionManager: SessionManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
