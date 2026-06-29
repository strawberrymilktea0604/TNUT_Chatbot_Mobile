package com.example.tnutchatboxapp.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidGmail(email: String): Boolean {
        return isValidEmail(email) && email.endsWith("@gmail.com", ignoreCase = true)
    }
    
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() && username.length >= 3 && username.length <= 30
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.isNotBlank() && password.length >= 6
    }
    
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    
    fun isValidVerificationCode(code: String): Boolean {
        return code.isNotBlank() && code.length == 6 && code.all { it.isDigit() }
    }
    
    fun getEmailValidationError(email: String): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            !isValidEmail(email) -> "Email không đúng định dạng"
            !isValidGmail(email) -> "Chỉ chấp nhận email Gmail"
            else -> null
        }
    }
    
    fun getUsernameValidationError(username: String): String? {
        return when {
            username.isBlank() -> "Tên đăng nhập không được để trống"
            username.length < 3 -> "Tên đăng nhập phải có ít nhất 3 ký tự"
            username.length > 30 -> "Tên đăng nhập không được quá 30 ký tự"
            else -> null
        }
    }
    
    fun getPasswordValidationError(password: String): String? {
        return when {
            password.isBlank() -> "Mật khẩu không được để trống"
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
            else -> null
        }
    }
    
    fun getConfirmPasswordError(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Xác nhận mật khẩu không được để trống"
            !doPasswordsMatch(password, confirmPassword) -> "Mật khẩu xác nhận không khớp"
            else -> null
        }
    }
    
    fun getVerificationCodeError(code: String): String? {
        return when {
            code.isBlank() -> "Mã xác nhận không được để trống"
            code.length != 6 -> "Mã xác nhận phải có 6 chữ số"
            !code.all { it.isDigit() } -> "Mã xác nhận chỉ được chứa số"
            else -> null
        }
    }
}
