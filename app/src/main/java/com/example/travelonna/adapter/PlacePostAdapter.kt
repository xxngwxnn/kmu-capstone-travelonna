package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Post

class PlacePostAdapter(
    var posts: List<Post>,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<PlacePostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val userName: TextView = view.findViewById(R.id.userName)
        val description: TextView = view.findViewById(R.id.description)
        val date: TextView = view.findViewById(R.id.date)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val commentCount: TextView = view.findViewById(R.id.commentCount)
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
        holder.likeCount.text = post.likeCount.toString()
        holder.commentCount.text = post.commentCount.toString()
        holder.itemView.setOnClickListener { onItemClick(post) }
    }

    override fun getItemCount() = posts.size

    fun updateData(newPosts: List<Post>) {
        (this as RecyclerView.Adapter<*>).apply {
            (this@PlacePostAdapter as PlacePostAdapter).posts = newPosts
            notifyDataSetChanged()
        }
    }
} 