package com.example.travelonna.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Post
import com.example.travelonna.view.CustomToggleButton

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val userName: TextView = view.findViewById(R.id.userName)
        val followText: TextView = view.findViewById(R.id.followText)
        val followToggle: CustomToggleButton = view.findViewById(R.id.followToggle)
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
        holder.followToggle.setChecked(post.isFollowing)
        updateFollowTextState(holder.followText, post.isFollowing)
        
        // 토글 상태 변경 리스너
        holder.followToggle.setOnCheckedChangeListener { isChecked ->
            updateFollowTextState(holder.followText, isChecked)
        }
    }
    
    // 팔로우 텍스트 상태 업데이트 (텍스트 내용 및 색상)
    private fun updateFollowTextState(textView: TextView, isFollowing: Boolean) {
        if (isFollowing) {
            textView.text = "팔로우 중"
            textView.setTextColor(Color.parseColor("#527BF9"))
        } else {
            textView.text = "팔로우"
            textView.setTextColor(Color.parseColor("#B9B9B9")) // 토글 배경과 동일한 회색
        }
    }

    override fun getItemCount() = posts.size
} 