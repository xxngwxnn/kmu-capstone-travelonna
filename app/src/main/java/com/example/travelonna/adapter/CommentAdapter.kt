package com.example.travelonna.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.CommentData
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParseException

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
        val deleteButton: ImageView = itemView.findViewById(R.id.commentDeleteButton)
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
        
        // 현재 로그인한 사용자 ID와 댓글 작성자 ID 비교
        val currentUserId = com.example.travelonna.api.RetrofitClient.getUserId()
        if (comment.userId == currentUserId) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                listener.onDeleteComment(comment, position)
            }
        } else {
            holder.deleteButton.visibility = View.GONE
        }
        
        // 롱클릭으로 수정/삭제 메뉴 표시 (본인 댓글만)
        holder.itemView.setOnLongClickListener {
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

    private fun getTimeAgo(createdAt: String?): String {
        if (createdAt == null || createdAt.isEmpty()) {
            return "방금 전"
        }
        try {
            val date = try {
                // 첫 번째 형식 시도: yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'
                val sdf1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.getDefault())
                sdf1.timeZone = TimeZone.getTimeZone("UTC")
                sdf1.parse(createdAt)
            } catch (e: ParseException) {
                try {
                    // 두 번째 형식 시도: yyyy-MM-dd'T'HH:mm:ss
                    val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    sdf2.timeZone = TimeZone.getTimeZone("UTC")
                    sdf2.parse(createdAt)
                } catch (e: ParseException) {
                    // 세 번째 형식 시도: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
                    val sdf3 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    sdf3.timeZone = TimeZone.getTimeZone("UTC")
                    sdf3.parse(createdAt)
                }
            }

            val now = System.currentTimeMillis()
            val diff = now - (date?.time ?: 0)
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            return when {
                days > 0 -> "${days}일 전"
                hours > 0 -> "${hours}시간 전"
                minutes > 0 -> "${minutes}분 전"
                else -> "방금 전"
            }
        } catch (e: Exception) {
            Log.e("CommentAdapter", "Error parsing date: $createdAt", e)
            return "방금 전"
        }
    }
} 