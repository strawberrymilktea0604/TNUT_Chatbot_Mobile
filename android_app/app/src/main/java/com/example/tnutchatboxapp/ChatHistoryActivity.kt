package com.example.tnutchatboxapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tnutchatboxapp.adapter.ChatHistoryAdapter
import com.example.tnutchatboxapp.databinding.ActivityChatHistoryBinding
import com.example.tnutchatboxapp.model.ChatSession
import com.example.tnutchatboxapp.model.ChatSessionsResponse
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatHistoryBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: ChatHistoryAdapter
    private val chatSessions = mutableListOf<ChatSession>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupToolbar()
        setupRecyclerView()
        loadChatHistory()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Lịch sử đoạn chat"
    }
    
    private fun setupRecyclerView() {
        historyAdapter = ChatHistoryAdapter(chatSessions) { session ->
            openChatSession(session)
        }
        
        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@ChatHistoryActivity)
        }
    }
    
    private fun loadChatHistory() {
        showLoading(true)
        
        RetrofitClient.chatInstance.getChatSessions().enqueue(object : Callback<ChatSessionsResponse> {
            override fun onResponse(call: Call<ChatSessionsResponse>, response: Response<ChatSessionsResponse>) {
                showLoading(false)
                
                if (response.isSuccessful && response.body() != null) {
                    val sessions = response.body()!!.sessions
                    chatSessions.clear()
                    chatSessions.addAll(sessions)
                    historyAdapter.notifyDataSetChanged()
                    
                    showEmptyState(sessions.isEmpty())
                } else {
                    Toast.makeText(this@ChatHistoryActivity, "Không thể tải lịch sử chat", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            }
            
            override fun onFailure(call: Call<ChatSessionsResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@ChatHistoryActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            }
        })
    }
    
    private fun openChatSession(session: ChatSession) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("session_id", session.id)
            putExtra("session_title", session.title)
        }
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewHistory.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        binding.textViewEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
