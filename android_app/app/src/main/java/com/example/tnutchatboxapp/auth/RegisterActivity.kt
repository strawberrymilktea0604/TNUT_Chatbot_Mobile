package com.example.tnutchatboxapp.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityRegisterBinding
import com.example.tnutchatboxapp.MainActivity
import com.example.tnutchatboxapp.model.AuthResponse
import com.example.tnutchatboxapp.model.UserRegistration
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.PreferenceManager
import com.example.tnutchatboxapp.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonRegister.setOnClickListener {
            performRegister()
        }

        binding.textViewLogin.setOnClickListener {
            finish()
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val username = binding.editTextUsername.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        if (!validateInputs(username, email, password, confirmPassword)) {
            return
        }

        showLoading(true)

        val userRegistration = UserRegistration(username, email, password, confirmPassword)

        RetrofitClient.authInstance.register(userRegistration).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.accessToken != null) {
                        preferenceManager.saveUserData(
                            authResponse.accessToken,
                            authResponse.userId ?: "",
                            authResponse.username ?: username,
                            authResponse.email ?: email,
                            authResponse.isVerified ?: false
                        )
                        showSuccess("Đăng ký thành công! Vui lòng xác nhận email của bạn.")
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
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
    
    private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.inputLayoutUsername.error = null
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
        binding.inputLayoutConfirmPassword.error = null
        
        // Validate username
        val usernameError = ValidationUtils.getUsernameValidationError(username)
        if (usernameError != null) {
            binding.inputLayoutUsername.error = usernameError
            isValid = false
        }
        
        // Validate email
        val emailError = ValidationUtils.getEmailValidationError(email)
        if (emailError != null) {
            binding.inputLayoutEmail.error = emailError
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
