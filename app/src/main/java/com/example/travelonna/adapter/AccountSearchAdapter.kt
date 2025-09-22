package com.example.travelonna.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.FollowRequest
import com.example.travelonna.api.FollowResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.UnfollowResponse
import com.example.travelonna.model.Post
import com.example.travelonna.view.CustomToggleButton
import com.example.travelonna.util.ImageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountSearchAdapter(private var posts: List<Post>, private val onItemClick: (Post) -> Unit) : 
    RecyclerView.Adapter<AccountSearchAdapter.AccountViewHolder>() {
    
    // posts 접근을 위한 getter
    fun getPosts(): List<Post> = posts

    companion object {
        private const val TAG = "AccountSearchAdapter"
    }

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
        
        // 프로필 이미지 설정 - ImageUtils 사용
        ImageUtils.loadProfileImage(holder.profileImageView, post.imageUrl)
        
        // 사용자 이름 설정
        holder.usernameTextView.text = post.userName
        Log.d(TAG, "사용자 표시 - userId: ${post.id}, userName: '${post.userName}', date: '${post.date}'")
        
        // 팔로우 상태에 따라 토글과 텍스트 UI 업데이트
        updateFollowStatus(holder, post.isFollowing)
        
        // 토글 클릭 이벤트 - 실제 API 호출 구현
        holder.followToggle.setOnCheckedChangeListener { isChecked ->
            if (isChecked != post.isFollowing) {
                handleFollowToggle(holder, post, position, isChecked)
            }
        }
        
        holder.itemView.setOnClickListener { onItemClick(post) }
    }

    private fun handleFollowToggle(holder: AccountViewHolder, post: Post, position: Int, shouldFollow: Boolean) {
        val context = holder.itemView.context
        val userId = post.id.toInt()
        
        Log.d(TAG, "Toggling follow for userId: $userId, shouldFollow: $shouldFollow")
        
        if (shouldFollow) {
            // 팔로우하기
            followUser(context, holder, post, position, userId)
        } else {
            // 언팔로우하기
            unfollowUser(context, holder, post, position, userId)
        }
    }
    
    private fun followUser(context: Context, holder: AccountViewHolder, post: Post, position: Int, userId: Int) {
        val currentUserId = RetrofitClient.getUserId()
        val request = FollowRequest(toUser = userId, fromUser = currentUserId)
        
        Log.d(TAG, "팔로우 API 호출 - toUser: $userId, fromUser: $currentUserId")
        RetrofitClient.apiService.followUser(request).enqueue(object : Callback<FollowResponse> {
            override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val followResponse = response.body()!!
                    
                    if (followResponse.success && followResponse.data != null) {
                        val isFollowing = followResponse.data.isFollowing
                        Log.d(TAG, "팔로우 성공: userId=$userId, isFollowing=$isFollowing")
                        
                        // 상태 업데이트
                        post.isFollowing = isFollowing
                        updateFollowStatus(holder, isFollowing)
                        
                        // 브로드캐스트 전송으로 다른 화면에 알림
                        sendFollowUpdateBroadcast(context, userId, isFollowing)
                        
                        Toast.makeText(context, "${post.userName}님을 팔로우했습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w(TAG, "팔로우 실패: ${followResponse.message}")
                        revertToggleState(holder, post, false)
                        Toast.makeText(context, "팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "팔로우 HTTP 오류: ${response.code()}")
                    revertToggleState(holder, post, false)
                    Toast.makeText(context, "팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
                Log.e(TAG, "팔로우 네트워크 오류", t)
                revertToggleState(holder, post, false)
                Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun unfollowUser(context: Context, holder: AccountViewHolder, post: Post, position: Int, userId: Int) {
        Log.d(TAG, "언팔로우 API 호출 - userId: $userId")
        RetrofitClient.apiService.unfollowUser(userId).enqueue(object : Callback<UnfollowResponse> {
            override fun onResponse(call: Call<UnfollowResponse>, response: Response<UnfollowResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val unfollowResponse = response.body()!!
                    
                    if (unfollowResponse.success) {
                        Log.d(TAG, "언팔로우 성공: userId=$userId")
                        
                        // 상태 업데이트
                        post.isFollowing = false
                        updateFollowStatus(holder, false)
                        
                        // 브로드캐스트 전송으로 다른 화면에 알림
                        sendFollowUpdateBroadcast(context, userId, false)
                        
                        Toast.makeText(context, "${post.userName}님의 팔로우를 취소했습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w(TAG, "언팔로우 실패: ${unfollowResponse.message}")
                        revertToggleState(holder, post, true)
                        Toast.makeText(context, "언팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "언팔로우 HTTP 오류: ${response.code()}")
                    revertToggleState(holder, post, true)
                    Toast.makeText(context, "언팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<UnfollowResponse>, t: Throwable) {
                Log.e(TAG, "언팔로우 네트워크 오류", t)
                revertToggleState(holder, post, true)
                Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun revertToggleState(holder: AccountViewHolder, post: Post, originalState: Boolean) {
        // 토글 상태를 원래대로 되돌리기
        post.isFollowing = originalState
        holder.followToggle.setOnCheckedChangeListener { } // 리스너 임시 제거 (빈 람다)
        holder.followToggle.setChecked(originalState)
        updateFollowStatus(holder, originalState)
        // 리스너 다시 설정
        holder.followToggle.setOnCheckedChangeListener { isChecked ->
            if (isChecked != post.isFollowing) {
                handleFollowToggle(holder, post, holder.adapterPosition, isChecked)
            }
        }
    }
    
    /**
     * 팔로우 상태 변경을 다른 화면에 알리는 브로드캐스트 전송
     */
    private fun sendFollowUpdateBroadcast(context: Context, userId: Int, isFollowing: Boolean) {
        val intent = Intent("com.example.travelonna.FOLLOW_UPDATED")
        intent.putExtra("user_id", userId)
        intent.putExtra("is_following", isFollowing)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        Log.d(TAG, "팔로우 상태 변경 브로드캐스트 전송 - userId: $userId, isFollowing: $isFollowing")
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