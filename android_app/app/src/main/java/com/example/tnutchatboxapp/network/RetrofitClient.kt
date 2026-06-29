package com.example.tnutchatboxapp.network

import com.example.tnutchatboxapp.MyApplication
import com.example.tnutchatboxapp.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val AUTH_BASE_URL = "http://10.0.2.2:8000/"
    private const val CHAT_BASE_URL = "http://10.0.2.2:5000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val sessionManager = SessionManager(MyApplication.instance)
        val token = sessionManager.fetchAuthToken()
        val requestBuilder = chain.request().newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(950, TimeUnit.SECONDS)
        .readTimeout(950, TimeUnit.SECONDS)
        .writeTimeout(950, TimeUnit.SECONDS)
        .build()

    val authInstance: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    val chatInstance: ChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)  // Use auth server for chat endpoints
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }
}
