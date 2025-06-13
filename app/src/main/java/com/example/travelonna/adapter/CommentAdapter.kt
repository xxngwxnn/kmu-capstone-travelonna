package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.CommentData
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private var comments: MutableList<CommentData>,
    private val listener: CommentActionListener
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    interface CommentActionListener {
        fun onEditComment(comment: CommentData, position: Int)
        fun onDeleteComment(comment: CommentData, position: Int)
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.commentUserName)
        val content: TextView = itemView.findViewById(R.id.commentContent)
        val timestamp: TextView = itemView.findViewById(R.id.commentTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.userName.text = comment.userName
        holder.content.text = comment.comment
        
        // 타임스탬프를 상대적 시간으로 표시
        val timeAgo = getTimeAgo(comment.createdAt)
        holder.timestamp.text = timeAgo
        
        // 롱클릭으로 수정/삭제 메뉴 표시 (본인 댓글만)
        holder.itemView.setOnLongClickListener {
            // TODO: 현재 사용자 ID와 댓글 작성자 ID 비교 후 메뉴 표시
            false
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<CommentData>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
    
    fun addComment(comment: CommentData) {
        comments.add(0, comment) // 최신 댓글을 맨 위에 추가
        notifyItemInserted(0)
    }
    
    fun removeComment(position: Int) {
        if (position >= 0 && position < comments.size) {
            comments.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun updateComment(position: Int, updatedComment: CommentData) {
        if (position >= 0 && position < comments.size) {
            comments[position] = updatedComment
            notifyItemChanged(position)
        }
    }

    private fun getTimeAgo(createdAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = inputFormat.parse(createdAt)
            val timestamp = date?.time ?: return createdAt
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            when {
                diff < 60 * 1000 -> "방금 전"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}분 전"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}시간 전"
                else -> {
                    val sdf = SimpleDateFormat("MM.dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        } catch (e: Exception) {
            createdAt.substring(0, 10).replace("-", ".")
        }
    }
} 