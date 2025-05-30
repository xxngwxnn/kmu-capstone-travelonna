package com.example.travelonna

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import com.example.travelonna.model.Post
import com.example.travelonna.model.Comment
import com.example.travelonna.util.PostManager
import com.example.travelonna.adapter.CommentAdapter

class PostDetailActivity : AppCompatActivity(), PostManager.PostUpdateListener {
    
    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
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
    private lateinit var followText: TextView
    private lateinit var followToggle: Switch
    private lateinit var commentHeader: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var noCommentsText: TextView
    
    private lateinit var commentAdapter: CommentAdapter
    
    // 게시물 ID
    private var postId: Long = -1L
    private var currentPost: Post? = null
    
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
            // If no post ID, load dummy data for preview
            loadDummyData()
        }
        
        // PostManager 리스너 등록
        PostManager.addListener(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 메모리 누수 방지를 위해 리스너 제거
        PostManager.removeListener(this)
    }
    
    private fun initViews() {
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
        followText = findViewById(R.id.followText)
        followToggle = findViewById(R.id.followToggle)
        commentHeader = findViewById(R.id.commentHeader)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        noCommentsText = findViewById(R.id.noCommentsText)
    }
    
    private fun setupCommentsRecyclerView() {
        commentAdapter = CommentAdapter(emptyList())
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentAdapter
    }
    
    private fun setupListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }
        
        // Like button click listener
        likeIcon.setOnClickListener {
            if (postId != -1L) {
                PostManager.toggleLike(postId)
            }
        }
        
        // Comment send button click listener
        sendButton.setOnClickListener {
            sendComment()
        }
        
        // Schedule button click listener
        scheduleButton.setOnClickListener {
            // 팝업 대신 액티비티로 전환
            val intent = Intent(this, TravelDetailActivity::class.java).apply {
                putExtra("PLAN_ID", postId)
            }
            startActivity(intent)
        }
        
        // Follow toggle listener
        followToggle.setOnCheckedChangeListener { _, isChecked ->
            if (postId != -1L) {
                PostManager.updateFollowStatus(postId, isChecked)
            }
        }
    }
    
    private fun loadPostData(postId: Long) {
        // PostManager에서 게시물 데이터 가져오기
        currentPost = PostManager.getPost(postId)
        currentPost?.let { post ->
            updateUI(post)
        } ?: run {
            // 게시물을 찾을 수 없으면 더미 데이터 로드
            loadDummyData()
        }
    }
    
    private fun loadComments(postId: Long) {
        val comments = PostManager.getComments(postId)
        updateCommentsUI(comments)
    }
    
    private fun updateCommentsUI(comments: List<Comment>) {
        if (comments.isEmpty()) {
            noCommentsText.visibility = View.VISIBLE
            commentsRecyclerView.visibility = View.GONE
        } else {
            noCommentsText.visibility = View.GONE
            commentsRecyclerView.visibility = View.VISIBLE
            commentAdapter.updateData(comments)
        }
        
        // 댓글 헤더 업데이트
        val commentCountText = if (comments.isEmpty()) "댓글" else "댓글 ${comments.size}개"
        commentHeader.text = commentCountText
    }
    
    private fun updateUI(post: Post) {
        postImage.setImageResource(post.imageResource)
        userName.text = post.userName
        postContent.text = post.description
        postDate.text = post.date
        likeCount.text = post.likeCount.toString()
        commentCount.text = post.commentCount.toString()
        
        // 좋아요 상태 업데이트
        updateLikeStatus(post.isLiked)
        
        // 팔로우 상태 설정
        followToggle.isChecked = post.isFollowing
        updateFollowStatus(post.isFollowing)
    }
    
    private fun updateLikeStatus(isLiked: Boolean) {
        if (isLiked) {
            likeIcon.setImageResource(R.drawable.ic_like_filled)
            likeIcon.setColorFilter(getColor(R.color.red))
        } else {
            likeIcon.setImageResource(R.drawable.ic_like_filled)
            likeIcon.setColorFilter(getColor(R.color.gray_text))
        }
    }
    
    private fun loadDummyData() {
        // Set dummy post data
        postImage.setImageResource(R.drawable.main_dummy_1)
        userName.text = "Pro_traveler_kr"
        postContent.text = "지난 주에 일본 가벼지만을 다녀왔어요~ 모던하면서도 자분하고 깔끔한 느낌이었어요. 그 외 내용들을 적어주세요. 그 외 내용들을 적어주세요. 그외 내용들을 적어주세요. 그 외 내용들을 적어주세요. 그 외 내용들을 적어주세요."
        postDate.text = "2025.08.17"
        likeCount.text = "125"
        commentCount.text = "23"
        
        // 팔로우 상태 설정 (초기 상태를 팔로우 중으로 설정)
        followToggle.isChecked = true
        updateFollowStatus(true)
        
        // 빈 댓글 목록 표시
        updateCommentsUI(emptyList())
    }
    
    private fun updateFollowStatus(isFollowing: Boolean) {
        if (isFollowing) {
            followText.text = "팔로우 중"
            followText.setTextColor(getColor(R.color.blue))
        } else {
            followText.text = "팔로우"
            followText.setTextColor(getColor(R.color.gray_text))
        }
    }
    
    private fun sendComment() {
        val commentText = commentEditText.text.toString().trim()
        if (commentText.isNotEmpty() && postId != -1L) {
            // 현재 사용자 이름 (임시로 "나"로 설정)
            val currentUserName = "나"
            
            // 댓글 추가
            PostManager.addComment(postId, currentUserName, commentText)
            
            // 입력 필드 클리어
            commentEditText.setText("")
        }
    }
    
    override fun onPostUpdated(post: Post) {
        // 현재 게시물이 업데이트되면 UI 반영
        if (post.id == postId) {
            runOnUiThread {
                currentPost = post
                updateUI(post)
            }
        }
    }
    
    override fun onCommentsUpdated(postId: Long, comments: List<Comment>) {
        // 현재 게시물의 댓글이 업데이트되면 UI 반영
        if (postId == this.postId) {
            runOnUiThread {
                updateCommentsUI(comments)
            }
        }
    }
} 