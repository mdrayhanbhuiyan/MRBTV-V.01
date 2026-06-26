package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "social_posts")
data class SocialPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val channelName: String? = null
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val username: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
