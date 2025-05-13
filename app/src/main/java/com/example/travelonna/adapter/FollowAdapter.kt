package com.example.travelonna.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.User

class FollowAdapter(
    private val users: List<User>,
    private val onFollowClick: (User, Int) -> Unit
) : RecyclerView.Adapter<FollowAdapter.FollowViewHolder>() {

    class FollowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.ivProfileImage)
        val username: TextView = view.findViewById(R.id.tvUsername)
        val followStatus: TextView = view.findViewById(R.id.tvFollowStatus)
        val followToggle: SwitchCompat = view.findViewById(R.id.followToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow, parent, false)
        return FollowViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        val user = users[position]
        
        holder.username.text = user.username
        
        // 프로필 이미지 설정
        // 실제 앱에서는 Glide나 Picasso 같은 이미지 로딩 라이브러리를 사용할 수 있습니다
        holder.profileImage.setImageResource(R.drawable.ic_place_holder)
        
        // 팔로우 상태에 따라 텍스트와 색상 변경
        updateFollowStatus(holder, user.isFollowing)
        
        // 리스너 설정 전에 이벤트 발생 방지를 위해 일시적으로 리스너 제거
        holder.followToggle.setOnCheckedChangeListener(null)
        
        // 팔로우 상태에 따라 토글 상태 설정
        holder.followToggle.isChecked = user.isFollowing
        
        // 팔로우 토글 변경 이벤트
        holder.followToggle.setOnCheckedChangeListener { _, isChecked ->
            user.isFollowing = isChecked
            updateFollowStatus(holder, isChecked)
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