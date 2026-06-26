package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TvRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val channelDao = database.channelDao()
    private val socialDao = database.socialDao()
    private val downloader = StreamDownloader(context, channelDao)

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels()
    val downloadedChannels: Flow<List<Channel>> = channelDao.getDownloadedChannels()
    val allPosts: Flow<List<SocialPost>> = socialDao.getAllPosts()

    fun getCommentsForPost(postId: Int): Flow<List<Comment>> = socialDao.getCommentsForPost(postId)

    suspend fun refreshChannels(m3uUrl: String) {
        try {
            Log.d("TvRepository", "Refreshing channels from $m3uUrl")
            val parsed = M3uParser.fetchAndParse(m3uUrl)
            if (parsed.isNotEmpty()) {
                channelDao.insertChannels(parsed)
                Log.d("TvRepository", "Successfully stored ${parsed.size} channels in DB.")
            } else {
                Log.w("TvRepository", "Parsed empty list of channels or fetch failed")
            }
        } catch (e: Exception) {
            Log.e("TvRepository", "Failed to refresh channels: ${e.message}", e)
        }
    }

    suspend fun toggleFavorite(url: String, currentFavorite: Boolean) {
        channelDao.setFavorite(url, !currentFavorite)
    }

    suspend fun startDownload(channel: Channel) {
        downloader.downloadChannel(channel)
    }

    suspend fun deleteDownload(channel: Channel) {
        downloader.deleteDownload(channel)
        channelDao.setDownloadStatus(
            url = channel.url,
            isDownloaded = false,
            localFilePath = null,
            progress = 0f
        )
    }

    suspend fun addPost(username: String, content: String, channelName: String? = null) {
        socialDao.insertPost(
            SocialPost(
                username = username,
                content = content,
                channelName = channelName
            )
        )
    }

    suspend fun likePost(postId: Int) {
        socialDao.likePost(postId)
    }

    suspend fun addComment(postId: Int, username: String, content: String) {
        socialDao.insertComment(
            Comment(
                postId = postId,
                username = username,
                content = content
            )
        )
    }

    suspend fun initializeDefaultDataIfEmpty() {
        val count = channelDao.getChannelCount()
        if (count == 0) {
            // Seed beautiful channels representing requested categories
            val defaultChannels = listOf(
                // FIFA World Cup 2026
                Channel(
                    url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    name = "FIFA WC 2026: USA vs Bangladesh Live Match",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/FIFA_World_Cup_2026_Logo.svg",
                    groupTitle = "FIFA World Cup 2026"
                ),
                Channel(
                    url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8",
                    name = "FIFA WC 2026: Opening Ceremony Preview",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/FIFA_World_Cup_2026_Logo.svg",
                    groupTitle = "FIFA World Cup 2026"
                ),
                Channel(
                    url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    name = "FIFA WC 2026: Official Highlights & Stats",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/FIFA_World_Cup_2026_Logo.svg",
                    groupTitle = "FIFA World Cup 2026"
                ),
                // Sports
                Channel(
                    url = "https://fcc3da4b86b7.us-east-1.playback.live-video.net/api/video/v1/us-east-1.990728248424.channel.bhkxUv1sW97f.m3u8",
                    name = "GTV Sports Live HD",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/82/GTV_Logo_2021.png",
                    groupTitle = "Sports"
                ),
                Channel(
                    url = "https://res.cloudinary.com/dannykeane/video/upload/sp_full_hd/q_80/v1498683526/sample_hg89vv.m3u8",
                    name = "T-Sports Live Bangladesh",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/en/2/23/T_Sports_logo.png",
                    groupTitle = "Sports"
                ),
                // Bangla
                Channel(
                    url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    name = "Somoy TV News Live",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6d/Somoy_TV_Official_Logo.jpg",
                    groupTitle = "Bangla"
                ),
                Channel(
                    url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8",
                    name = "Channel i Bangla Live",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/77/Channel_i_Logo.png",
                    groupTitle = "Bangla"
                ),
                Channel(
                    url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    name = "BTV World HD",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5e/BTV_Logo_Official.png",
                    groupTitle = "Bangla"
                ),
                // Entertainment
                Channel(
                    url = "https://fcc3da4b86b7.us-east-1.playback.live-video.net/api/video/v1/us-east-1.990728248424.channel.bhkxUv1sW97f.m3u8",
                    name = "Cinema Premium Lounge",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c5/HBO_Logo.svg",
                    groupTitle = "Entertainment"
                ),
                Channel(
                    url = "https://res.cloudinary.com/dannykeane/video/upload/sp_full_hd/q_80/v1498683526/sample_hg89vv.m3u8",
                    name = "Discovery Life HD",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/1/15/Discovery_Channel_logo.svg",
                    groupTitle = "Entertainment"
                )
            )
            channelDao.insertChannels(defaultChannels)

            // Seed default community posts
            socialDao.insertPost(
                SocialPost(
                    username = "mrb_admin",
                    content = "Welcome to MRB-TV! Support Bangladesh in the FIFA World Cup 2026. Comment your match predictions in our Lounge Feed! ⚽🇧🇩",
                    channelName = "General Lounge"
                )
            )
            socialDao.insertPost(
                SocialPost(
                    username = "sakib_99",
                    content = "T-Sports streaming quality is awesome! Waiting for Bangladesh vs USA WC match!! Let's go!",
                    channelName = "FIFA WC 2026: USA vs Bangladesh Live Match"
                )
            )
        }
    }
}
