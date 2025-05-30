package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Post
import com.example.travelonna.view.CustomToggleButton

class AccountSearchAdapter(private var posts: List<Post>) : 
    RecyclerView.Adapter<AccountSearchAdapter.AccountViewHolder>() {

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val followStatusText: TextView = itemView.findViewById(R.id.followStatusText)
        val followToggle: CustomToggleButton = itemView.findViewById(R.id.followToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_search_new, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val post = posts[position]
        
        // 프로필 이미지 설정
        holder.profileImageView.setImageResource(post.imageResource)
        
        // 사용자 이름 설정
        holder.usernameTextView.text = post.userName
        
        // 팔로우 상태에 따라 토글과 텍스트 UI 업데이트
        updateFollowStatus(holder, post.isFollowing)
        
        // 토글 클릭 이벤트
        holder.followToggle.setOnCheckedChangeListener { isChecked ->
            post.isFollowing = isChecked
            updateFollowStatus(holder, isChecked)
            // TODO: 실제 팔로우/언팔로우 API 호출 구현
        }
    }

    private fun updateFollowStatus(holder: AccountViewHolder, isFollowing: Boolean) {
        holder.followToggle.setChecked(isFollowing)
        if (isFollowing) {
            holder.followStatusText.text = "팔로우 중"
            holder.followStatusText.setTextColor(holder.itemView.context.getColor(R.color.blue))
        } else {
            holder.followStatusText.text = "팔로우"
            holder.followStatusText.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updateData(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
} 