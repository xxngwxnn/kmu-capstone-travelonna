package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Post

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val userName: TextView = view.findViewById(R.id.userName)
        val followText: TextView = view.findViewById(R.id.followText)
        val followToggle: SwitchCompat = view.findViewById(R.id.followToggle)
        val description: TextView = view.findViewById(R.id.description)
        val date: TextView = view.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.postImage.setImageResource(post.imageResource)
        holder.userName.text = post.userName
        holder.description.text = post.description
        holder.date.text = post.date
        
        // 팔로우 상태에 따라 토글과 텍스트 업데이트
        holder.followToggle.isChecked = post.isFollowing
        holder.followText.text = if (post.isFollowing) "팔로우 중" else "팔로우"
        
        // 토글 상태 변경 리스너
        holder.followToggle.setOnCheckedChangeListener { _, isChecked ->
            holder.followText.text = if (isChecked) "팔로우 중" else "팔로우"
        }
    }

    override fun getItemCount() = posts.size
} 