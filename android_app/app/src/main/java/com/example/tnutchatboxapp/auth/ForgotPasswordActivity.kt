package com.example.tnutchatboxapp.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityForgotPasswordBinding
import com.example.tnutchatboxapp.model.PasswordReset
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityForgotPasswordBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.buttonSendCode.setOnClickListener {
            sendResetCode()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun sendResetCode() {
        val email = binding.editTextEmail.text.toString().trim()
        
        // Validate email
        if (!validateEmail(email)) {
            return
        }
        
        showLoading(true)
        
        val passwordReset = PasswordReset(email)
        
        RetrofitClient.authInstance.forgotPassword(passwordReset).enqueue(object : Callback<com.example.tnutchatboxapp.model.ApiResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.ApiResponse>, response: Response<com.example.tnutchatboxapp.model.ApiResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        // Show success message and go back to login
                        showSuccess(apiResponse.message)
                        // Delay for user to read the message, then go back to login
                        binding.buttonSendCode.postDelayed({
                            finish()
                        }, 3000)
                    } else {
                        showError("Không thể đặt lại mật khẩu")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        404 -> "Email không tồn tại trong hệ thống"
                        400 -> "Email chưa được xác thực. Vui lòng xác thực email trước khi đặt lại mật khẩu"
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
    }
    
    private fun validateEmail(email: String): Boolean {
        binding.inputLayoutEmail.error = null
        
        val emailError = ValidationUtils.getEmailValidationError(email)
        if (emailError != null) {
            binding.inputLayoutEmail.error = emailError
            return false
        }
        
        return true
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonSendCode.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
