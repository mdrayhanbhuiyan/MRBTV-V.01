package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isDownloaded = 1 ORDER BY name ASC")
    fun getDownloadedChannels(): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun setFavorite(url: String, isFavorite: Boolean)

    @Query("UPDATE channels SET isDownloaded = :isDownloaded, localFilePath = :localFilePath, downloadProgress = :progress WHERE url = :url")
    suspend fun setDownloadStatus(url: String, isDownloaded: Boolean, localFilePath: String?, progress: Float)

    @Query("UPDATE channels SET downloadProgress = :progress WHERE url = :url")
    suspend fun updateDownloadProgress(url: String, progress: Float)

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int
}

@Dao
interface SocialDao {
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPost)

    @Query("UPDATE social_posts SET likes = likes + 1 WHERE id = :postId")
    suspend fun likePost(postId: Int)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Int): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}

@Database(entities = [Channel::class, SocialPost::class, Comment::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun socialDao(): SocialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mrb_tv_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
