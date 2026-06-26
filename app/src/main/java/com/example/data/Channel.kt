package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val url: String, // Stream URL is naturally unique
    val name: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val downloadProgress: Float = 0f // 0 to 100
)
