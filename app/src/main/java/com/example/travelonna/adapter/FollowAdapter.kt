package com.example.travelonna.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.User
import com.example.travelonna.view.CustomToggleButton
import com.example.travelonna.util.ImageUtils

class FollowAdapter(
    private val users: List<User>,
    private val onFollowClick: (User, Int) -> Unit
) : RecyclerView.Adapter<FollowAdapter.FollowViewHolder>() {

    class FollowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.ivProfileImage)
        val username: TextView = view.findViewById(R.id.tvUsername)
        val followStatus: TextView = view.findViewById(R.id.tvFollowStatus)
        val followToggle: CustomToggleButton = view.findViewById(R.id.followToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow, parent, false)
        return FollowViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        val user = users[position]
        
        holder.username.text = user.username
        
        // 프로필 이미지 설정 - ImageUtils 사용
        ImageUtils.loadProfileImage(holder.profileImage, user.profileImageUrl)
        
        // 팔로우 상태에 따라 텍스트와 색상 변경
        updateFollowStatus(holder, user.isFollowing)
        
        // 팔로우 상태에 따라 토글 상태 설정
        holder.followToggle.setChecked(user.isFollowing)
        
        // 팔로우 토글 변경 이벤트
        holder.followToggle.setOnCheckedChangeListener { isChecked ->
            // 상태 변경은 콜백에서 처리하도록 하고, 여기서는 즉시 변경하지 않음
            onFollowClick(user, position)
        }
    }
    
    private fun updateFollowStatus(holder: FollowViewHolder, isFollowing: Boolean) {
        if (isFollowing) {
            holder.followStatus.text = "팔로우 중"
            holder.followStatus.setTextColor(Color.parseColor("#5E7BF9"))
        } else {
            holder.followStatus.text = "팔로우"
            holder.followStatus.setTextColor(Color.parseColor("#888888"))
        }
    }

    override fun getItemCount() = users.size
} 