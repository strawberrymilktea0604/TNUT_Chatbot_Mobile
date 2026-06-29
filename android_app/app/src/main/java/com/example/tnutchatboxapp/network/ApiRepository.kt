package com.example.tnutchatboxapp.network

import android.content.Context
import com.example.tnutchatboxapp.model.ChatbotQueryRequest
import com.example.tnutchatboxapp.model.ChatbotQueryResponse
import com.example.tnutchatboxapp.model.*
import com.example.tnutchatboxapp.account.ChangePasswordRequest
import com.example.tnutchatboxapp.account.ChangeEmailRequest
import com.example.tnutchatboxapp.account.VerifyChangeEmailRequest
import com.example.tnutchatboxapp.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.awaitResponse

class ApiRepository(private val context: Context) {
    private val authApiService = RetrofitClient.authInstance
    private val chatApiService = RetrofitClient.chatInstance
    
    // Auth methods với error handling
    suspend fun register(userRegistration: UserRegistration): Result<AuthResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.register(userRegistration).awaitResponse()
            handleResponse(response)
        }
    }
    
    
    suspend fun login(userLogin: UserLogin): Result<AuthResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.login(userLogin).awaitResponse()
            handleResponse(response)
        }
    }
    
    suspend fun googleAuth(googleAuth: GoogleAuth): Result<AuthResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.googleAuth(googleAuth).awaitResponse()
            handleResponse(response)
        }
    }
    
    
    suspend fun forgotPassword(passwordReset: PasswordReset): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.forgotPassword(passwordReset).awaitResponse()
            handleResponse(response)
        }
    }
    
    // DEPRECATED: These methods are no longer used with the new password reset flow
    /*
    suspend fun verifyResetCode(verification: PasswordResetVerification): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.verifyResetCode(verification).awaitResponse()
            handleResponse(response)
        }
    }
    
    suspend fun resetPassword(passwordReset: PasswordResetVerification): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.resetPassword(passwordReset).awaitResponse()
            handleResponse(response)
        }
    }
    */
    
    suspend fun changePassword(token: String, passwordChange: PasswordChange): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            // Convert model PasswordChange to account ChangePasswordRequest
            val request = ChangePasswordRequest(
                passwordChange.currentPassword,
                passwordChange.newPassword
            )
            val response = authApiService.changePassword(request).awaitResponse()
            handleResponse(response)
        }
    }
    
    suspend fun changeEmail(token: String, emailChange: EmailChange): Result<VerificationCodeResponse> {
        return ErrorHandler.safeApiCall(context) {
            // Convert model EmailChange to account ChangeEmailRequest
            val request = ChangeEmailRequest(emailChange.newEmail)
            val response = authApiService.sendChangeEmailCode(request).awaitResponse()
            // Convert ApiResponse to VerificationCodeResponse
            val apiResponse = handleResponse(response)
            VerificationCodeResponse(apiResponse.message, true)
        }
    }
    
    suspend fun verifyEmailChange(token: String, verification: EmailVerification): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            // Convert model EmailVerification to account VerifyChangeEmailRequest
            val request = VerifyChangeEmailRequest(
                verification.email,
                verification.verificationCode
            )
            val response = authApiService.verifyChangeEmail(request).awaitResponse()
            // The response is AuthResponse, need to convert to ApiResponse
            val authResponse = response.body()
            if (response.isSuccessful && authResponse != null) {
                ApiResponse("Email changed successfully")
            } else {
                throw Exception(response.errorBody()?.string() ?: "Unknown error")
            }
        }
    }
    
    suspend fun getProfile(token: String): Result<UserProfile> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.getProfile().awaitResponse()
            handleResponse(response)
        }
    }
    
    suspend fun logout(token: String): Result<ApiResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = authApiService.logout().awaitResponse()
            handleResponse(response)
        }
    }
    
    // Chat methods
    suspend fun sendMessage(message: Message): Result<ResponseMessage> {
        return ErrorHandler.safeApiCall(context) {
            val response = chatApiService.sendMessage(message).awaitResponse()
            handleResponse(response)
        }
    }
    
    suspend fun getChatHistory(token: String, sessionId: String): Result<ChatHistoryResponse> {
        return ErrorHandler.safeApiCall(context) {
            val response = chatApiService.getChatHistory(sessionId).awaitResponse()
            handleResponse(response)
        }
    }
    
    // Chatbot methods
    suspend fun sendChatbotQuery(token: String, question: String, sessionId: String): Result<ChatbotQueryResponse> {
        return ErrorHandler.safeApiCall(context) {
            val queryRequest = ChatbotQueryRequest(question, sessionId)
            val response = chatApiService.sendChatbotQuery(queryRequest).awaitResponse()
            handleResponse(response)
        }
    }
    
    
    // Helper method để handle HTTP responses
    private fun <T> handleResponse(response: Response<T>): T {
        return if (response.isSuccessful) {
            response.body() ?: throw Exception("Empty response body")
        } else {
            val errorMessage = response.errorBody()?.string() ?: "Unknown error"
            
            // Handle specific HTTP status codes
            when (response.code()) {
                400 -> throw Exception("Bad Request: $errorMessage")
                401 -> throw Exception("Unauthorized: $errorMessage")
                403 -> throw Exception("Forbidden: $errorMessage")
                404 -> throw Exception("Not Found: $errorMessage")
                422 -> throw Exception("Validation Error: $errorMessage")
                500 -> throw Exception("Server Error: $errorMessage")
                503 -> throw Exception("Service Unavailable: $errorMessage")
                else -> throw Exception("HTTP ${response.code()}: $errorMessage")
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ApiRepository? = null
        
        fun getInstance(context: Context): ApiRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
