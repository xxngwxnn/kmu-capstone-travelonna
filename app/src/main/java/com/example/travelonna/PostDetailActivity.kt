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
            finishWithResult()
        }
        
        // Schedule button click listener
        scheduleButton.setOnClickListener {
            // 현재 게시물의 planId를 가져와서 전달
            val planId = currentPost?.planId ?: 0
            
            Log.d(TAG, "Schedule button clicked - planId: $planId, currentPost: $currentPost")
            
            if (planId > 0) {
                val intent = Intent(this, TravelDetailActivity::class.java).apply {
                    putExtra("PLAN_ID", planId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "연결된 여행 계획이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        
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
        val post = PostManager.getPost(postId)
        if (post != null) {
            currentPost = post
            updateUI(post)
            Log.d(TAG, "Post data loaded from PostManager: ${post.description}")
        } else {
            Log.w(TAG, "Post not found in PostManager: $postId")
            // 기본값으로 표시
            showDefaultData()
        }
    }
    
    private fun updateUI(post: Post) {
        userName.text = post.userName
        postContent.text = post.description
        postDate.text = post.date
        likeCount.text = post.likeCount.toString()
        commentCount.text = post.commentCount.toString()
        
        // 이미지 처리
        if (post.hasImage) {
            postImage.visibility = android.view.View.VISIBLE
            postImage.setImageResource(post.imageResource)
        } else {
            postImage.visibility = android.view.View.GONE
        }
        
        // 좋아요 상태
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
            Log.d(TAG, "Creating comment for logId: $postId")
            
            val request = CreateCommentRequest(
                comment = commentText
            )
            
            // 전송 버튼 비활성화 (중복 전송 방지)
            sendButton.isEnabled = false
            
            RetrofitClient.apiService.createComment(postId.toInt(), request).enqueue(object : Callback<CommentData> {
                override fun onResponse(call: Call<CommentData>, response: Response<CommentData>) {
                    sendButton.isEnabled = true
                    
                    if (response.isSuccessful && response.body() != null) {
                        val newComment = response.body()!!
                        Log.d(TAG, "Comment created successfully: ${newComment.commentId}")
                        
                        // 입력 필드 클리어
                        commentEditText.setText("")
                        
                        // 새 댓글을 즉시 목록에 추가
                        commentAdapter.addComment(newComment)
                        
                        // 댓글 목록이 비어있었다면 UI 업데이트
                        if (commentAdapter.itemCount == 1) {
                            noCommentsText.visibility = View.GONE
                            commentsRecyclerView.visibility = View.VISIBLE
                        }
                        
                        // 댓글 헤더 업데이트
                        commentHeader.text = "댓글 ${commentAdapter.itemCount}개"
                        
                        // 댓글 수 UI 업데이트
                        val currentCommentCount = commentCount.text.toString().toIntOrNull() ?: 0
                        commentCount.text = (currentCommentCount + 1).toString()
                        
                        // 새 댓글로 스크롤
                        commentsRecyclerView.scrollToPosition(0)
                        hasDataChanged = true
                        
                        Toast.makeText(this@PostDetailActivity, "댓글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "HTTP error creating comment: ${response.code()}")
                        
                        when (response.code()) {
                            400 -> {
                                Toast.makeText(this@PostDetailActivity, "잘못된 요청입니다.", Toast.LENGTH_SHORT).show()
                            }
                            404 -> {
                                Toast.makeText(this@PostDetailActivity, "게시물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this@PostDetailActivity, "댓글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                
                override fun onFailure(call: Call<CommentData>, t: Throwable) {
                    sendButton.isEnabled = true
                    Log.e(TAG, "Network error creating comment", t)
                    Toast.makeText(this@PostDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    
    private fun finishWithResult() {
        if (hasDataChanged) {
            val resultIntent = Intent().apply {
                putExtra(RESULT_REFRESH_NEEDED, true)
            }
            setResult(RESULT_OK, resultIntent)
            Log.d(TAG, "Finishing with refresh result due to data changes")
        }
        finish()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithResult()
    }

    // CommentActionListener 구현
    override fun onEditComment(comment: CommentData, position: Int) {
        // TODO: 댓글 수정 다이얼로그 구현
        Toast.makeText(this, "댓글 수정 기능 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteComment(comment: CommentData, position: Int) {
        // TODO: 댓글 삭제 확인 다이얼로그 구현
        Toast.makeText(this, "댓글 삭제 기능 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
} 