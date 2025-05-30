package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private var comments: List<Comment>) : 
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val userName: TextView = view.findViewById(R.id.userName)
        val content: TextView = view.findViewById(R.id.content)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        // 프로필 이미지 설정
        comment.userProfileImage?.let { imageUrl ->
            // TODO: 프로필 이미지 로딩 구현
        } ?: run {
            holder.profileImage.setImageResource(R.drawable.default_profile)
        }
        
        // 사용자 이름 설정
        holder.userName.text = comment.userName
        
        // 댓글 내용 설정
        holder.content.text = comment.content
        
        // 시간 설정
        holder.timestamp.text = dateFormat.format(Date(comment.timestamp))
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
} 