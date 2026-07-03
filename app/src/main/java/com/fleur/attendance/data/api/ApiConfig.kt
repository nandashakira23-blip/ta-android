package com.fleur.attendance.data.api

import com.fleur.attendance.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    private val BASE_URL: String by lazy {
        val raw = BuildConfig.API_BASE_URL.trim()
        if (raw.endsWith("/")) raw else "$raw/"
    }
    
    private var authToken: String? = null
    
    fun getBaseUrl(): String {
        return BASE_URL
    }
    
    /**
     * Set JWT token for authenticated requests
     */
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    /**
     * Get current auth token
     */
    fun getAuthToken(): String? {
        return authToken
    }
    
    /**
     * Clear auth token (for logout)
     */
    fun clearAuthToken() {
        authToken = null
    }
    
    /**
     * Create auth interceptor for adding JWT token to requests
     * Also handles automatic token refresh on 401/403 responses
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Skip adding token if Authorization header already exists
            if (originalRequest.header("Authorization") != null) {
                return@Interceptor chain.proceed(originalRequest)
            }
            
            // Add token if available
            val newRequest = if (authToken != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $authToken")
                    .build()
            } else {
                originalRequest
            }
            
            val response = chain.proceed(newRequest)
            
            // If we get 401 or 403, try to refresh token and retry once
            if ((response.code == 401 || response.code == 403) && authToken != null) {
                
                // Try to refresh token synchronously
                val refreshSuccess = tryRefreshTokenSync()
                
                if (refreshSuccess && authToken != null) {
                    // Tutup response lama HANYA saat benar-benar akan retry.
                    // (Jangan menutup lalu mengembalikan response yang sama — interceptor
                    //  logging di atasnya akan crash: java.lang.IllegalStateException: closed)
                    response.close()
                    // Retry the original request with new token
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $authToken")
                        .build()
                    return@Interceptor chain.proceed(retryRequest)
                }
            }
            
            response
        }
    }
    
    /**
     * Synchronously refresh token (for use in interceptor)
     */
    private fun tryRefreshTokenSync(): Boolean {
        return try {
            // This is a simplified approach - in production you might want to use a more sophisticated method
            // For now, we'll just clear the token and let the user re-login
            clearAuthToken()
            false
        } catch (e: Exception) {
            false
        }
    }
    
    fun getApiService(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(createAuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
}
