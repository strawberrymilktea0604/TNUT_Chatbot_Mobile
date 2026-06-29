package com.example.tnutchatboxapp.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tnutchatboxapp.R
import com.example.tnutchatboxapp.model.ChatMessage
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.*

class ChatAdapter(
    private val context: Context,
    private val messages: MutableList<ChatMessage>,
    private val onRegenerateMessage: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val markwon: Markwon by lazy {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    
    private var typewriterJob: Job? = null

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMessageLayout: LinearLayout = itemView.findViewById(R.id.userMessageLayout)
        val botMessageLayout: LinearLayout = itemView.findViewById(R.id.botMessageLayout)
        val loadingLayout: LinearLayout = itemView.findViewById(R.id.loadingLayout)
        val userMessageText: TextView = itemView.findViewById(R.id.userMessageText)
        val botMessageText: TextView = itemView.findViewById(R.id.botMessageText)
        val botMessageActions: LinearLayout = itemView.findViewById(R.id.botMessageActions)
        val buttonCopyMessage: ImageButton = itemView.findViewById(R.id.buttonCopyMessage)
        val buttonRegenerateMessage: ImageButton = itemView.findViewById(R.id.buttonRegenerateMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // Hide all layouts first
        holder.userMessageLayout.visibility = View.GONE
        holder.botMessageLayout.visibility = View.GONE
        holder.loadingLayout.visibility = View.GONE
        holder.botMessageActions.visibility = View.GONE

        when {
            message.isLoading -> {
                // Show loading layout with Lottie animation
                holder.loadingLayout.visibility = View.VISIBLE
            }
            message.isUser -> {
                // Show user message
                holder.userMessageLayout.visibility = View.VISIBLE
                holder.userMessageText.text = message.text
            }
            message.isTyping -> {
                // Show bot message with typewriter effect
                holder.botMessageLayout.visibility = View.VISIBLE
                startTypewriterEffect(holder.botMessageText, message.text) {
                    // After typewriter completes, convert to regular message and refresh to show buttons
                    val position = holder.adapterPosition
                    if (position != RecyclerView.NO_POSITION && position < messages.size) {
                        messages[position] = ChatMessage.createBotMessage(message.text)
                        notifyItemChanged(position)
                    }
                }
                
                // Hide action buttons during typing
                holder.botMessageActions.visibility = View.GONE
            }
            else -> {
                // Show bot message normally
                holder.botMessageLayout.visibility = View.VISIBLE
                
                // Use Markwon to render markdown content
                markwon.setMarkdown(holder.botMessageText, message.text)
                
                // Show action buttons for bot messages (except welcome messages)
                if (message.text.isNotEmpty() && !message.text.contains("Xin chào")) {
                    holder.botMessageActions.visibility = View.VISIBLE
                    
                    // Show copy button only if message is successful (not failed)
                    if (!message.isFailed) {
                        holder.buttonCopyMessage.visibility = View.VISIBLE
                    } else {
                        holder.buttonCopyMessage.visibility = View.GONE
                    }
                    
                    // Always show regenerate button for bot messages
                    holder.buttonRegenerateMessage.visibility = View.VISIBLE
                    
                    // Setup click listeners
                    holder.buttonCopyMessage.setOnClickListener {
                        copyMessageToClipboard(holder.itemView.context, message.text)
                    }
                    
                    holder.buttonRegenerateMessage.setOnClickListener {
                        // Find the previous user message to regenerate
                        val userMessage = findPreviousUserMessage(position)
                        if (userMessage != null) {
                            onRegenerateMessage?.invoke(userMessage)
                        }
                    }
                } else {
                    // Hide action buttons for welcome messages
                    holder.botMessageActions.visibility = View.GONE
                }
            }
        }

        // Add animation for new messages
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.message_slide_in)
        holder.itemView.startAnimation(animation)
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastMessage() {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages.removeAt(lastIndex)
            notifyItemRemoved(lastIndex)
        }
    }

    fun updateLastMessage(newMessage: ChatMessage) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = newMessage
            notifyItemChanged(lastIndex)
        }
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }

    fun getMessages(): List<ChatMessage> {
        return messages.toList()
    }
    
    fun startTypewriterForLastMessage(fullText: String, onComplete: (() -> Unit)? = null) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            val typingMessage = ChatMessage.createTypingMessage(fullText)
            messages[lastIndex] = typingMessage
            notifyItemChanged(lastIndex)
            
            // The completion will be handled by the typewriter effect callback
            // which will automatically convert to final message and show buttons
            typewriterJob = CoroutineScope(Dispatchers.Main).launch {
                delay(calculateTypewriterDuration(fullText) + 100) // Small buffer for completion
                onComplete?.invoke()
            }
        }
    }
    
    private fun calculateTypewriterDuration(text: String): Long {
        // Fast typing speed: 30ms per character
        return (text.length * 30L).coerceAtLeast(500L)
    }
    
    private fun startTypewriterEffect(textView: TextView, fullText: String, onComplete: (() -> Unit)? = null) {
        typewriterJob?.cancel()
        typewriterJob = CoroutineScope(Dispatchers.Main).launch {
            textView.text = ""
            
            for (i in fullText.indices) {
                delay(30) // Fast typing speed - 30ms per character
                val currentText = fullText.substring(0, i + 1)
                
                // For markdown content, render incrementally
                markwon.setMarkdown(textView, currentText)
            }
            
            // Trigger completion callback to refresh the item and show buttons
            onComplete?.invoke()
        }
    }

    private fun copyMessageToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
    }

    private fun findPreviousUserMessage(currentPosition: Int): String? {
        // Look backward from current position to find the last user message
        for (i in currentPosition - 1 downTo 0) {
            if (messages[i].isUser) {
                return messages[i].text
            }
        }
        return null
    }
    
    fun cancelTypewriter() {
        typewriterJob?.cancel()
    }
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cancelTypewriter()
    }
}
