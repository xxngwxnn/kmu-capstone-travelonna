package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travelonna.R
import com.example.travelonna.adapter.CommentAdapter
import com.example.travelonna.api.*
import com.example.travelonna.model.Post
import com.example.travelonna.util.PostManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostDetailActivity : AppCompatActivity(), CommentAdapter.CommentActionListener {
    
    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
        const val RESULT_REFRESH_NEEDED = "result_refresh_needed"
        private const val TAG = "PostDetailActivity"
    }
    
    private lateinit var backButton: ImageView
    private lateinit var postImage: ImageView
    private lateinit var userName: TextView
    private lateinit var scheduleButton: Button
    private lateinit var postContent: TextView
    private lateinit var postDate: TextView
    private lateinit var likeIcon: ImageView
    private lateinit var likeCount: TextView
    private lateinit var commentIcon: ImageView
    private lateinit var commentCount: TextView
    private lateinit var commentEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var commentHeader: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var noCommentsText: TextView
    
    private lateinit var commentAdapter: CommentAdapter
    
    // 게시물 ID
    private var postId: Long = -1L
    private var currentPost: Post? = null
    private var hasDataChanged = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)
        
        // Initialize views
        initViews()
        
        // Set up listeners
        setupListeners()
        
        // Set up comments RecyclerView
        setupCommentsRecyclerView()
        
        // Load post data
        postId = intent.getLongExtra(EXTRA_POST_ID, -1L)
        if (postId != -1L) {
            loadPostData(postId)
            loadComments(postId)
        } else {
            Log.e(TAG, "No post ID provided")
            finish()
        }
    }
    
    private fun initViews() {
        try {
            backButton = findViewById(R.id.backButton)
            postImage = findViewById(R.id.postImage)
            userName = findViewById(R.id.userName)
            scheduleButton = findViewById(R.id.scheduleButton)
            postContent = findViewById(R.id.postContent)
            postDate = findViewById(R.id.postDate)
            likeIcon = findViewById(R.id.likeIcon)
            likeCount = findViewById(R.id.likeCount)
            commentIcon = findViewById(R.id.commentIcon)
            commentCount = findViewById(R.id.commentCount)
            commentEditText = findViewById(R.id.commentEditText)
            sendButton = findViewById(R.id.sendButton)
            commentHeader = findViewById(R.id.commentHeader)
            commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
            noCommentsText = findViewById(R.id.noCommentsText)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
        }
    }
    
    private fun setupListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked, calling finishWithResult()")
            finishWithResult()
        }
        
        // Like button click listener
        likeIcon.setOnClickListener {
            toggleLike()
        }
        
        // Schedule button click listener
        setupScheduleButton()
        
        // Comment send button click listener
        sendButton.setOnClickListener {
            sendComment()
        }
    }
    
    private fun setupCommentsRecyclerView() {
        commentAdapter = CommentAdapter(mutableListOf(), this)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentAdapter
    }
    
    private fun loadPostData(postId: Long) {
        RetrofitClient.apiService.getLogDetail(postId.toInt()).enqueue(object : Callback<LogDetailApiResponse> {
            override fun onResponse(call: Call<LogDetailApiResponse>, response: Response<LogDetailApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val logDetail = response.body()!!.data
                    if (logDetail != null) {
                        currentPost = Post(
                            id = logDetail.logId.toLong(),
                            imageResource = R.drawable.main_dummy_1,
                            imageUrl = logDetail.imageUrls.firstOrNull(),
                            hasImage = logDetail.imageUrls.isNotEmpty(),
                            userName = logDetail.userName,
                            userId = logDetail.userId,
                            isFollowing = false,
                            description = logDetail.comment,
                            date = logDetail.createdAt,
                            isLiked = logDetail.isLiked,
                            likeCount = logDetail.likeCount,
                            commentCount = logDetail.commentCount,
                            planId = logDetail.plan.planId
                        )
                        updateUI(currentPost!!)
                    }
                }
            }
            override fun onFailure(call: Call<LogDetailApiResponse>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateUI(post: Post) {
        userName.text = post.userName
        postContent.text = post.description
        postDate.text = post.date
        likeCount.text = post.likeCount.toString()
        commentCount.text = post.commentCount.toString()
        
        // 이미지 표시 로직 개선
        Log.d(TAG, "이미지 표시 확인 - hasImage: ${post.hasImage}, imageUrl: ${post.imageUrl}")
        if (post.hasImage && !post.imageUrl.isNullOrBlank()) {
            val imageUrl = post.imageUrl!!
            // 유효한 HTTP(S) URL인지 확인
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                postImage.visibility = View.VISIBLE
                Log.d(TAG, "실제 이미지 로드: $imageUrl")
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_place_holder)
                    .error(R.drawable.ic_place_holder)
                    .centerCrop()
                    .into(postImage)
            } else {
                Log.w(TAG, "유효하지 않은 이미지 URL: $imageUrl")
                postImage.visibility = View.GONE
            }
        } else {
            Log.d(TAG, "이미지가 없음 - 이미지 숨김")
            postImage.visibility = View.GONE
        }
        
        if (post.isLiked) {
            likeIcon.setImageResource(R.drawable.ic_like_filled)
            likeIcon.setColorFilter(getColor(R.color.red))
        } else {
            likeIcon.setImageResource(R.drawable.ic_like_outline)
            likeIcon.setColorFilter(getColor(R.color.gray_text))
        }
    }
    
    private fun showDefaultData() {
        userName.text = "사용자"
        postContent.text = "게시물 내용을 불러올 수 없습니다."
        postDate.text = "날짜 없음"
        likeCount.text = "0"
        commentCount.text = "0"
        postImage.visibility = android.view.View.GONE
        
        likeIcon.setImageResource(R.drawable.ic_like_outline)
        likeIcon.setColorFilter(getColor(R.color.gray_text))
    }
    
    private fun loadComments(postId: Long) {
        Log.d(TAG, "Loading comments for logId: $postId")
        
        RetrofitClient.apiService.getLogComments(postId.toInt()).enqueue(object : Callback<CommentsResponse> {
            override fun onResponse(call: Call<CommentsResponse>, response: Response<CommentsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val commentsResponse = response.body()!!
                    
                    if (commentsResponse.success) {
                        Log.d(TAG, "Comments loaded: ${commentsResponse.data?.size ?: 0} items")
                        updateCommentsUI(commentsResponse.data ?: emptyList())
                    } else {
                        Log.w(TAG, "Failed to load comments: ${commentsResponse.message}")
                        showCommentsError("댓글 로드에 실패했습니다.")
                    }
                } else {
                    Log.e(TAG, "HTTP error loading comments: ${response.code()}")
                    
                    when (response.code()) {
                        404 -> {
                            Log.w(TAG, "Log not found for comments")
                            // 404는 댓글이 없는 것으로 처리
                            updateCommentsUI(emptyList())
                        }
                        500 -> {
                            showCommentsError("서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.")
                        }
                        else -> {
                            showCommentsError("댓글을 불러올 수 없습니다.")
                        }
                    }
                }
            }
            
            override fun onFailure(call: Call<CommentsResponse>, t: Throwable) {
                Log.e(TAG, "Network error loading comments", t)
                showCommentsError("네트워크 연결을 확인해주세요.")
            }
        })
    }
    
    private fun updateCommentsUI(comments: List<CommentData>) {
        if (comments.isEmpty()) {
            noCommentsText.text = "아직 댓글이 없습니다"
            noCommentsText.visibility = View.VISIBLE
            commentsRecyclerView.visibility = View.GONE
        } else {
            noCommentsText.visibility = View.GONE
            commentsRecyclerView.visibility = View.VISIBLE
            commentAdapter.updateComments(comments)
        }
        
        // 댓글 헤더 업데이트
        val commentCountText = if (comments.isEmpty()) "댓글" else "댓글 ${comments.size}개"
        commentHeader.text = commentCountText
    }
    
    private fun showCommentsError(message: String) {
        // 이미 댓글이 표시된 경우 에러 메시지만 토스트로 표시
        if (commentAdapter.itemCount > 0) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            // 댓글이 없는 경우 에러 메시지를 텍스트로 표시
            noCommentsText.text = message
            noCommentsText.visibility = View.VISIBLE
            commentsRecyclerView.visibility = View.GONE
        }
    }
    
    private fun sendComment() {
        val commentText = commentEditText.text.toString().trim()
        if (commentText.isNotEmpty() && postId != -1L) {
            val request = CreateCommentRequest(
                comment = commentText,
                parentId = null
            )
            sendButton.isEnabled = false
            RetrofitClient.apiService.createComment(postId.toInt(), request).enqueue(object : Callback<CommentData> {
                override fun onResponse(call: Call<CommentData>, response: Response<CommentData>) {
                    sendButton.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        val newComment = response.body()!!
                        commentEditText.setText("")
                        commentAdapter.addComment(newComment)
                        if (commentAdapter.itemCount == 1) {
                            noCommentsText.visibility = View.GONE
                            commentsRecyclerView.visibility = View.VISIBLE
                        }
                        commentHeader.text = "댓글 ${commentAdapter.itemCount}개"
                        val currentCommentCount = commentCount.text.toString().toIntOrNull() ?: 0
                        commentCount.text = (currentCommentCount + 1).toString()
                        commentsRecyclerView.scrollToPosition(0)
                        hasDataChanged = true
                        Toast.makeText(this@PostDetailActivity, "댓글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                        // 댓글 목록 새로고침
                        loadComments(postId)
                        // Post 객체의 댓글 수 갱신 및 UI 갱신
                        currentPost?.commentCount = (currentPost?.commentCount ?: 0) + 1
                        updateUI(currentPost!!)
                    } else {
                        Toast.makeText(this@PostDetailActivity, "댓글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CommentData>, t: Throwable) {
                    sendButton.isEnabled = true
                    Toast.makeText(this@PostDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    
    private fun toggleLike() {
        if (currentPost == null) return
        RetrofitClient.apiService.toggleLogLike(currentPost!!.id.toInt()).enqueue(object : Callback<LikeToggleResponse> {
            override fun onResponse(call: Call<LikeToggleResponse>, response: Response<LikeToggleResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { likeResponse ->
                        val isLiked = likeResponse.data
                        currentPost?.let { post ->
                            post.isLiked = isLiked
                            post.likeCount = if (isLiked) post.likeCount + 1 else maxOf(0, post.likeCount - 1)
                            updateLikeUI()
                            // 데이터가 변경되었음을 표시
                            hasDataChanged = true
                            // PostManager에도 업데이트된 정보 반영
                            PostManager.updatePost(post)
                        }
                    }
                } else {
                    Toast.makeText(this@PostDetailActivity, "좋아요 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateLikeUI() {
        if (currentPost == null) return
        updateUI(currentPost!!)
    }
    
    private fun finishWithResult() {
        Log.d(TAG, "finishWithResult called - hasDataChanged: $hasDataChanged")
        if (hasDataChanged) {
            val resultIntent = Intent().apply {
                putExtra(RESULT_REFRESH_NEEDED, true)
            }
            setResult(RESULT_OK, resultIntent)
            Log.d(TAG, "Finishing with refresh result due to data changes")
        } else {
            Log.d(TAG, "No data changes detected, finishing normally")
        }
        finish()
    }
    
    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed called, calling finishWithResult()")
        finishWithResult()
    }

    // CommentActionListener 구현
    override fun onEditComment(comment: CommentData, position: Int) {
        // TODO: 댓글 수정 다이얼로그 구현
        Toast.makeText(this, "댓글 수정 기능 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteComment(comment: CommentData, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("댓글 삭제")
            .setMessage("정말로 이 댓글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                RetrofitClient.apiService.deleteComment(comment.commentId).enqueue(object : retrofit2.Callback<DeleteCommentResponse> {
                    override fun onResponse(call: retrofit2.Call<DeleteCommentResponse>, response: retrofit2.Response<DeleteCommentResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            commentAdapter.removeComment(position)
                            Toast.makeText(this@PostDetailActivity, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            commentHeader.text = "댓글 ${commentAdapter.itemCount}개"
                            val currentCommentCount = commentCount.text.toString().toIntOrNull() ?: 1
                            commentCount.text = (currentCommentCount - 1).toString()
                            // Post 객체의 댓글 수 갱신 및 UI 갱신
                            currentPost?.commentCount = (currentPost?.commentCount ?: 1) - 1
                            updateUI(currentPost!!)
                            // 데이터가 변경되었음을 표시
                            hasDataChanged = true
                            // PostManager에도 업데이트된 정보 반영
                            currentPost?.let { PostManager.updatePost(it) }
                        } else {
                            Toast.makeText(this@PostDetailActivity, "댓글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<DeleteCommentResponse>, t: Throwable) {
                        Toast.makeText(this@PostDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupScheduleButton() {
        scheduleButton.setOnClickListener {
            val planId = currentPost?.planId ?: return@setOnClickListener
            val userId = currentPost?.userId ?: return@setOnClickListener
            Log.d(TAG, "Schedule button clicked - planId: $planId, currentPost: $currentPost")
            
            // 현재 로그인한 사용자의 ID 가져오기
            val currentUserId = RetrofitClient.getUserId()
            Log.d(TAG, "Current user ID: $currentUserId, Post user ID: $userId")
            
            // 권한 확인을 위한 API 호출
            RetrofitClient.apiService.getPlanDetail(planId).enqueue(object : Callback<PlanDetailResponse> {
                override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                    when (response.code()) {
                        200 -> {
                            val planDetail = response.body()?.data
                            if (planDetail != null) {
                                // 일정 소유자 확인
                                if (planDetail.userId == currentUserId) {
                                    // 소유자인 경우 바로 이동
                                    navigateToTravelDetail(planId)
                                } else {
                                    // 소유자가 아닌 경우 공개 여부 확인
                                    if (planDetail.isPublic) {
                                        navigateToTravelDetail(planId)
                                    } else {
                                        Toast.makeText(this@PostDetailActivity, 
                                            "이 여행 계획은 비공개로 설정되어 있습니다.\n계획 소유자에게 공유를 요청해주세요.", 
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this@PostDetailActivity, 
                                    "일정 정보를 불러올 수 없습니다.\n잠시 후 다시 시도해주세요.", 
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                        400 -> {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Plan access error: $errorBody")
                            if (errorBody?.contains("권한이 없습니다") == true) {
                                Toast.makeText(this@PostDetailActivity, 
                                    "이 여행 계획에 대한 접근 권한이 없습니다.\n계획 소유자에게 공유를 요청해주세요.", 
                                    Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@PostDetailActivity, 
                                    "해당 여행 계획을 찾을 수 없습니다.\n계획이 삭제되었거나 변경되었을 수 있습니다.", 
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                        401 -> {
                            Toast.makeText(this@PostDetailActivity, 
                                "로그인이 필요합니다.\n다시 로그인해주세요.", 
                                Toast.LENGTH_LONG).show()
                            // 로그인 화면으로 이동
                            navigateToLogin()
                        }
                        403 -> {
                            Toast.makeText(this@PostDetailActivity, 
                                "접근 권한이 없습니다.\n계획 소유자에게 공유를 요청해주세요.", 
                                Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this@PostDetailActivity, 
                                "여행 계획을 불러오는데 실패했습니다.\n잠시 후 다시 시도해주세요. (${response.code()})", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                    Log.e(TAG, "Network error when checking plan access", t)
                    Toast.makeText(this@PostDetailActivity, 
                        "네트워크 오류가 발생했습니다.\n인터넷 연결을 확인해주세요.", 
                        Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun navigateToTravelDetail(planId: Int) {
        val intent = Intent(this@PostDetailActivity, TravelDetailActivity::class.java)
        intent.putExtra("PLAN_ID", planId)
        startActivity(intent)
    }

    private fun navigateToLogin() {
        val intent = Intent(this@PostDetailActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 