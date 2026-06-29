package com.example.tnutchatboxapp.model

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isFailed: Boolean = false,
    val messageId: String? = null,
    val isTyping: Boolean = false,
    val sessionMessageId: Int? = null,  // For tracking session-specific message order
    val isFromCurrentSession: Boolean = true  // To distinguish session messages from historical
) {
    companion object {
        fun createUserMessage(text: String): ChatMessage {
            return ChatMessage(text, true)
        }
        
        fun createBotMessage(text: String): ChatMessage {
            return ChatMessage(text, false)
        }
        
        fun createFailedBotMessage(text: String): ChatMessage {
            return ChatMessage(text, false, isFailed = true)
        }
        
        fun createLoadingMessage(): ChatMessage {
            return ChatMessage("", false, System.currentTimeMillis(), true)
        }
        
        fun createTypingMessage(text: String): ChatMessage {
            return ChatMessage(text, false, isTyping = true)
        }
    }
}
