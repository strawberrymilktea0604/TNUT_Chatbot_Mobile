package com.example.tnutchatboxapp.account

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.databinding.ActivityChangeEmailVerificationBinding
import com.example.tnutchatboxapp.model.ApiResponse
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeEmailVerificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChangeEmailVerificationBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var newEmail: String
    private lateinit var codeInputs: List<EditText>
    private var countDownTimer: CountDownTimer? = null
    private var resendCooldown = 60
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        newEmail = intent.getStringExtra("new_email") ?: ""
        
        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin email", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        setupClickListeners()
        setupCodeInputs()
        startResendCooldown()
    }
    
    private fun setupUI() {
        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Xác nhận email"
        }
        
        // Mask email for display
        val maskedEmail = maskEmail(newEmail)
        binding.emailTextView.text = maskedEmail
    }
    
    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        binding.verifyButton.setOnClickListener {
            verifyCode()
        }
        
        binding.resendCodeTextView.setOnClickListener {
            if (binding.resendCodeTextView.text.toString().contains("Gửi lại")) {
                resendCode()
            }
        }
    }
    
    private fun setupCodeInputs() {
        codeInputs = listOf(
            binding.codeDigit1,
            binding.codeDigit2,
            binding.codeDigit3,
            binding.codeDigit4,
            binding.codeDigit5,
            binding.codeDigit6
        )
        
        codeInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < codeInputs.size - 1) {
                        codeInputs[index + 1].requestFocus()
                    }
                    
                    // Auto verify when all digits are entered
                    if (index == codeInputs.size - 1 && s?.length == 1) {
                        val code = getEnteredCode()
                        if (code.length == 6) {
                            verifyCode()
                        }
                    }
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })
            
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        codeInputs[index - 1].requestFocus()
                        codeInputs[index - 1].setText("")
                    }
                }
                false
            }
            
            // Handle IME action for last input
            if (index == codeInputs.size - 1) {
                editText.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        verifyCode()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
    
    private fun getEnteredCode(): String {
        return codeInputs.joinToString("") { it.text.toString() }
    }
    
    private fun clearCode() {
        codeInputs.forEach { it.setText("") }
        codeInputs[0].requestFocus()
    }
    
    private fun verifyCode() {
        val code = getEnteredCode()
        
        if (code.length != 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ 6 số", Toast.LENGTH_SHORT).show()
            return
        }
        
        setLoading(true)
        
        val request = VerifyChangeEmailRequest(newEmail, code)
        
        RetrofitClient.authInstance.verifyChangeEmail(request).enqueue(object : Callback<com.example.tnutchatboxapp.model.AuthResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, response: Response<com.example.tnutchatboxapp.model.AuthResponse>) {
                setLoading(false)
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    // Update token with new email info
                    authResponse.accessToken?.let {
                        sessionManager.saveAuthToken(it)
                    }
                    
                    Toast.makeText(this@ChangeEmailVerificationActivity, "Đổi email thành công", Toast.LENGTH_SHORT).show()
                    
                    // Close all change email related activities
                    setResult(RESULT_OK)
                    finish()
                    
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Mã xác nhận không đúng"
                        401 -> "Phiên đăng nhập hết hạn"
                        410 -> "Mã xác nhận đã hết hạn"
                        500 -> "Lỗi server, vui lòng thử lại sau"
                        else -> "Xác thực thất bại"
                    }
                    
                    Toast.makeText(this@ChangeEmailVerificationActivity, errorMessage, Toast.LENGTH_LONG).show()
                    
                    if (response.code() == 400 || response.code() == 410) {
                        clearCode()
                    }
                }
            }
            
            override fun onFailure(call: Call<com.example.tnutchatboxapp.model.AuthResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@ChangeEmailVerificationActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun resendCode() {
        setLoading(true)
        
        val request = ChangeEmailRequest(newEmail)
        
        RetrofitClient.authInstance.sendChangeEmailCode(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                setLoading(false)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangeEmailVerificationActivity, "Mã xác nhận đã được gửi lại", Toast.LENGTH_SHORT).show()
                    clearCode()
                    startResendCooldown()
                } else {
                    Toast.makeText(this@ChangeEmailVerificationActivity, "Gửi lại mã thất bại", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@ChangeEmailVerificationActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun startResendCooldown() {
        resendCooldown = 60
        
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                resendCooldown = (millisUntilFinished / 1000).toInt()
                binding.resendCodeTextView.text = "Gửi lại ($resendCooldown)"
                binding.resendCodeTextView.isEnabled = false
            }
            
            override fun onFinish() {
                binding.resendCodeTextView.text = "Gửi lại"
                binding.resendCodeTextView.isEnabled = true
            }
        }
        countDownTimer?.start()
    }
    
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.verifyButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading
        codeInputs.forEach { it.isEnabled = !isLoading }
    }
    
    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return email
        
        val username = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        
        val maskedUsername = when {
            username.length <= 2 -> username
            username.length <= 4 -> username.take(2) + "*".repeat(username.length - 2)
            else -> username.take(2) + "*".repeat(username.length - 4) + username.takeLast(2)
        }
        
        return maskedUsername + domain
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
