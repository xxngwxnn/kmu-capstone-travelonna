package com.example.travelonna.adapter

import android.content.Intent
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travelonna.PostDetailActivity
import com.example.travelonna.HomeActivity
import com.example.travelonna.view.CustomToggleButton
import com.example.travelonna.R
import com.example.travelonna.api.RecommendationItem
import com.example.travelonna.api.LikeToggleResponse
import com.example.travelonna.api.LogDetailApiResponse
import com.example.travelonna.api.LogDetailData
import com.example.travelonna.api.CommentsResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.FollowRequest
import com.example.travelonna.api.FollowResponse
import com.example.travelonna.api.UnfollowResponse
import com.example.travelonna.api.FollowStatusResponse
import com.example.travelonna.model.Post
import com.example.travelonna.util.PostManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class RecommendationAdapter : RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {

    interface OnPostClickListener {
        fun onPostClick(intent: Intent)
    }

    private val TAG = "RecommendationAdapter"
    private val items = mutableListOf<RecommendationItem>()
    private var onPostClickListener: OnPostClickListener? = null
    
    // 상세 정보를 캐시하기 위한 Map
    private val logDetailCache = mutableMapOf<Int, LogDetailData>()

    class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val postDescription: TextView = itemView.findViewById(R.id.description)
        val postDate: TextView = itemView.findViewById(R.id.date)
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        val likeCount: TextView = itemView.findViewById(R.id.likeCount)
        val commentIcon: ImageView = itemView.findViewById(R.id.commentIcon)
        val commentCount: TextView = itemView.findViewById(R.id.commentCount)
        // val followText: TextView = itemView.findViewById(R.id.followText)
        // val followToggle: CustomToggleButton = itemView.findViewById(R.id.followToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        val item = items[position]
        bind(item, holder)
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<RecommendationItem>) {
        items.clear()
        items.addAll(newItems)
        // 새로운 데이터로 업데이트할 때 캐시 클리어
        logDetailCache.clear()
        notifyDataSetChanged()
    }
    
    fun appendData(newItems: List<RecommendationItem>) {
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }
    
    fun setOnPostClickListener(listener: OnPostClickListener) {
        this.onPostClickListener = listener
    }

    private fun bind(item: RecommendationItem, holder: RecommendationViewHolder) {
        // 기본 정보 먼저 설정
        holder.postDescription.text = item.comment
        holder.postDate.text = formatDate(item.createdAt)
        
        // 캐시된 상세 정보가 있으면 이미지 설정, 없으면 기본적으로 숨김
        val cachedDetail = logDetailCache[item.logId]
        val hasImage = cachedDetail?.imageUrls?.isNotEmpty() == true
        
        if (hasImage) {
            holder.postImage.visibility = View.VISIBLE
            // TODO: Glide를 사용하여 실제 이미지 로드
            holder.postImage.setImageResource(R.drawable.main_dummy_1)
        } else {
            // 이미지가 없는 경우 이미지뷰 숨김
            holder.postImage.visibility = View.GONE
        }
        
        // 캐시된 상세 정보가 있으면 사용, 없으면 API 호출
        if (cachedDetail != null) {
            bindDetailData(holder, cachedDetail)
        } else {
            // 기본값으로 먼저 설정 - 추천 데이터의 정보 활용
            holder.userName.text = "User_${item.userId}"
            holder.likeCount.text = "-"  // 로딩 중 표시를 위해 "-" 사용
            holder.commentCount.text = "-"  // 로딩 중 표시를 위해 "-" 사용
            updateLikeUI(holder, false)
            
            // API 호출해서 실제 데이터 가져오기
            loadLogDetail(item.logId, holder, item)
        }
        
        // 게시물 클릭 시 PostDetailActivity로 이동 (실제 데이터 사용)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            
            // RecommendationItem을 Post로 변환
            val post = convertRecommendationToPost(item, cachedDetail)
            
            // PostManager에 게시물 등록 (이미 있다면 업데이트)
            PostManager.updatePost(post)
            
            // PostDetailActivity로 이동
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            }
            
            // 콜백이 설정되어 있으면 콜백 사용, 아니면 기본 방식
            if (onPostClickListener != null) {
                onPostClickListener?.onPostClick(intent)
            } else {
                context.startActivity(intent)
            }
        }
        
        // 좋아요 버튼 클릭 리스너 - 실제 API 호출
        holder.likeIcon.setOnClickListener {
            toggleLike(item.logId, holder)
        }
        
        // 댓글 버튼 클릭 리스너
        holder.commentIcon.setOnClickListener {
            val context = holder.itemView.context
            
            // RecommendationItem을 Post로 변환
            val post = convertRecommendationToPost(item, cachedDetail)
            
            // PostManager에 게시물 등록
            PostManager.updatePost(post)
            
            // PostDetailActivity로 이동 (댓글 섹션으로 스크롤)
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
                putExtra("SCROLL_TO_COMMENTS", true)
            }
            
            // 콜백이 설정되어 있으면 콜백 사용, 아니면 기본 방식
            if (onPostClickListener != null) {
                onPostClickListener?.onPostClick(intent)
            } else {
                context.startActivity(intent)
            }
        }
        
        // 팔로우 토글/텍스트는 item_post.xml에 없으므로 무시
    }

    private fun loadLogDetail(logId: Int, holder: RecommendationViewHolder, recommendation: RecommendationItem) {
        Log.d(TAG, "Starting to load log detail for logId: $logId")
        RetrofitClient.apiService.getLogDetail(logId).enqueue(object : Callback<LogDetailApiResponse> {
            override fun onResponse(call: Call<LogDetailApiResponse>, response: Response<LogDetailApiResponse>) {
                Log.d(TAG, "API response received for logId: $logId, code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    Log.d(TAG, "API response body: success=${apiResponse.success}, data=${apiResponse.data}")
                    
                    if (apiResponse.success && apiResponse.data != null) {
                        val logDetail = apiResponse.data
                        Log.d(TAG, "Log detail data: userName=${logDetail.userName}, likeCount=${logDetail.likeCount}, commentCount=${logDetail.commentCount}")
                        
                        // 캐시에 저장
                        logDetailCache[logId] = logDetail
                        
                        // UI 업데이트
                        bindDetailData(holder, logDetail)
                        
                        Log.d(TAG, "Log detail loaded successfully for logId: $logId")
                    } else {
                        Log.w(TAG, "Failed to load log detail: ${apiResponse.message}")
                        // API 호출은 성공했지만 데이터가 없는 경우 기본값으로 설정
                        setDefaultValues(holder, recommendation)
                    }
                } else {
                    Log.e(TAG, "HTTP error loading log detail: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error response body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read error body", e)
                    }
                    // HTTP 에러 시 기본값으로 설정
                    setDefaultValues(holder, recommendation)
                }
            }
            
            override fun onFailure(call: Call<LogDetailApiResponse>, t: Throwable) {
                Log.e(TAG, "Network error loading log detail for logId: $logId", t)
                // 네트워크 에러 시 기본값으로 설정
                setDefaultValues(holder, recommendation)
            }
        })
    }
    
    /**
     * API 호출 실패 시 기본값 설정
     */
    private fun setDefaultValues(holder: RecommendationViewHolder, recommendation: RecommendationItem) {
        // API 실패 시에도 사용자에게 더 나은 경험 제공
        // 추천 데이터의 기본 정보 활용
        holder.userName.text = "User_${recommendation.userId}"
        
        // 좋아요는 가져올 수 없지만, 댓글 수는 별도 API로 시도
        holder.likeCount.text = "?"  // 서버 문제로 불러올 수 없음을 표시
        holder.commentCount.text = "-"  // 로딩 중 표시
        updateLikeUI(holder, false)
        
        Log.d(TAG, "Set default values for logId: ${recommendation.logId}, likes: '${holder.likeCount.text}', comments: '${holder.commentCount.text}'")
        Log.d(TAG, "Attempting to load comment count separately for logId: ${recommendation.logId}")
        loadCommentCount(recommendation.logId, holder, recommendation)
    }
    
    /**
     * 댓글 수만 별도로 로드
     */
    private fun loadCommentCount(logId: Int, holder: RecommendationViewHolder, recommendation: RecommendationItem) {
        RetrofitClient.apiService.getLogComments(logId).enqueue(object : Callback<CommentsResponse> {
            override fun onResponse(call: Call<CommentsResponse>, response: Response<CommentsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val commentsResponse = response.body()!!
                    
                    if (commentsResponse.success) {
                        val commentCount = commentsResponse.data.size
                        
                        // UI 업데이트를 메인 스레드에서 강제 실행
                        holder.itemView.post {
                            holder.commentCount.text = commentCount.toString()
                            Log.d(TAG, "Comment count UI updated on main thread for logId: $logId, count: $commentCount")
                            Log.d(TAG, "TextView now displays: '${holder.commentCount.text}'")
                        }
                        
                        Log.d(TAG, "Comment count loaded successfully for logId: $logId, count: $commentCount")
                        
                        // PostManager에 업데이트된 댓글 수 반영
                        updatePostInManager(recommendation, commentCount)
                    } else {
                        holder.commentCount.text = "?"
                        Log.w(TAG, "Failed to load comment count: ${commentsResponse.message}")
                    }
                } else {
                    holder.commentCount.text = "?"
                    Log.e(TAG, "HTTP error loading comment count: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<CommentsResponse>, t: Throwable) {
                holder.commentCount.text = "?"
                Log.e(TAG, "Network error loading comment count for logId: $logId", t)
            }
        })
    }
    
    /**
     * 상세 정보를 UI에 바인딩
     */
    private fun bindDetailData(holder: RecommendationViewHolder, logDetail: LogDetailData) {
        // 사용자 정보
        holder.userName.text = logDetail.userName
        
        // 좋아요 정보
        holder.likeCount.text = logDetail.likeCount.toString()
        updateLikeUI(holder, logDetail.isLiked)
        
        // 댓글 정보
        holder.commentCount.text = logDetail.commentCount.toString()
        
        // 이미지 처리 - 이미지가 있는 경우만 표시
        if (logDetail.imageUrls.isNotEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(logDetail.imageUrls[0])
                .placeholder(R.drawable.main_dummy_1)
                .error(R.drawable.main_dummy_1)
                .into(holder.postImage)
        } else {
            // 이미지가 없는 경우 이미지뷰 숨김
            holder.postImage.visibility = View.GONE
        }
    }
    
    /**
     * 좋아요 토글 API 호출
     */
    private fun toggleLike(logId: Int, holder: RecommendationViewHolder) {
        Log.d(TAG, "Toggling like for logId: $logId")
        
        // 현재 좋아요 상태 확인
        val isCurrentlyLiked = holder.likeIcon.tag == "liked"
        
        // 즉시 UI 업데이트 (낙관적 업데이트)
        updateLikeUI(holder, !isCurrentlyLiked)
        
        // API 호출
        RetrofitClient.apiService.toggleLogLike(logId).enqueue(object : Callback<LikeToggleResponse> {
            override fun onResponse(call: Call<LikeToggleResponse>, response: Response<LikeToggleResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { likeResponse ->
                        val position = holder.adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val isLiked = likeResponse.data
                            // 캐시 업데이트
                            logDetailCache[logId]?.let { cachedDetail ->
                                val currentCount = cachedDetail.likeCount
                                val newCount = if (isLiked) currentCount + 1 else maxOf(0, currentCount - 1)
                                val updatedDetail = cachedDetail.copy(
                                    isLiked = isLiked,
                                    likeCount = newCount
                                )
                                logDetailCache[logId] = updatedDetail
                            }
                            // UI 업데이트
                            updateLikeUI(holder, isLiked)
                            val currentCount = holder.likeCount.text.toString().toIntOrNull() ?: 0
                            val newCount = if (isLiked) currentCount + 1 else maxOf(0, currentCount - 1)
                            holder.likeCount.text = newCount.toString()
                        }
                    }
                } else {
                    Toast.makeText(holder.itemView.context, "좋아요 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Log.e(TAG, "Like toggle network error", t)
                
                // 네트워크 에러 시 원래 상태로 되돌리기
                updateLikeUI(holder, isCurrentlyLiked)
                Toast.makeText(holder.itemView.context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    /**
     * 좋아요 UI 업데이트
     */
    private fun updateLikeUI(holder: RecommendationViewHolder, isLiked: Boolean) {
        if (isLiked) {
            holder.likeIcon.setImageResource(R.drawable.ic_like_filled)
            holder.likeIcon.setColorFilter(holder.itemView.context.getColor(R.color.red))
            holder.likeIcon.tag = "liked"
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_like_outline)
            holder.likeIcon.setColorFilter(holder.itemView.context.getColor(R.color.gray_text))
            holder.likeIcon.tag = "unliked"
        }
    }
    
    /**
     * RecommendationItem을 Post로 변환하는 함수
     */
    private fun convertRecommendationToPost(recommendation: RecommendationItem, logDetail: LogDetailData?): Post {
        val hasImage = logDetail?.imageUrls?.isNotEmpty() == true
        val imageUrl = if (hasImage) logDetail?.imageUrls?.get(0) else null
        
        return Post(
            id = recommendation.logId.toLong(), // logId를 Post ID로 사용
            imageResource = R.drawable.main_dummy_1, // 기본 이미지, 실제로는 이미지 URL 처리 필요
            imageUrl = imageUrl, // 실제 이미지 URL
            hasImage = hasImage, // 이미지 유무
            userName = logDetail?.userName ?: "User_${recommendation.userId}",
            userId = recommendation.userId, // 게시물 작성자 ID
            isFollowing = false, // 기본값, 실제로는 팔로우 상태 API 호출 필요
            description = recommendation.comment,
            date = formatDate(recommendation.createdAt),
            isLiked = logDetail?.isLiked ?: false,
            likeCount = logDetail?.likeCount ?: 0,
            commentCount = logDetail?.commentCount ?: 0,
            planId = recommendation.planId // 여행 계획 ID 추가
        )
    }
    
    /**
     * PostManager에 팔로우 상태 업데이트
     */
    private fun updateFollowStatusInPostManager(recommendation: RecommendationItem, isFollowing: Boolean) {
        val postId = recommendation.logId.toLong()
        val existingPost = PostManager.getPost(postId)
        
        Log.d(TAG, "Updating follow status in PostManager for logId: $postId, isFollowing: $isFollowing")
        Log.d(TAG, "PostManager currently has post for ID $postId: ${existingPost != null}")
        
        if (existingPost != null) {
            // 기존 Post가 있으면 팔로우 상태만 업데이트
            Log.d(TAG, "Before update: existingPost.isFollowing = ${existingPost.isFollowing}")
            val updatedPost = existingPost.copy(isFollowing = isFollowing)
            PostManager.updatePost(updatedPost)
            Log.d(TAG, "Updated existing post follow status in PostManager. PostId: $postId, isFollowing: $isFollowing")
            
            // 업데이트 후 검증
            val verifyPost = PostManager.getPost(postId)
            Log.d(TAG, "After update verification: PostManager.getPost($postId).isFollowing = ${verifyPost?.isFollowing}")
        } else {
            // 새 Post 생성해서 등록
            val cachedDetail = logDetailCache[recommendation.logId]
            val hasImage = cachedDetail?.imageUrls?.isNotEmpty() == true
            val imageUrl = if (hasImage) cachedDetail?.imageUrls?.get(0) else null
            
            val post = Post(
                id = postId,
                imageResource = R.drawable.main_dummy_1,
                imageUrl = imageUrl,
                hasImage = hasImage,
                userName = cachedDetail?.userName ?: "User_${recommendation.userId}",
                userId = recommendation.userId,
                isFollowing = isFollowing,
                description = recommendation.comment,
                date = formatDate(recommendation.createdAt),
                isLiked = cachedDetail?.isLiked ?: false,
                likeCount = cachedDetail?.likeCount ?: 0,
                commentCount = cachedDetail?.commentCount ?: 0,
                planId = recommendation.planId
            )
            PostManager.updatePost(post)
            Log.d(TAG, "Created new post with follow status in PostManager. PostId: $postId, isFollowing: $isFollowing, userId: ${recommendation.userId}")
            
            // 생성 후 검증
            val verifyPost = PostManager.getPost(postId)
            Log.d(TAG, "After creation verification: PostManager.getPost($postId).isFollowing = ${verifyPost?.isFollowing}")
        }
    }
    
    /**
     * PostManager에 댓글 수 업데이트
     */
    private fun updatePostInManager(recommendation: RecommendationItem, commentCount: Int) {
        val postId = recommendation.logId.toLong()
        val existingPost = PostManager.getPost(postId)
        
        Log.d(TAG, "Updating PostManager for logId: $postId, commentCount: $commentCount")
        
        if (existingPost != null) {
            // 기존 Post가 있으면 댓글 수만 업데이트
            val updatedPost = existingPost.copy(commentCount = commentCount)
            PostManager.updatePost(updatedPost)
            Log.d(TAG, "Updated existing post in PostManager. PostId: $postId, old comments: ${existingPost.commentCount}, new comments: $commentCount")
        } else {
            // 새 Post 생성해서 등록
            val cachedDetail = logDetailCache[recommendation.logId]
            val hasImage = cachedDetail?.imageUrls?.isNotEmpty() == true
            val imageUrl = if (hasImage) cachedDetail?.imageUrls?.get(0) else null
            
            val post = Post(
                id = postId,
                imageResource = R.drawable.main_dummy_1,
                imageUrl = imageUrl,
                hasImage = hasImage,
                userName = cachedDetail?.userName ?: "User_${recommendation.userId}",
                userId = recommendation.userId,
                isFollowing = false,
                description = recommendation.comment,
                date = formatDate(recommendation.createdAt),
                isLiked = cachedDetail?.isLiked ?: false,
                likeCount = cachedDetail?.likeCount ?: 0,
                commentCount = commentCount,
                planId = recommendation.planId
            )
            PostManager.updatePost(post)
            Log.d(TAG, "Created new post in PostManager. PostId: $postId, commentCount: $commentCount")
        }
    }
    
    /**
     * 팔로우 상태 UI 업데이트
     */
    private fun updateFollowStatusUI(holder: RecommendationViewHolder, isFollowing: Boolean) {
        if (isFollowing) {
            // holder.followText.text = "팔로잉"
            // holder.followText.setTextColor(holder.itemView.context.getColor(R.color.blue))
        } else {
            // holder.followText.text = "팔로우"
            // holder.followText.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
        }
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // 파싱에 실패하면 원본 문자열의 앞 10자리만 반환 (yyyy-MM-dd)
            dateString.take(10).replace("-", ".")
        }
    }
    
    /**
     * 팔로우 토글 설정
     */
    private fun setupFollowToggle(holder: RecommendationViewHolder, recommendation: RecommendationItem) {
        // 자기 자신의 게시물인지 확인
        val currentUserId = RetrofitClient.getUserId()
        if (currentUserId == recommendation.userId) {
            // 자기 자신의 게시물이면 팔로우 버튼 숨기기
            // holder.followToggle.visibility = View.GONE
            return
        }
        
        // holder.followToggle.visibility = View.VISIBLE
        
        // PostManager에서 현재 팔로우 상태 먼저 확인
        val postId = recommendation.logId.toLong()
        val existingPost = PostManager.getPost(postId)
        
        if (existingPost != null) {
            // PostManager에 이미 게시물이 있으면 그 상태를 우선 사용
            Log.d(TAG, "Found existing post in PostManager for logId: $postId, follow status: ${existingPost.isFollowing}")
            // holder.followToggle.setChecked(existingPost.isFollowing)
            updateFollowStatusUI(holder, existingPost.isFollowing)
        } else {
            // PostManager에 없으면 기본값으로 설정
            // holder.followToggle.setChecked(false)
            updateFollowStatusUI(holder, false)
        }
        
        // 실제 팔로우 상태를 서버에서 확인 (PostManager와 다를 경우에만 업데이트)
        loadFollowStatus(holder, recommendation)
        
        // 팔로우 토글 리스너 설정
        // holder.followToggle.setOnCheckedChangeListener { isChecked ->
        //     toggleFollow(holder, recommendation, isChecked)
        // }
    }
    
    /**
     * 팔로우 토글 실행
     */
    private fun toggleFollow(holder: RecommendationViewHolder, recommendation: RecommendationItem, shouldFollow: Boolean) {
        Log.d(TAG, "Toggling follow for userId: ${recommendation.userId}, shouldFollow: $shouldFollow")
        
        if (shouldFollow) {
            // 팔로우하기
            val currentUserId = RetrofitClient.getUserId()
            val request = FollowRequest(toUser = recommendation.userId, fromUser = currentUserId)
            RetrofitClient.apiService.followUser(request).enqueue(object : Callback<FollowResponse> {
                override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val followResponse = response.body()!!
                        
                        if (followResponse.success && followResponse.data != null) {
                            val isFollowing = followResponse.data.isFollowing
                            Log.d(TAG, "Follow successful: following=$isFollowing")
                            
                            // PostManager에 팔로우 상태 업데이트
                            updateFollowStatusInPostManager(recommendation, isFollowing)
                            
                            // UI 업데이트
                            updateFollowStatusUI(holder, isFollowing)
                            
                            // 브로드캐스트 전송으로 다른 화면에 알림
                            sendFollowUpdateBroadcast(holder.itemView.context, recommendation.userId, isFollowing)
                            
                            Toast.makeText(holder.itemView.context, "팔로우했습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w(TAG, "Follow failed: ${followResponse.message}")
                            revertFollowToggle(holder, false)
                            Toast.makeText(holder.itemView.context, "팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Follow HTTP error: ${response.code()}")
                        revertFollowToggle(holder, false)
                        Toast.makeText(holder.itemView.context, "팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
                    Log.e(TAG, "Follow network error", t)
                    revertFollowToggle(holder, false)
                    Toast.makeText(holder.itemView.context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // 언팔로우하기
            RetrofitClient.apiService.unfollowUser(recommendation.userId).enqueue(object : Callback<UnfollowResponse> {
                override fun onResponse(call: Call<UnfollowResponse>, response: Response<UnfollowResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val unfollowResponse = response.body()!!
                        
                        if (unfollowResponse.success) {
                            Log.d(TAG, "Unfollow successful")
                            
                            // PostManager에 팔로우 상태 업데이트
                            updateFollowStatusInPostManager(recommendation, false)
                            
                            // UI 업데이트
                            updateFollowStatusUI(holder, false)
                            
                            // 브로드캐스트 전송으로 다른 화면에 알림
                            sendFollowUpdateBroadcast(holder.itemView.context, recommendation.userId, false)
                            
                            Toast.makeText(holder.itemView.context, "언팔로우했습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w(TAG, "Unfollow failed: ${unfollowResponse.message}")
                            revertFollowToggle(holder, true)
                            Toast.makeText(holder.itemView.context, "언팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Unfollow HTTP error: ${response.code()}")
                        revertFollowToggle(holder, true)
                        Toast.makeText(holder.itemView.context, "언팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<UnfollowResponse>, t: Throwable) {
                    Log.e(TAG, "Unfollow network error", t)
                    revertFollowToggle(holder, true)
                    Toast.makeText(holder.itemView.context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    
    /**
     * 팔로우 상태 로드
     */
    private fun loadFollowStatus(holder: RecommendationViewHolder, recommendation: RecommendationItem) {
        Log.d(TAG, "Loading follow status for userId: ${recommendation.userId}")
        
        RetrofitClient.apiService.getFollowStatus(recommendation.userId).enqueue(object : Callback<FollowStatusResponse> {
            override fun onResponse(call: Call<FollowStatusResponse>, response: Response<FollowStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val statusResponse = response.body()!!
                    
                    if (statusResponse.success && statusResponse.data != null) {
                        // data에서 isFollowing 속성 직접 사용
                        val serverFollowStatus = statusResponse.data.isFollowing
                        
                        // PostManager에서 현재 상태 확인
                        val postId = recommendation.logId.toLong()
                        val existingPost = PostManager.getPost(postId)
                        val currentFollowStatus = existingPost?.isFollowing ?: false
                        
                        Log.d(TAG, "Server follow status for userId ${recommendation.userId}: $serverFollowStatus, PostManager status: $currentFollowStatus")
                        
                        // 서버 상태와 PostManager 상태가 다를 경우에만 업데이트
                        if (serverFollowStatus != currentFollowStatus) {
                            Log.d(TAG, "Follow status mismatch detected for userId ${recommendation.userId}, updating to server status: $serverFollowStatus")
                            
                            // UI 업데이트 (리스너 제거 후 설정하여 무한 루프 방지)
                            // holder.followToggle.setOnCheckedChangeListener { }
                            // holder.followToggle.setChecked(serverFollowStatus)
                            // updateFollowStatusUI(holder, serverFollowStatus)
                            // holder.followToggle.setOnCheckedChangeListener { isChecked ->
                            //     toggleFollow(holder, recommendation, isChecked)
                            // }
                            
                            // PostManager에 팔로우 상태 업데이트
                            updateFollowStatusInPostManager(recommendation, serverFollowStatus)
                        } else {
                            Log.d(TAG, "Follow status in sync for userId ${recommendation.userId}, no update needed")
                        }
                    } else {
                        Log.w(TAG, "Failed to load follow status: ${statusResponse.message}")
                    }
                } else {
                    Log.e(TAG, "Follow status HTTP error: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<FollowStatusResponse>, t: Throwable) {
                Log.e(TAG, "Follow status network error for userId ${recommendation.userId}", t)
            }
        })
    }
    
    /**
     * 팔로우 토글 상태 되돌리기
     */
    private fun revertFollowToggle(holder: RecommendationViewHolder, originalState: Boolean) {
        // holder.followToggle.setOnCheckedChangeListener { }
        // holder.followToggle.setChecked(originalState)
        // 리스너를 다시 설정해야 하지만, 여기서는 position 정보가 없으므로 일시적으로 제거
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
} 