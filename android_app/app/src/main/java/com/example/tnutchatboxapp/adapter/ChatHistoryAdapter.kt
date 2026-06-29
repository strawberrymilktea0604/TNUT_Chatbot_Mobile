package com.example.tnutchatboxapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tnutchatboxapp.R
import com.example.tnutchatboxapp.model.ChatSession

class ChatHistoryAdapter(
    private val sessions: List<ChatSession>,
    private val onSessionClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatHistoryAdapter.ChatHistoryViewHolder>() {

    class ChatHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.textViewTitle)
        val previewText: TextView = itemView.findViewById(R.id.textViewPreview)
        val dateText: TextView = itemView.findViewById(R.id.textViewDate)
        val messageCountText: TextView = itemView.findViewById(R.id.textViewMessageCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_history, parent, false)
        return ChatHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatHistoryViewHolder, position: Int) {
        val session = sessions[position]
        
        holder.titleText.text = session.title
        holder.previewText.text = session.getPreviewText()
        holder.dateText.text = session.getFormattedDate()
        holder.messageCountText.text = "${session.messageCount} tin nhắn"
        
        holder.itemView.setOnClickListener {
            onSessionClick(session)
        }
    }

    override fun getItemCount(): Int = sessions.size
}
