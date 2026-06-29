package com.example.tnutchatboxapp.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityResetPasswordBinding
import com.example.tnutchatboxapp.model.PasswordResetVerification
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * DEPRECATED: This activity is no longer used with the new password reset flow.
 * Password reset now sends a new password directly to the user's email.
 */
@Deprecated("Use ForgotPasswordActivity with new direct password reset flow")

class ResetPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityResetPasswordBinding
    private var email: String = ""
    private var verificationCode: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get data from intent
        email = intent.getStringExtra("email") ?: ""
        verificationCode = intent.getStringExtra("verification_code") ?: ""
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.buttonResetPassword.setOnClickListener {
            resetPassword()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun resetPassword() {
        val newPassword = binding.editTextNewPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()
        
        // Validate inputs
        if (!validateInputs(newPassword, confirmPassword)) {
            return
        }
        
        /*
        showLoading(true)
        
        val passwordReset = PasswordResetVerification(email, verificationCode)
        
        RetrofitClient.authInstance.resetPassword(passwordReset).enqueue(object : Callback<com.example.tnutchatboxapp.model.ApiResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.ApiResponse>, response: Response<com.example.tnutchatboxapp.model.ApiResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    showSuccess("Đặt lại mật khẩu thành công!")
                    // Navigate back to login
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Mã xác nhận không hợp lệ hoặc đã hết hạn"
                        404 -> "Email không tồn tại"
                        else -> "Lỗi đặt lại mật khẩu: ${response.code()}"
                    }
                    showError(errorMessage)
                }
            }
            
            override fun onFailure(call: Call<com.example.tnutchatboxapp.model.ApiResponse>, t: Throwable) {
                showLoading(false)
                showError("Lỗi kết nối: ${t.message}")
            }
        })
        */
        
        // DEPRECATED: This activity is no longer used with the new password reset flow
        showError("Tính năng này không còn được hỗ trợ. Vui lòng sử dụng chức năng quên mật khẩu.")
    }
    
    private fun validateInputs(newPassword: String, confirmPassword: String): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.inputLayoutNewPassword.error = null
        binding.inputLayoutConfirmPassword.error = null
        
        // Validate new password
        val passwordError = ValidationUtils.getPasswordValidationError(newPassword)
        if (passwordError != null) {
            binding.inputLayoutNewPassword.error = passwordError
            isValid = false
        }
        
        // Validate confirm password
        val confirmPasswordError = ValidationUtils.getConfirmPasswordError(newPassword, confirmPassword)
        if (confirmPasswordError != null) {
            binding.inputLayoutConfirmPassword.error = confirmPasswordError
            isValid = false
        }
        
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonResetPassword.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
