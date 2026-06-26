package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class StreamDownloader(private val context: Context, private val channelDao: ChannelDao) {
    private val client = OkHttpClient()
    private val TAG = "StreamDownloader"

    suspend fun downloadChannel(channel: Channel) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download for: ${channel.name} from ${channel.url}")
            
            // Set initial state
            channelDao.setDownloadStatus(channel.url, isDownloaded = false, localFilePath = null, progress = 5f)

            val request = Request.Builder().url(channel.url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Download failed with response code: ${response.code}")
                    channelDao.setDownloadStatus(channel.url, isDownloaded = false, localFilePath = null, progress = 0f)
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    Log.e(TAG, "Empty response body")
                    channelDao.setDownloadStatus(channel.url, isDownloaded = false, localFilePath = null, progress = 0f)
                    return@withContext
                }

                // Prepare file in app internal storage
                val safeFileName = "channel_${channel.name.filter { it.isLetterOrDigit() }}_${System.currentTimeMillis()}.mp4"
                val file = File(context.filesDir, safeFileName)
                
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(file)
                
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                val contentLength = body.contentLength()
                
                // For live streams, contentLength might be -1. We will set a max limit of 15MB to prevent infinite disk filling.
                val maxLimit = if (contentLength <= 0) 15 * 1024 * 1024 else contentLength

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Update progress
                    val progress = if (contentLength > 0) {
                        (totalBytesRead.toFloat() / contentLength.toFloat()) * 100f
                    } else {
                        // For infinite live streams, simulate up to maxLimit
                        (totalBytesRead.toFloat() / maxLimit.toFloat()) * 100f
                    }
                    val boundedProgress = progress.coerceIn(5f, 95f)
                    channelDao.updateDownloadProgress(channel.url, boundedProgress)

                    // Cap downloads at 15MB for live streams
                    if (contentLength <= 0 && totalBytesRead >= maxLimit) {
                        Log.d(TAG, "Reached maximum limit for live stream download, finishing.")
                        break
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d(TAG, "Download complete: ${file.absolutePath}")
                
                // Save complete status in database
                channelDao.setDownloadStatus(
                    url = channel.url,
                    isDownloaded = true,
                    localFilePath = file.absolutePath,
                    progress = 100f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading stream: ${e.message}", e)
            channelDao.setDownloadStatus(channel.url, isDownloaded = false, localFilePath = null, progress = 0f)
        }
    }

    fun deleteDownload(channel: Channel) {
        channel.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted offline file: $path")
            }
        }
    }
}
