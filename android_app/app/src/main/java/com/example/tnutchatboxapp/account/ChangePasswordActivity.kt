package com.example.tnutchatboxapp.account

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityChangePasswordBinding
import com.example.tnutchatboxapp.model.ApiResponse
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.SessionManager
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Đổi mật khẩu"
        }
    }
    
    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        binding.confirmButton.setOnClickListener {
            validateAndChangePassword()
        }
    }
    
    private fun validateAndChangePassword() {
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        
        // Reset error states
        binding.currentPasswordLayout.error = null
        binding.newPasswordLayout.error = null
        binding.confirmPasswordLayout.error = null
        
        // Validate input
        when {
            currentPassword.isEmpty() -> {
                binding.currentPasswordLayout.error = "Vui lòng nhập mật khẩu hiện tại"
                binding.currentPasswordEditText.requestFocus()
                return
            }
            
            newPassword.isEmpty() -> {
                binding.newPasswordLayout.error = "Vui lòng nhập mật khẩu mới"
                binding.newPasswordEditText.requestFocus()
                return
            }
            
            !ValidationUtils.isValidPassword(newPassword) -> {
                binding.newPasswordLayout.error = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số"
                binding.newPasswordEditText.requestFocus()
                return
            }
            
            confirmPassword.isEmpty() -> {
                binding.confirmPasswordLayout.error = "Vui lòng xác nhận mật khẩu mới"
                binding.confirmPasswordEditText.requestFocus()
                return
            }
            
            newPassword != confirmPassword -> {
                binding.confirmPasswordLayout.error = "Mật khẩu xác nhận không khớp"
                binding.confirmPasswordEditText.requestFocus()
                return
            }
            
            currentPassword == newPassword -> {
                binding.newPasswordLayout.error = "Mật khẩu mới phải khác mật khẩu hiện tại"
                binding.newPasswordEditText.requestFocus()
                return
            }
        }
        
        // Call API to change password
        changePassword(currentPassword, newPassword)
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        setLoading(true)
        
        val request = ChangePasswordRequest(currentPassword, newPassword)
        
        RetrofitClient.authInstance.changePassword(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                setLoading(false)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Mật khẩu hiện tại không đúng"
                        401 -> "Phiên đăng nhập hết hạn"
                        500 -> "Lỗi server, vui lòng thử lại sau"
                        else -> "Đổi mật khẩu thất bại"
                    }
                    
                    if (response.code() == 400) {
                        binding.currentPasswordLayout.error = errorMessage
                    } else {
                        Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@ChangePasswordActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.confirmButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading
        
        binding.currentPasswordEditText.isEnabled = !isLoading
        binding.newPasswordEditText.isEnabled = !isLoading
        binding.confirmPasswordEditText.isEnabled = !isLoading
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
