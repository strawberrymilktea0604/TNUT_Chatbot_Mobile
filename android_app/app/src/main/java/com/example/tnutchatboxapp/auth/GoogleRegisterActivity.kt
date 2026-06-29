package com.example.tnutchatboxapp.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.MainActivity
import com.example.tnutchatboxapp.databinding.ActivityGoogleRegisterBinding
import com.example.tnutchatboxapp.model.AuthResponse
import com.example.tnutchatboxapp.model.GoogleAuthManual
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.SessionManager
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GoogleRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleRegisterBinding
    private lateinit var sessionManager: SessionManager
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        
        // Get email from intent
        userEmail = intent.getStringExtra("email") ?: ""
        if (userEmail.isEmpty()) {
            showError("Lỗi: Không nhận được thông tin email")
            finish()
            return
        }

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        binding.textViewEmail.text = userEmail
    }

    private fun setupClickListeners() {
        binding.buttonRegister.setOnClickListener {
            performRegister()
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        if (!validateInputs(username, password, confirmPassword)) {
            return
        }

        showLoading(true)

        val googleAuthManual = GoogleAuthManual(userEmail, username, password, confirmPassword)

        RetrofitClient.authInstance.googleRegister(googleAuthManual).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.accessToken != null) {
                        // Save auth token
                        sessionManager.saveAuthToken(authResponse.accessToken)
                        
                        showSuccess("Đăng ký thành công!")
                        navigateToMain()
                    } else {
                        showError("Đăng ký thất bại: Phản hồi không hợp lệ")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        409 -> {
                            try {
                                val errorBody = response.errorBody()?.string()
                                if (errorBody?.contains("Username already exists") == true) {
                                    "Tên đăng nhập đã tồn tại"
                                } else if (errorBody?.contains("Email already exists") == true) {
                                    "Email đã được sử dụng"
                                } else {
                                    "Tài khoản đã tồn tại"
                                }
                            } catch (e: Exception) {
                                "Tài khoản đã tồn tại"
                            }
                        }
                        400 -> "Thông tin đăng ký không hợp lệ"
                        else -> "Lỗi đăng ký: ${response.code()}"
                    }
                    showError(errorMessage)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                showLoading(false)
                showError("Lỗi kết nối: ${t.message}")
            }
        })
    }
    
    private fun validateInputs(username: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.inputLayoutUsername.error = null
        binding.inputLayoutPassword.error = null
        binding.inputLayoutConfirmPassword.error = null
        
        // Validate username
        val usernameError = ValidationUtils.getUsernameValidationError(username)
        if (usernameError != null) {
            binding.inputLayoutUsername.error = usernameError
            isValid = false
        }
        
        // Validate password
        val passwordError = ValidationUtils.getPasswordValidationError(password)
        if (passwordError != null) {
            binding.inputLayoutPassword.error = passwordError
            isValid = false
        }
        
        // Validate confirm password
        val confirmPasswordError = ValidationUtils.getConfirmPasswordError(password, confirmPassword)
        if (confirmPasswordError != null) {
            binding.inputLayoutConfirmPassword.error = confirmPasswordError
            isValid = false
        }
        
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
