package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

object M3uParser {
    private const val TAG = "M3uParser"
    private val client = OkHttpClient()

    suspend fun fetchAndParse(url: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download M3U: ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parse(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/parsing M3U: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val reader = BufferedReader(StringReader(m3uContent))
            var line = reader.readLine()
            
            if (line == null || !line.trim().startsWith("#EXTM3U")) {
                Log.e(TAG, "Not a valid M3U file (missing #EXTM3U header)")
                // We'll still try to parse in case of minor formats
            }

            var currentName = ""
            var currentLogo: String? = null
            var currentGroup: String? = null

            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:")) {
                    // Extract tvg-logo
                    currentLogo = extractAttribute(trimmed, "tvg-logo") ?: extractAttribute(trimmed, "logo")
                    
                    // Extract group-title
                    currentGroup = extractAttribute(trimmed, "group-title") ?: extractAttribute(trimmed, "group")
                    
                    // Extract channel name from the comma or tvg-name
                    val commaIndex = trimmed.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        extractAttribute(trimmed, "tvg-name") ?: "Unknown Channel"
                    }
                    if (currentName.isEmpty()) {
                        currentName = extractAttribute(trimmed, "tvg-name") ?: "Unknown Channel"
                    }
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    if (currentName.isEmpty()) {
                        // Use last segment of URL as fallback name
                        val lastSegment = trimmed.substringAfterLast('/')
                        currentName = lastSegment.substringBefore('?').ifEmpty { "Channel ${channels.size + 1}" }
                    }
                    channels.add(
                        Channel(
                            url = trimmed,
                            name = currentName,
                            logoUrl = currentLogo?.ifEmpty { null },
                            groupTitle = currentGroup?.ifEmpty { "Uncategorized" }
                        )
                    )
                    // Reset variables for next channel
                    currentName = ""
                    currentLogo = null
                    currentGroup = null
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U: ${e.message}", e)
        }
        return channels
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        return match?.groups?.get(1)?.value
    }
}
