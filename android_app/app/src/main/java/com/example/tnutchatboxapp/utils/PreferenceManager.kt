package com.example.tnutchatboxapp.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "AIChaoBoxAppPrefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FIRST_TIME = "first_time"
        private const val KEY_IS_VERIFIED = "is_verified"
    }

    fun saveUserData(accessToken: String, userId: String, username: String, email: String, isVerified: Boolean) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_VERIFIED, isVerified)
            apply()
        }
    }

    fun getAccessToken(): String? = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)

    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun getEmail(): String? = sharedPreferences.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun isFirstTime(): Boolean = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)

    fun setFirstTime(isFirstTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).apply()
    }

    fun isUserVerified(): Boolean = sharedPreferences.getBoolean(KEY_IS_VERIFIED, false)

    fun setUserVerified(isVerified: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_VERIFIED, isVerified).apply()
    }

    fun updateUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun updateEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }

    fun clearUserData() {
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_IS_VERIFIED)
            apply()
        }
    }

    fun getBearerToken(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }
}
