package com.example.tnutchatboxapp.utils

import com.auth0.android.jwt.JWT

object JWTUtils {

    fun getUsername(token: String): String? {
        return decodeToken(token)?.getClaim("username")?.asString()
    }

    fun getEmail(token: String): String? {
        return decodeToken(token)?.getClaim("email")?.asString()
    }

    fun isUserVerified(token: String): Boolean {
        return decodeToken(token)?.getClaim("is_verified")?.asBoolean() ?: false
    }

    private fun decodeToken(token: String): JWT? {
        return try {
            JWT(token)
        } catch (e: Exception) {
            // Log error or handle invalid token
            e.printStackTrace()
            null
        }
    }
}