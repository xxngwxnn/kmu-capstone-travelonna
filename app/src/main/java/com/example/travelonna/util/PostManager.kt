package com.example.travelonna.util

import com.example.travelonna.model.Post
import com.example.travelonna.model.Comment

object PostManager {
    private val posts = mutableMapOf<Long, Post>()
    private val comments = mutableMapOf<Long, MutableList<Comment>>()
    private val listeners = mutableListOf<PostUpdateListener>()
    private var nextCommentId = 1L
    
    interface PostUpdateListener {
        fun onPostUpdated(post: Post)
        fun onCommentsUpdated(postId: Long, comments: List<Comment>)
    }
    
    fun addListener(listener: PostUpdateListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: PostUpdateListener) {
        listeners.remove(listener)
    }
    
    fun initializePosts(postList: List<Post>) {
        posts.clear()
        comments.clear()
        postList.forEach { post ->
            posts[post.id] = post
            comments[post.id] = mutableListOf()
        }
    }
    
    fun getPost(id: Long): Post? {
        return posts[id]
    }
    
    fun updatePost(post: Post) {
        posts[post.id] = post
        notifyListeners(post)
    }
    
    fun toggleLike(postId: Long) {
        posts[postId]?.let { post ->
            post.isLiked = !post.isLiked
            if (post.isLiked) {
                post.likeCount++
            } else {
                post.likeCount--
            }
            notifyListeners(post)
        }
    }
    
    fun addComment(postId: Long, userName: String, content: String) {
        posts[postId]?.let { post ->
            val comment = Comment(
                id = nextCommentId++,
                postId = postId,
                userName = userName,
                content = content
            )
            
            // 댓글 리스트에 추가
            comments.getOrPut(postId) { mutableListOf() }.add(comment)
            
            // 게시물의 댓글 수 업데이트
            post.commentCount = comments[postId]?.size ?: 0
            
            notifyListeners(post)
            notifyCommentsUpdated(postId)
        }
    }
    
    fun getComments(postId: Long): List<Comment> {
        return comments[postId] ?: emptyList()
    }
    
    fun updateFollowStatus(postId: Long, isFollowing: Boolean) {
        posts[postId]?.let { post ->
            post.isFollowing = isFollowing
            notifyListeners(post)
        }
    }
    
    fun getAllPosts(): List<Post> {
        return posts.values.toList()
    }
    
    private fun notifyListeners(post: Post) {
        listeners.forEach { listener ->
            listener.onPostUpdated(post)
        }
    }
    
    private fun notifyCommentsUpdated(postId: Long) {
        val commentList = comments[postId] ?: emptyList()
        listeners.forEach { listener ->
            listener.onCommentsUpdated(postId, commentList)
        }
    }
} 