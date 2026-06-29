package com.example.tnutchatboxapp.network

import com.example.tnutchatboxapp.model.*
import retrofit2.Call
import retrofit2.http.*

interface ChatApiService {
    // Chat session management endpoints
    @POST("api/chat/sessions")
    fun createChatSession(@Body sessionCreate: ChatSessionCreate): Call<ChatSessionResponse>

    @GET("api/chat/sessions")
    fun getChatSessions(): Call<ChatSessionsResponse>

    @GET("api/chat/sessions/{session_id}/messages")
    fun getChatHistory(@Path("session_id") sessionId: String): Call<ChatHistoryResponse>

    @POST("api/chat/sessions/{session_id}/messages")
    fun sendMessageToSession(
        @Path("session_id") sessionId: String,
        @Body message: ChatMessageRequest
    ): Call<ChatMessageResponse>

    @POST("api/chat/sessions/{session_id}/regenerate")
    fun regenerateMessage(
        @Path("session_id") sessionId: String,
        @Body message: ChatMessageRequest
    ): Call<ChatMessageResponse>

    @DELETE("api/chat/sessions/{session_id}")
    fun deleteSession(@Path("session_id") sessionId: String): Call<ApiResponse>

    @PUT("api/chat/sessions/{session_id}")
    fun updateSessionTitle(
        @Path("session_id") sessionId: String,
        @Body updateRequest: SessionTitleUpdate
    ): Call<ApiResponse>

    @POST("api/chatbot/query")
    fun sendChatbotQuery(@Body queryRequest: ChatbotQueryRequest): Call<ChatbotQueryResponse>

    // Legacy endpoint for backward compatibility
    @POST("query")
    fun sendMessage(@Body message: Message): Call<ResponseMessage>
}