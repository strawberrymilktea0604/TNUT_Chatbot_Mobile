package com.example.tnutchatboxapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tnutchatboxapp.MainActivity
import com.example.tnutchatboxapp.R
import com.example.tnutchatboxapp.databinding.ActivityLoginBinding
import com.example.tnutchatboxapp.model.UserLogin
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.SessionManager
import com.example.tnutchatboxapp.utils.ValidationUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            sessionManager = SessionManager(this)
            setupGoogleSignIn()
            setupClickListeners()
        } catch (e: Exception) {
            // Log the error and show a message
            android.util.Log.e("LoginActivity", "Error in onCreate: ${e.message}", e)
            showError("Lỗi khởi tạo ứng dụng: ${e.message}")
        }
    }
    
    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            android.util.Log.d("LoginActivity", "Google Sign-In setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Error setting up Google Sign-In: ${e.message}", e)
            showError("Lỗi thiết lập Google Sign-In: ${e.message}")
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }
        
        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        binding.textViewForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        
        binding.buttonGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    private fun performLogin() {
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        
        // Validate inputs
        if (!validateInputs(username, password)) {
            return
        }
        
        showLoading(true)
        
        val userLogin = UserLogin(username, password)
        
        RetrofitClient.authInstance.login(userLogin).enqueue(object : Callback<com.example.tnutchatboxapp.model.AuthResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, response: Response<com.example.tnutchatboxapp.model.AuthResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.accessToken != null) {
                        // Save auth token
                        sessionManager.saveAuthToken(authResponse.accessToken)
                        
                        // Navigate to main activity
                        navigateToMain()
                    } else {
                        showError("Đăng nhập thất bại")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Sai tên đăng nhập hoặc mật khẩu"
                        401 -> "Thông tin đăng nhập không đúng"
                        404 -> "Tài khoản không tồn tại"
                        else -> "Lỗi đăng nhập: ${response.code()}"
                    }
                    showError(errorMessage)
                }
            }
            
            override fun onFailure(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, t: Throwable) {
                showLoading(false)
                showError("Lỗi kết nối: ${t.message}")
            }
        })
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                showError("Google đăng nhập thất bại: ${e.message}")
            }
        }
    }
    
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        showLoading(true)
        
        val googleAuth = com.example.tnutchatboxapp.model.GoogleAuth(acct.idToken!!)
        
        RetrofitClient.authInstance.googleAuth(googleAuth).enqueue(object : Callback<com.example.tnutchatboxapp.model.AuthResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, response: Response<com.example.tnutchatboxapp.model.AuthResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.accessToken != null) {
                        // Save auth token
                        sessionManager.saveAuthToken(authResponse.accessToken)
                        
                        navigateToMain()
                    } else {
                        showError("Đăng nhập Google thất bại")
                    }
                } else {
                    // If user doesn't exist, navigate to Google Register
                    if (response.code() == 404 || response.code() == 400) {
                        val email = acct.email
                        if (email != null) {
                            navigateToGoogleRegister(email)
                        } else {
                            showError("Không thể lấy thông tin email từ Google")
                        }
                    } else {
                        showError("Lỗi đăng nhập Google: ${response.code()} - ${response.message()}")
                    }
                }
            }
            
            override fun onFailure(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, t: Throwable) {
                showLoading(false)
                showError("Lỗi kết nối Google: ${t.message}")
            }
        })
    }
    
    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true
        
        binding.inputLayoutUsername.error = null
        binding.inputLayoutPassword.error = null
        
        if (username.isBlank()) {
            binding.inputLayoutUsername.error = "Tên đăng nhập không được để trống"
            isValid = false
        }
        
        if (password.isBlank()) {
            binding.inputLayoutPassword.error = "Mật khẩu không được để trống"
            isValid = false
        }
        
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !show
        binding.buttonGoogleSignIn.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToGoogleRegister(email: String) {
        val intent = Intent(this, GoogleRegisterActivity::class.java)
        intent.putExtra("email", email)
        startActivity(intent)
    }
}
