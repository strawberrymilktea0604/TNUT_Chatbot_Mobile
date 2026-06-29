package com.example.tnutchatboxapp.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.tnutchatboxapp.WelcomeActivity
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandler {
    companion object {
        private const val TAG = "ErrorHandler"
        
        /**
         * Global exception handler để catch unhandled exceptions
         */
        fun setupGlobalExceptionHandler(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
                
                // Log crash details
                logCrashDetails(exception)
                
                // Show crash dialog và restart app
                showCrashDialog(context, exception)
            }
        }
        
        /**
         * Handle network errors
         */
        fun handleNetworkError(context: Context, error: Throwable, retry: (() -> Unit)? = null) {
            when (error) {
                is UnknownHostException -> {
                    showErrorDialog(
                        context,
                        "Lỗi kết nối",
                        "Không thể kết nối đến server. Vui lòng kiểm tra kết nối internet.",
                        retry
                    )
                }
                is SocketTimeoutException -> {
                    showErrorDialog(
                        context,
                        "Timeout",
                        "Kết nối quá chậm. Vui lòng thử lại.",
                        retry
                    )
                }
                else -> {
                    showErrorDialog(
                        context,
                        "Lỗi mạng",
                        "Có lỗi xảy ra: ${error.message}",
                        retry
                    )
                }
            }
        }
        
        /**
         * Handle API errors
         */
        fun handleApiError(context: Context, code: Int, message: String) {
            when (code) {
                400 -> showErrorToast(context, "Dữ liệu không hợp lệ: $message")
                401 -> {
                    showErrorToast(context, "Phiên đăng nhập hết hạn")
                    redirectToLogin(context)
                }
                403 -> showErrorToast(context, "Không có quyền truy cập")
                404 -> showErrorToast(context, "Không tìm thấy dữ liệu")
                500 -> showErrorToast(context, "Lỗi server. Vui lòng thử lại sau")
                else -> showErrorToast(context, "Lỗi: $message")
            }
        }
        
        /**
         * Handle validation errors
         */
        fun handleValidationError(context: Context, field: String, message: String) {
            showErrorDialog(
                context,
                "Lỗi nhập liệu",
                "$field: $message"
            )
        }
        
        /**
         * Check network connectivity
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                   networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
        
        /**
         * Safe API call với error handling
         */
        suspend fun <T> safeApiCall(
            context: Context,
            apiCall: suspend () -> T
        ): Result<T> {
            return try {
                if (!isNetworkAvailable(context)) {
                    return Result.failure(Exception("Không có kết nối internet"))
                }
                
                val result = apiCall()
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                Result.failure(e)
            }
        }
        
        /**
         * Show error dialog
         */
        private fun showErrorDialog(
            context: Context,
            title: String,
            message: String,
            retry: (() -> Unit)? = null
        ) {
            if (context is Activity && context.isFinishing) return
            
            val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            
            if (retry != null) {
                builder.setNeutralButton("Thử lại") { dialog, _ ->
                    dialog.dismiss()
                    retry()
                }
            }
            
            try {
                builder.show()
            } catch (e: Exception) {
                Log.e(TAG, "Cannot show dialog", e)
                showErrorToast(context, message)
            }
        }
        
        /**
         * Show error toast
         */
        private fun showErrorToast(context: Context, message: String) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Cannot show toast: $message", e)
            }
        }
        
        /**
         * Show crash dialog và restart app
         */
        private fun showCrashDialog(context: Context, exception: Throwable) {
            try {
                val intent = Intent(context, WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                
                if (context is Activity) {
                    context.finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot restart app", e)
                System.exit(1)
            }
        }
        
        /**
         * Redirect to login khi session hết hạn
         */
        private fun redirectToLogin(context: Context) {
            try {
                // Clear user data
                PreferenceManager(context).clearUserData()
                
                val intent = Intent(context, WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                
                if (context is Activity) {
                    context.finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot redirect to login", e)
            }
        }
        
        /**
         * Log crash details
         */
        private fun logCrashDetails(exception: Throwable) {
            Log.e(TAG, "=== CRASH REPORT ===")
            Log.e(TAG, "Exception: ${exception.javaClass.simpleName}")
            Log.e(TAG, "Message: ${exception.message}")
            Log.e(TAG, "Stack trace:", exception)
            Log.e(TAG, "=== END CRASH REPORT ===")
        }
    }
}

/**
 * Application class để setup global error handling
 */
class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Setup global exception handler
        ErrorHandler.setupGlobalExceptionHandler(this)
        
        // Setup activity lifecycle callbacks để track app state
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Log.d("ActivityLifecycle", "Created: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityStarted(activity: Activity) {
                Log.d("ActivityLifecycle", "Started: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityResumed(activity: Activity) {
                Log.d("ActivityLifecycle", "Resumed: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityPaused(activity: Activity) {
                Log.d("ActivityLifecycle", "Paused: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityStopped(activity: Activity) {
                Log.d("ActivityLifecycle", "Stopped: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Log.d("ActivityLifecycle", "SaveInstanceState: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityDestroyed(activity: Activity) {
                Log.d("ActivityLifecycle", "Destroyed: ${activity.javaClass.simpleName}")
            }
        })
    }
}
