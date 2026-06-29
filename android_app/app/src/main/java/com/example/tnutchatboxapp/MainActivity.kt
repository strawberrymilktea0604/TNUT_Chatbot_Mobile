package com.example.tnutchatboxapp

import com.example.tnutchatboxapp.model.AuthResponse
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tnutchatboxapp.adapter.ChatAdapter
import com.example.tnutchatboxapp.auth.LoginActivity
import com.example.tnutchatboxapp.databinding.ActivityMainBinding
import com.example.tnutchatboxapp.model.*
import com.example.tnutchatboxapp.network.Message
import com.example.tnutchatboxapp.network.ResponseMessage
import com.example.tnutchatboxapp.network.RetrofitClient
import com.example.tnutchatboxapp.utils.JWTUtils
import com.example.tnutchatboxapp.utils.SessionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle
    
    private val chatMessages = mutableListOf<ChatMessage>()
    private var requestStartTime: Long = 0
    private var currentSessionId: String? = null
    private var isFirstMessage = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        // The check is now done in SplashActivity, so we can remove it from here.
        // However, we'll keep a fallback check just in case.
        if (sessionManager.fetchAuthToken().isNullOrEmpty()) {
            navigateToLogin()
            return
        }
        
        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupClickListeners()
        setupUserInfo()
        checkUserVerification()
        
        // Handle incoming session from intent
        handleIncomingSession()
        
        // Add welcome message
        addWelcomeMessage()
    }

    override fun onResume() {
        super.onResume()
        // Check verification status every time the activity is resumed
        checkUserVerification()

        // Deselect navigation items when returning to the activity
        binding.navigationView.menu.findItem(R.id.nav_new_chat).isChecked = false
        binding.navigationView.menu.findItem(R.id.nav_chat_history).isChecked = false
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.chat_title)
    }
    
    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Add a drawer listener to deselect items when the drawer is opened
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Deselect items as soon as the drawer starts sliding
                if (slideOffset > 0) {
                    binding.navigationView.menu.findItem(R.id.nav_new_chat).isChecked = false
                    binding.navigationView.menu.findItem(R.id.nav_chat_history).isChecked = false
                }
            }
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        
        binding.navigationView.setNavigationItemSelectedListener(this)
    }
    
    private fun setupUserInfo() {
        val headerView = binding.navigationView.getHeaderView(0)
        val usernameText = headerView.findViewById<android.widget.TextView>(R.id.textViewUsername)
        val emailText = headerView.findViewById<android.widget.TextView>(R.id.textViewEmail)
        
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
            usernameText.text = JWTUtils.getUsername(token) ?: "User"
            emailText.text = JWTUtils.getEmail(token) ?: "No email"
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_new_chat -> {
                createNewChatSession()
            }
            R.id.nav_chat_history -> {
                openChatHistory()
            }
            R.id.nav_change_password -> {
                showChangePasswordDialog()
            }
            R.id.nav_change_email -> {
                showChangeEmailDialog()
            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun handleIncomingSession() {
        val sessionId = intent.getStringExtra("session_id")
        val sessionTitle = intent.getStringExtra("session_title")
        
        if (sessionId != null && sessionTitle != null) {
            currentSessionId = sessionId
            supportActionBar?.title = sessionTitle
            loadChatHistory(sessionId)
        }
    }
    
    private fun loadChatHistory(sessionId: String) {
        RetrofitClient.chatInstance.getChatHistory(sessionId).enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val history = response.body()!!.messages
                    chatAdapter.clearMessages()
                    
                    // Add welcome message first
                    addWelcomeMessage()
                    
                    // Add historical messages (only current session messages, not regenerated)
                    history.filter { !it.isRegenerated }.forEach { historyItem ->
                        val message = if (historyItem.isUser) {
                            ChatMessage.createUserMessage(historyItem.content).copy(
                                sessionMessageId = historyItem.sessionMessageId,
                                isFromCurrentSession = true
                            )
                        } else {
                            ChatMessage.createBotMessage(historyItem.content).copy(
                                sessionMessageId = historyItem.sessionMessageId,
                                isFromCurrentSession = true
                            )
                        }
                        chatAdapter.addMessage(message)
                    }
                    
                    isFirstMessage = history.filter { !it.isRegenerated }.isEmpty()
                } else {
                    Toast.makeText(this@MainActivity, "Không thể tải lịch sử chat", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Lỗi kết nối khi tải lịch sử", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun createNewChatSession() {
        // Clear current chat
        chatAdapter.clearMessages()
        currentSessionId = null
        isFirstMessage = true
        
        // Add welcome message
        addWelcomeMessage()
        
        supportActionBar?.title = getString(R.string.new_chat_title)
    }
    
    private fun openChatHistory() {
        val intent = Intent(this, ChatHistoryActivity::class.java)
        startActivity(intent)
    }
    
    private fun showChangePasswordDialog() {
        val intent = Intent(this, com.example.tnutchatboxapp.account.ChangePasswordActivity::class.java)
        startActivity(intent)
    }
    
    private fun showChangeEmailDialog() {
        val intent = Intent(this, com.example.tnutchatboxapp.account.ChangeEmailActivity::class.java)
        startActivity(intent)
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                logout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun logout() {
        sessionManager.clearAuthToken()
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
        }
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(this, chatMessages) { userMessage ->
            // Handle regenerate message
            regenerateMessage(userMessage)
        }
        binding.recyclerViewChat.adapter = chatAdapter
        
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = layoutManager
        
        // Auto scroll to bottom when new message is added
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener {
            sendMessageIfNotEmpty()
        }
        
        // Allow sending message by pressing Enter
        binding.editTextMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendMessageIfNotEmpty()
                true
            } else {
                false
            }
        }
        
        // Auto-scroll when keyboard appears
        binding.editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && chatMessages.isNotEmpty()) {
                binding.recyclerViewChat.postDelayed({
                    binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 300)
            }
        }
    }
    
    private fun sendMessageIfNotEmpty() {
        val messageText = binding.editTextMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            sendMessage(messageText)
            binding.editTextMessage.text?.clear()
        }
    }
    
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage.createBotMessage(getString(R.string.welcome_message))
        chatAdapter.addMessage(welcomeMessage)
        scrollToBottom()
    }
    
    private fun sendMessage(messageText: String) {
        // 1. Add user message immediately
        val userMessage = ChatMessage.createUserMessage(messageText)
        chatAdapter.addMessage(userMessage)
        
        // 2. Create session on first message
        if (isFirstMessage && currentSessionId == null) {
            createSessionAndSendMessage(messageText)
            return
        }
        
        // 3. Add loading message
        val loadingMessage = ChatMessage.createLoadingMessage()
        chatAdapter.addMessage(loadingMessage)
        
        // 4. Disable send button during API call
        binding.buttonSend.isEnabled = false
        
        // 5. Record start time for measuring response time
        requestStartTime = System.currentTimeMillis()
        
        // 6. Send message to server
        sendMessageToServer(messageText)
        
        // Scroll to bottom
        scrollToBottom()
    }

    private fun createSessionAndSendMessage(messageText: String) {
        val sessionTitle = generateSessionTitle(messageText)
        val sessionCreate = ChatSessionCreate(sessionTitle)
        
        RetrofitClient.chatInstance.createChatSession(sessionCreate).enqueue(object : Callback<ChatSessionResponse> {
            override fun onResponse(call: Call<ChatSessionResponse>, response: Response<ChatSessionResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    currentSessionId = response.body()!!.session_id
                    supportActionBar?.title = sessionTitle
                    isFirstMessage = false
                    
                    // Now send the message
                    val loadingMessage = ChatMessage.createLoadingMessage()
                    chatAdapter.addMessage(loadingMessage)
                    binding.buttonSend.isEnabled = false
                    requestStartTime = System.currentTimeMillis()
                    sendMessageToServer(messageText)
                    scrollToBottom()
                } else {
                    Toast.makeText(this@MainActivity, "Không thể tạo phiên chat", Toast.LENGTH_SHORT).show()
                    binding.buttonSend.isEnabled = true
                }
            }
            
            override fun onFailure(call: Call<ChatSessionResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Lỗi kết nối khi tạo phiên", Toast.LENGTH_SHORT).show()
                binding.buttonSend.isEnabled = true
            }
        })
    }

    private fun generateSessionTitle(messageText: String): String {
        return if (messageText.length > 30) {
            messageText.substring(0, 30) + "..."
        } else {
            messageText
        }
    }

    private fun regenerateMessage(userMessage: String) {
        // Remove the last bot message from UI only (not from database)
        if (chatAdapter.getMessages().isNotEmpty() && !chatAdapter.getMessages().last().isUser) {
            chatAdapter.removeLastMessage()
        }
        
        // Add loading message
        val loadingMessage = ChatMessage.createLoadingMessage()
        chatAdapter.addMessage(loadingMessage)
        
        // Disable send button
        binding.buttonSend.isEnabled = false
        requestStartTime = System.currentTimeMillis()
        
        // Call regenerate API instead of regular send message
        if (currentSessionId != null) {
            val messageRequest = ChatMessageRequest(userMessage)
            
            RetrofitClient.chatInstance.regenerateMessage(currentSessionId!!, messageRequest)
                .enqueue(object : Callback<ChatMessageResponse> {
                    override fun onResponse(call: Call<ChatMessageResponse>, response: Response<ChatMessageResponse>) {
                        handleServerResponse(response.isSuccessful, response.body()?.aiResponse)
                    }
                    
                    override fun onFailure(call: Call<ChatMessageResponse>, t: Throwable) {
                        handleServerFailure(t)
                    }
                })
        } else {
            // Fallback to regular message sending if no session
            sendMessageToServer(userMessage)
        }
        
        scrollToBottom()
    }

    private fun sendMessageToServer(messageText: String) {
        // Use the new session-based API if we have a session
        if (currentSessionId != null) {
            val messageRequest = ChatMessageRequest(messageText)
            
            RetrofitClient.chatInstance.sendMessageToSession(currentSessionId!!, messageRequest)
                .enqueue(object : Callback<ChatMessageResponse> {
                    override fun onResponse(call: Call<ChatMessageResponse>, response: Response<ChatMessageResponse>) {
                        handleServerResponse(response.isSuccessful, response.body()?.aiResponse)
                    }
                    
                    override fun onFailure(call: Call<ChatMessageResponse>, t: Throwable) {
                        handleServerFailure(t)
                    }
                })
        } else {
            // Fallback to legacy API
            val message = Message(messageText)
            
            RetrofitClient.chatInstance.sendMessage(message).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    handleServerResponse(response.isSuccessful, response.body()?.answer)
                }
                
                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    handleServerFailure(t)
                }
            })
        }
    }

    private fun handleServerResponse(isSuccessful: Boolean, responseText: String?) {
        val responseTime = System.currentTimeMillis() - requestStartTime
        
        // Re-enable send button
        binding.buttonSend.isEnabled = true
        
        // Remove loading message
        chatAdapter.removeLastMessage()
        
        if (isSuccessful && responseText != null) {
            // Add bot response with typewriter effect directly
            val typingMessage = ChatMessage.createTypingMessage(responseText)
            chatAdapter.addMessage(typingMessage)
            
            // Log response time for debugging
            android.util.Log.d("ChatApp", "Response time: ${responseTime}ms")
        } else {
            // Add error message
            val errorMessage = ChatMessage.createFailedBotMessage(getString(R.string.error_message))
            chatAdapter.addMessage(errorMessage)
            
            Toast.makeText(this, "Lỗi phản hồi từ server", Toast.LENGTH_SHORT).show()
        }
        
        scrollToBottom()
    }

    private fun handleServerFailure(t: Throwable) {
        val responseTime = System.currentTimeMillis() - requestStartTime

        // Re-enable send button
        binding.buttonSend.isEnabled = true

        // Remove loading message
        chatAdapter.removeLastMessage()

        // Check for timeout exception
        val errorMessage = if (t is java.net.SocketTimeoutException) {
            getString(R.string.timeout_error)
        } else {
            getString(R.string.connection_error)
        }

        // Add failure message
        val failureMessage = ChatMessage.createFailedBotMessage(errorMessage)
        chatAdapter.addMessage(failureMessage)

        Toast.makeText(this, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()

        // Log response time for debugging
        android.util.Log.d("ChatApp", "Failed response time: ${responseTime}ms")
        
        scrollToBottom()
    }
    
    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        chatAdapter.cancelTypewriter()
    }
    private fun checkUserVerification() {
        // Instead of checking the local token, we ask the server for the latest status.
        // The server will validate the current token and return a new one with updated info.
        RetrofitClient.authInstance.verifyToken().enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    // Save the new token returned by the server
                    authResponse.accessToken?.let {
                        sessionManager.saveAuthToken(it)
                    }

                    // Update UI based on the verified status from the server
                    if (authResponse.isVerified == true) {
                        enableChat()
                    } else {
                        disableChat()
                        showVerificationSnackbar()
                    }
                } else {
                    // If token is invalid or expired, the API will return an error (e.g., 401)
                    // Log the user out to be safe.
                    Toast.makeText(this@MainActivity, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show()
                    logout()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                // Network error, assume not verified and disable chat
                disableChat()
                Toast.makeText(this@MainActivity, "Lỗi kết nối. Không thể xác thực tài khoản.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showVerificationSnackbar() {
        // Check current verification status first
        val token = sessionManager.fetchAuthToken()
        val isVerified = token?.let { JWTUtils.isUserVerified(it) } ?: false
        
        if (isVerified) {
            // If user is already verified, just show a message without action button
            Snackbar.make(binding.root, "Tài khoản đã được xác thực. Đang tải lại...", Snackbar.LENGTH_SHORT).show()
            // Refresh the verification status
            checkUserVerification()
        } else {
            // Show snackbar with resend action only if not verified
            Snackbar.make(binding.root, "Tài khoản của bạn chưa được xác thực.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Gửi lại email") {
                    resendVerificationEmail()
                }
                .show()
        }
    }

    private fun resendVerificationEmail() {
        val token = sessionManager.fetchAuthToken()
        val email = token?.let { JWTUtils.getEmail(it) }

        if (email == null) {
            Toast.makeText(this, "Không thể lấy email người dùng.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        RetrofitClient.authInstance.resendVerificationEmail(com.example.tnutchatboxapp.model.ResendVerificationRequest(email)).enqueue(object : Callback<com.example.tnutchatboxapp.model.ApiResponse> {
            override fun onResponse(call: Call<com.example.tnutchatboxapp.model.ApiResponse>, response: Response<com.example.tnutchatboxapp.model.ApiResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Email xác thực đã được gửi.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> {
                            try {
                                val errorBody = response.errorBody()?.string()
                                if (errorBody?.contains("already verified") == true) {
                                    "Email đã được xác thực. Không cần gửi lại mã xác nhận."
                                } else {
                                    "Thông tin không hợp lệ"
                                }
                            } catch (e: Exception) {
                                "Email đã được xác thực. Không cần gửi lại mã xác nhận."
                            }
                        }
                        500 -> "Không thể gửi email. Vui lòng thử lại sau."
                        else -> "Lỗi khi gửi lại email: ${response.code()}"
                    }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<com.example.tnutchatboxapp.model.ApiResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@MainActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun disableChat() {
        binding.editTextMessage.isEnabled = false
        binding.buttonSend.isEnabled = false
        binding.editTextMessage.hint = "Vui lòng xác thực email để chat"
    }

    private fun enableChat() {
        binding.editTextMessage.isEnabled = true
        binding.buttonSend.isEnabled = true
        binding.editTextMessage.hint = getString(R.string.type_a_message)
    }

    private fun showLoading(isLoading: Boolean) {
        // You can implement a progress bar if you want
    }
}
