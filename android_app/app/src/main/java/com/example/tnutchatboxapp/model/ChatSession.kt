package com.example.tnutchatboxapp.model

import java.text.SimpleDateFormat
import java.util.*

data class ChatSession(
    val id: String,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val messageCount: Int = 0,
    val lastMessage: String = ""
) {
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(updated_at)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            updated_at
        }
    }
    
    fun getPreviewText(): String {
        return if (lastMessage.isNotEmpty()) {
            if (lastMessage.length > 50) {
                lastMessage.substring(0, 50) + "..."
            } else {
                lastMessage
            }
        } else {
            "Đoạn chat trống"
        }
    }
}

data class ChatSessionCreate(
    val title: String
)

data class ChatSessionResponse(
    val message: String,
    val session_id: String,
    val title: String
)

data class ChatSessionsResponse(
    val sessions: List<ChatSession>
)
