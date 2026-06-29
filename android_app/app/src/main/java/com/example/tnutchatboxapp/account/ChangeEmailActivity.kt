package com.example.tnutchatboxapp.account

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityChangeEmailBinding
import com.example.tnutchatboxapp.model.ApiResponse
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.JWTUtils
import com.example.tnutchatboxapp.utils.SessionManager
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeEmailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChangeEmailBinding
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupUI()
        setupClickListeners()
        loadCurrentEmail()
    }
    
    private fun setupUI() {
        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Đổi email"
        }
    }
    
    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        binding.sendCodeButton.setOnClickListener {
            validateAndSendCode()
        }
    }
    
    private fun loadCurrentEmail() {
        val token = sessionManager.fetchAuthToken()
        val currentEmail = token?.let { JWTUtils.getEmail(it) }
        
        if (currentEmail != null) {
            binding.currentEmailEditText.setText(currentEmail)
        } else {
            Toast.makeText(this, "Không thể lấy thông tin email hiện tại", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun validateAndSendCode() {
        val newEmail = binding.newEmailEditText.text.toString().trim()
        val currentEmail = binding.currentEmailEditText.text.toString().trim()
        
        // Reset error state
        binding.newEmailLayout.error = null
        
        // Validate input
        when {
            newEmail.isEmpty() -> {
                binding.newEmailLayout.error = "Vui lòng nhập email mới"
                binding.newEmailEditText.requestFocus()
                return
            }
            
            !ValidationUtils.isValidEmail(newEmail) -> {
                binding.newEmailLayout.error = "Email không hợp lệ"
                binding.newEmailEditText.requestFocus()
                return
            }
            
            newEmail == currentEmail -> {
                binding.newEmailLayout.error = "Email mới phải khác email hiện tại"
                binding.newEmailEditText.requestFocus()
                return
            }
        }
        
        // Send verification code to new email
        sendVerificationCode(newEmail)
    }
    
    private fun sendVerificationCode(newEmail: String) {
        setLoading(true)
        
        val request = ChangeEmailRequest(newEmail)
        
        RetrofitClient.authInstance.sendChangeEmailCode(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                setLoading(false)
                
                if (response.isSuccessful) {
                    // Navigate to verification screen
                    val intent = Intent(this@ChangeEmailActivity, ChangeEmailVerificationActivity::class.java)
                    intent.putExtra("new_email", newEmail)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Email đã được sử dụng bởi tài khoản khác"
                        401 -> "Phiên đăng nhập hết hạn"
                        500 -> "Lỗi server, vui lòng thử lại sau"
                        else -> "Gửi mã xác nhận thất bại"
                    }
                    
                    if (response.code() == 400) {
                        binding.newEmailLayout.error = errorMessage
                    } else {
                        Toast.makeText(this@ChangeEmailActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@ChangeEmailActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.sendCodeButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading
        binding.newEmailEditText.isEnabled = !isLoading
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
