package com.example.tnutchatboxapp.model

import com.google.gson.annotations.SerializedName

// Chat Models
data class ChatMessageRequest(
    val content: String
)

data class ChatMessageResponse(
    @SerializedName("user_message")
    val userMessage: String,
    @SerializedName("ai_response")
    val aiResponse: String,
    @SerializedName("session_id")
    val sessionId: String,
    val regenerated: Boolean = false
)

data class ChatHistory(
    @SerializedName("message_id")
    val messageId: String? = null,
    val content: String,
    @SerializedName("message_type")
    val messageType: String, // "user" or "assistant"
    val timestamp: String,
    @SerializedName("is_regenerated")
    val isRegenerated: Boolean = false,
    @SerializedName("session_message_id")
    val sessionMessageId: Int? = null
) {
    val isUser: Boolean
        get() = messageType == "user"
}

data class ChatHistoryResponse(
    @SerializedName("session_id")
    val sessionId: String,
    val messages: List<ChatHistory>
)

data class SessionTitleUpdate(
    val title: String
)

data class ChatbotQueryRequest(
    val question: String,
    @SerializedName("session_id")
    val sessionId: String
)

data class ChatbotQueryResponse(
    val answer: String,
    @SerializedName("query_time")
    val queryTime: Float
)
