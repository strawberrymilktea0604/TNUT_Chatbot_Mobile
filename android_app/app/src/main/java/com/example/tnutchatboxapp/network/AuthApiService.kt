package com.example.tnutchatboxapp.network

import com.example.tnutchatboxapp.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    @POST("api/auth/register")
    fun register(@Body userRegistration: UserRegistration): Call<AuthResponse>

    @GET("api/auth/verify-email/{token}")
    fun verifyEmail(@Path("token") token: String): Call<ApiResponse>

    @POST("api/auth/resend-verification")
    fun resendVerificationEmail(@Body request: ResendVerificationRequest): Call<ApiResponse>

    @POST("api/auth/login")
    fun login(@Body userLogin: UserLogin): Call<AuthResponse>

    @POST("api/auth/google-auth")
    fun googleAuth(@Body googleAuth: GoogleAuth): Call<AuthResponse>

    @POST("api/auth/google-register")
    fun googleRegister(@Body googleAuthManual: GoogleAuthManual): Call<AuthResponse>

    @POST("api/auth/forgot-password")
    fun forgotPassword(@Body passwordReset: PasswordReset): Call<ApiResponse>

    // DEPRECATED: These endpoints are no longer used with the new password reset flow
    // @POST("api/auth/verify-reset-code")
    // fun verifyResetCode(@Body verification: PasswordResetVerification): Call<ApiResponse>

    // @POST("api/auth/reset-password")
    // fun resetPassword(@Body passwordReset: PasswordResetVerification): Call<ApiResponse>

    @POST("api/auth/change-password")
    fun changePassword(@Body passwordChange: com.example.tnutchatboxapp.account.ChangePasswordRequest): Call<ApiResponse>

    @POST("api/auth/change-email")
    fun sendChangeEmailCode(@Body emailChange: com.example.tnutchatboxapp.account.ChangeEmailRequest): Call<ApiResponse>

    @POST("api/auth/verify-email-change")
    fun verifyChangeEmail(@Body verification: com.example.tnutchatboxapp.account.VerifyChangeEmailRequest): Call<AuthResponse>

    @GET("api/auth/profile")
    fun getProfile(): Call<UserProfile>

    @GET("api/auth/verify-token")
    fun verifyToken(): Call<AuthResponse>

    @POST("api/auth/logout")
    fun logout(): Call<ApiResponse>
}