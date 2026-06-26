package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Channel
import com.example.data.Comment
import com.example.data.TvRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiscoveredCastDevice(
    val name: String,
    val serviceType: String,
    val ipAddress: String,
    val port: Int
)

class TvViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TvRepository
    
    // Player instance
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(application)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                        _isPlaying.value = isPlayingChanged
                    }
                })
            }
    }

    // System PiP States
    private val _isInSystemPip = MutableStateFlow(false)
    val isInSystemPip: StateFlow<Boolean> = _isInSystemPip.asStateFlow()

    fun setInSystemPip(active: Boolean) {
        _isInSystemPip.value = active
    }

    // Input States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isVoiceSearching = MutableStateFlow(false)
    val isVoiceSearching: StateFlow<Boolean> = _isVoiceSearching.asStateFlow()

    fun setVoiceSearching(active: Boolean) {
        _isVoiceSearching.value = active
    }

    private val _selectedGroup = MutableStateFlow<String?>("All")
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _theaterMode = MutableStateFlow(false)
    val theaterMode: StateFlow<Boolean> = _theaterMode.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(1f) // 0f to 1f
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var preMuteVolume = 1f

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _resizeMode = MutableStateFlow(0) // 0 = Fit, 1 = Zoom/Fill, 2 = Stretch
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _videoQuality = MutableStateFlow("Auto")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    fun setVideoQuality(quality: String) {
        _videoQuality.value = quality
    }

    private val _ambientColor = MutableStateFlow(AndroidColor.DKGRAY)
    val ambientColor: StateFlow<Int> = _ambientColor.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    // Watch History States
    private val _watchHistory = MutableStateFlow<List<Channel>>(emptyList())
    val watchHistory: StateFlow<List<Channel>> = _watchHistory.asStateFlow()

    private val prefs = application.getSharedPreferences("watch_history_prefs", Context.MODE_PRIVATE)

    private fun loadWatchHistory() {
        val savedUrls = prefs.getStringSet("history_urls", emptySet()) ?: emptySet()
        viewModelScope.launch {
            allChannels.collectLatest { channels ->
                if (channels.isNotEmpty()) {
                    val historyList = savedUrls.mapNotNull { url ->
                        channels.find { it.url == url }
                    }.toMutableList()
                    // Fallback to top 4 channels if history is empty, to look nice!
                    if (historyList.isEmpty()) {
                        historyList.addAll(channels.take(4))
                    }
                    _watchHistory.value = historyList
                }
            }
        }
    }

    fun addToWatchHistory(channel: Channel) {
        val currentList = _watchHistory.value.filter { it.url != channel.url }.toMutableList()
        currentList.add(0, channel)
        val cappedList = currentList.take(10)
        _watchHistory.value = cappedList
        
        prefs.edit().putStringSet("history_urls", cappedList.map { it.url }.toSet()).apply()
    }

    fun clearWatchHistory() {
        _watchHistory.value = emptyList()
        prefs.edit().remove("history_urls").apply()
    }

    private val _syncStatus = MutableStateFlow("Local Database Synced")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Casting States
    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castedDevice = MutableStateFlow<String?>(null)
    val castedDevice: StateFlow<String?> = _castedDevice.asStateFlow()

    private val _isSearchingDevices = MutableStateFlow(false)
    val isSearchingDevices: StateFlow<Boolean> = _isSearchingDevices.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private val discoveryListeners = mutableListOf<Pair<String, NsdManager.DiscoveryListener>>()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredCastDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredCastDevice>> = _discoveredDevices.asStateFlow()

    fun startDeviceScan() {
        if (_isSearchingDevices.value) return
        _isSearchingDevices.value = true
        _discoveredDevices.value = emptyList()

        try {
            val wifiManager = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("MRBCastMulticastLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.e("TvViewModel", "Failed to acquire multicast lock: ${e.message}")
        }

        val nsdManager = getApplication<Application>().getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceTypes = listOf("_googlecast._tcp", "_airplay._tcp", "_dlna._tcp")

        discoveryListeners.clear()

        for (serviceType in serviceTypes) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("TvViewModel", "Discovery failed to start for $serviceType: error $errorCode")
                    try {
                        nsdManager.stopServiceDiscovery(this)
                    } catch (e: Exception) {
                        Log.e("TvViewModel", "Error stopping service discovery: ${e.message}")
                    }
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("TvViewModel", "Discovery failed to stop for $serviceType: error $errorCode")
                }

                override fun onDiscoveryStarted(regType: String) {
                    Log.d("TvViewModel", "Service discovery started for $regType")
                }

                override fun onDiscoveryStopped(regType: String) {
                    Log.d("TvViewModel", "Service discovery stopped for $regType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.d("TvViewModel", "Service found: ${serviceInfo.serviceName} type: ${serviceInfo.serviceType}")
                    try {
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e("TvViewModel", "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                            }

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                val host = resolvedInfo.host?.hostAddress ?: ""
                                val rawName = resolvedInfo.serviceName
                                val cleanName = if (rawName.contains("@")) {
                                    rawName.substringAfter("@")
                                } else {
                                    rawName
                                }

                                val device = DiscoveredCastDevice(
                                    name = cleanName,
                                    serviceType = resolvedInfo.serviceType,
                                    ipAddress = host,
                                    port = resolvedInfo.port
                                )

                                val currentList = _discoveredDevices.value
                                if (currentList.none { it.ipAddress == device.ipAddress || it.name == device.name }) {
                                    _discoveredDevices.value = currentList + device
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Log.e("TvViewModel", "Exception resolving service: ${e.message}")
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    Log.d("TvViewModel", "Service lost: ${serviceInfo.serviceName}")
                    val currentList = _discoveredDevices.value
                    _discoveredDevices.value = currentList.filterNot { it.name == serviceInfo.serviceName }
                }
            }

            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                discoveryListeners.add(Pair(serviceType, listener))
            } catch (e: Exception) {
                Log.e("TvViewModel", "Exception starting discovery for $serviceType: ${e.message}")
            }
        }

        // Search for 6 seconds, then stop the loading indicator, keeping device lists
        viewModelScope.launch {
            delay(6000)
            _isSearchingDevices.value = false
        }
    }

    fun stopDeviceScan() {
        val nsdManager = getApplication<Application>().getSystemService(Context.NSD_SERVICE) as NsdManager
        for (pair in discoveryListeners) {
            try {
                nsdManager.stopServiceDiscovery(pair.second)
            } catch (e: Exception) {
                Log.e("TvViewModel", "Exception stopping discovery for ${pair.first}: ${e.message}")
            }
        }
        discoveryListeners.clear()

        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.e("TvViewModel", "Exception releasing multicast lock: ${e.message}")
        }
        _isSearchingDevices.value = false
    }

    fun startCasting(deviceName: String) {
        viewModelScope.launch {
            _isSearchingDevices.value = false
            _isCasting.value = true
            _castedDevice.value = deviceName
            _isPlaying.value = true
            player.pause() // Pause local video to cast on TV
        }
    }

    fun stopCasting() {
        _isCasting.value = false
        _castedDevice.value = null
        player.play() // Resume local video
    }

    private val _appLanguage = MutableStateFlow("English")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _userLocation = MutableStateFlow("Bangladesh (BD)")
    val userLocation: StateFlow<String> = _userLocation.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TvRepository(application, database)

        val country = java.util.Locale.getDefault().country
        val lang = java.util.Locale.getDefault().language
        if (country.equals("BD", ignoreCase = true) || lang.equals("bn", ignoreCase = true)) {
            _appLanguage.value = "Bangla"
            _userLocation.value = "Bangladesh (BD)"
        } else {
            _appLanguage.value = "English"
            val displayCountry = java.util.Locale.getDefault().displayCountry
            _userLocation.value = if (displayCountry.isNotEmpty()) "$displayCountry ($country)" else "Global (US)"
        }

        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
            // Auto refresh from playlist URL on start
            refreshPlaylist()
            loadWatchHistory()
        }

        createNotificationChannel()
    }

    // Flows from Repository
    val allChannels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedChannels: StateFlow<List<Channel>> = repository.downloadedChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered channels based on Search and Selected Group Category
    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        _searchQuery,
        _selectedGroup
    ) { channels, query, group ->
        channels.filter { channel ->
            val matchesQuery = channel.name.contains(query, ignoreCase = true) || 
                               (channel.groupTitle?.contains(query, ignoreCase = true) ?: false)
            val matchesGroup = group == "All" || channel.groupTitle == group
            matchesQuery && matchesGroup
        }.sortedWith { a, b ->
            if (group == "All") {
                val isFifaA = a.groupTitle == "FIFA World Cup 2026"
                val isFifaB = b.groupTitle == "FIFA World Cup 2026"
                when {
                    isFifaA && !isFifaB -> -1
                    !isFifaA && isFifaB -> 1
                    else -> 0
                }
            } else {
                0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique category groups with FIFA World Cup 2026 and major playlists prioritized at the front
    val groups: StateFlow<List<String>> = allChannels.map { channels ->
        val list = channels.mapNotNull { it.groupTitle }.distinct()
        val priorityList = listOf("FIFA World Cup 2026", "Sports", "Entertainment", "Bangla")
        val sortedList = list.sortedWith { a, b ->
            val indexA = priorityList.indexOf(a)
            val indexB = priorityList.indexOf(b)
            when {
                indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                indexA != -1 -> -1
                indexB != -1 -> 1
                else -> a.compareTo(b, ignoreCase = true)
            }
        }
        listOf("All") + sortedList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun refreshPlaylist(url: String = "https://is.gd/yQuS1g.m3u") {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshChannels(url)
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.url, channel.isFavorite)
            // If the currently playing channel is the one favorited, update currentChannel reference
            if (_currentChannel.value?.url == channel.url) {
                _currentChannel.value = channel.copy(isFavorite = !channel.isFavorite)
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _currentChannel.value = channel
        playChannel(channel)
        addToWatchHistory(channel)
    }

    private fun playChannel(channel: Channel) {
        // Stop current playing stream
        player.stop()
        player.clearMediaItems()

        // If channel is downloaded, play the offline file instead of streaming!
        val playUri = if (channel.isDownloaded && channel.localFilePath != null) {
            channel.localFilePath
        } else {
            channel.url
        }

        try {
            // Configure media item with low-latency LiveConfiguration
            val mediaItem = MediaItem.Builder()
                .setUri(playUri)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(1500) // Fast 1.5 second target latency
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            
            // Randomly simulate a color palette for ambient glow based on channel name
            updateAmbientGlowColor(channel.name)
        } catch (e: Exception) {
            Log.e("TvViewModel", "Error starting playback: ${e.message}")
        }
    }

    private var ambientJob: kotlinx.coroutines.Job? = null

    private fun updateAmbientGlowColor(name: String) {
        ambientJob?.cancel()
        val hash = name.hashCode()
        val baseColors = when {
            name.contains("sport", ignoreCase = true) || name.contains("fifa", ignoreCase = true) -> {
                listOf(
                    AndroidColor.rgb(0, 230, 118),   // Green field
                    AndroidColor.rgb(0, 200, 83),    // Emerald field
                    AndroidColor.rgb(41, 121, 255),  // Sky blue
                    AndroidColor.rgb(224, 64, 251)   // Bright magenta
                )
            }
            else -> {
                listOf(
                    AndroidColor.rgb(180, 50, 50),   // Red glow
                    AndroidColor.rgb(50, 150, 200),  // Teal glow
                    AndroidColor.rgb(120, 50, 180),  // Purple glow
                    AndroidColor.rgb(50, 180, 100),  // Green glow
                    AndroidColor.rgb(200, 150, 50),  // Gold glow
                    AndroidColor.rgb(220, 100, 50)   // Coral glow
                )
            }
        }
        val primaryColor = baseColors[kotlin.math.abs(hash) % baseColors.size]
        _ambientColor.value = primaryColor

        // Periodically modulate colors to simulate live frame changes
        ambientJob = viewModelScope.launch {
            while (true) {
                delay(2500)
                if (_isPlaying.value) {
                    val r = AndroidColor.red(primaryColor)
                    val g = AndroidColor.green(primaryColor)
                    val b = AndroidColor.blue(primaryColor)
                    // Modulate RGB slightly to simulate current video frame colors
                    val nr = (r + (-35..35).random()).coerceIn(20, 255)
                    val ng = (g + (-35..35).random()).coerceIn(20, 255)
                    val nb = (b + (-35..35).random()).coerceIn(20, 255)
                    _ambientColor.value = AndroidColor.rgb(nr, ng, nb)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stopPlayback() {
        player.stop()
        player.clearMediaItems()
        _currentChannel.value = null
        _isPlaying.value = false
    }

    fun playNext() {
        val channels = filteredChannels.value
        val current = _currentChannel.value ?: return
        val currentIndex = channels.indexOfFirst { it.url == current.url }
        if (currentIndex != -1 && currentIndex < channels.size - 1) {
            selectChannel(channels[currentIndex + 1])
        } else if (channels.isNotEmpty()) {
            selectChannel(channels[0])
        }
    }

    fun playPrevious() {
        val channels = filteredChannels.value
        val current = _currentChannel.value ?: return
        val currentIndex = channels.indexOfFirst { it.url == current.url }
        if (currentIndex > 0) {
            selectChannel(channels[currentIndex - 1])
        } else if (channels.isNotEmpty()) {
            selectChannel(channels.last())
        }
    }

    fun setVolume(vol: Float) {
        _volume.value = vol
        player.volume = vol
        _isMuted.value = vol <= 0f
    }

    fun toggleMute() {
        if (_isMuted.value) {
            _isMuted.value = false
            setVolume(preMuteVolume.coerceAtLeast(0.1f))
        } else {
            preMuteVolume = _volume.value
            _isMuted.value = true
            setVolume(0f)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
    }

    fun toggleResizeMode() {
        _resizeMode.value = (_resizeMode.value + 1) % 3
    }

    fun seekForward() {
        val newPos = player.currentPosition + 10000
        val duration = player.duration
        player.seekTo(if (duration > 0) kotlin.math.min(newPos, duration) else newPos)
    }

    fun seekBackward() {
        val newPos = player.currentPosition - 10000
        player.seekTo(kotlin.math.max(newPos, 0L))
    }

    fun toggleTheaterMode() {
        _theaterMode.value = !_theaterMode.value
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun getLocalizedString(key: String): String {
        val language = _appLanguage.value
        val en = mapOf(
            "app_title" to "MRB-TV Live Stream",
            "search_placeholder" to "Search TV channels...",
            "select_channel" to "Select a channel to start streaming",
            "home" to "Home",
            "lounge" to "Lounge Feed",
            "sync" to "Cloud Sync",
            "settings" to "Settings",
            "streams" to "Streams",
            "wc2026" to "WC 2026",
            "world_cup" to "World Cup",
            "dev_id" to "Developer ID:",
            "theme" to "Theme Mode",
            "dark_mode" to "Dark Mode",
            "light_mode" to "Light Mode",
            "language" to "App Language",
            "auto_loc" to "Auto-detected by location",
            "location" to "User Location",
            "no_channels" to "No channels found for this category.",
            "refresh_playlist" to "Sync Playlist",
            "favorites" to "Favorites Only",
            "personal_sync" to "Personalized Cloud Sync",
            "sync_now" to "Sync All Data Now",
            "offline_downloads" to "Offline Downloads",
            "all" to "All",
            "mrb_social" to "MRB Social Feed",
            "chat_with_viewers" to "Chat with viewers, read reviews & replies",
            "post_button" to "Post",
            "say_something" to "Say something about this stream...",
            "send_button" to "Send",
            "ambient_mode" to "Ambient Glow Effect",
            "aspect_ratio" to "Aspect Ratio Mode",
            "playback_speed" to "Playback Speed",
            "quality" to "Video Quality",
            "cast_to_device" to "Cast to Screen",
            "searching_cast" to "Searching for Cast devices...",
            "casting_to" to "Casting to",
            "stop_casting" to "Disconnect Cast"
        )
        val bn = mapOf(
            "app_title" to "এমআরবি-টিভি লাইভ",
            "search_placeholder" to "টিভি চ্যানেল খুঁজুন...",
            "select_channel" to "স্ট্রিমিং শুরু করতে একটি চ্যানেল নির্বাচন করুন",
            "home" to "হোম",
            "lounge" to "আড্ডা ফিড",
            "sync" to "ক্লাউড সিঙ্ক",
            "settings" to "সেটিংস",
            "streams" to "চ্যানেলসমূহ",
            "wc2026" to "বিশ্বকাপ ২৬",
            "world_cup" to "বিশ্বকাপ",
            "dev_id" to "ডেভেলপার আইডি:",
            "theme" to "থিম মোড",
            "dark_mode" to "ডার্ক মোড",
            "light_mode" to "লাইট মোড",
            "language" to "অ্যাপের ভাষা",
            "auto_loc" to "অবস্থান অনুযায়ী স্বয়ংক্রিয়",
            "location" to "ব্যবহারকারীর অবস্থান",
            "no_channels" to "এই বিভাগে কোনো চ্যানেল পাওয়া যায়নি।",
            "refresh_playlist" to "প্লেলিস্ট সিঙ্ক করুন",
            "favorites" to "শুধু প্রিয়গুলো",
            "personal_sync" to "ব্যক্তিগত ক্লাউড সিঙ্ক",
            "sync_now" to "সব তথ্য এখনই সিঙ্ক করুন",
            "offline_downloads" to "অফলাইন ডাউনলোড সমূহ",
            "all" to "সব",
            "mrb_social" to "এমআরবি সোশ্যাল ফিড",
            "chat_with_viewers" to "দর্শকদের সাথে চ্যাট করুন, মতামত ও উত্তর দিন",
            "post_button" to "পোস্ট করুন",
            "say_something" to "এই স্ট্রিমটি নিয়ে কিছু বলুন...",
            "send_button" to "পাঠান",
            "ambient_mode" to "অ্যাম্বিয়েন্ট গ্লো ইফেক্ট",
            "aspect_ratio" to "অ্যাসপেক্ট রেশিও মোড",
            "playback_speed" to "প্লেব্যাক স্পিড",
            "quality" to "ভিডিও কোয়ালিটি",
            "cast_to_device" to "স্ক্রিনে কাস্ট করুন",
            "searching_cast" to "কাস্ট ডিভাইস খোঁজা হচ্ছে...",
            "casting_to" to "কাস্ট হচ্ছে",
            "stop_casting" to "কাস্ট বন্ধ করুন"
        )
        return if (language == "Bangla") bn[key] ?: en[key] ?: key else en[key] ?: key
    }

    fun startDownload(channel: Channel) {
        viewModelScope.launch {
            repository.startDownload(channel)
        }
    }

    fun deleteDownload(channel: Channel) {
        viewModelScope.launch {
            repository.deleteDownload(channel)
            // Refresh current playing if it was downloaded
            if (_currentChannel.value?.url == channel.url) {
                _currentChannel.value = channel.copy(isDownloaded = false, localFilePath = null)
            }
        }
    }

    fun addPost(username: String, content: String, channelName: String? = null) {
        viewModelScope.launch {
            repository.addPost(username, content, channelName)
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.likePost(postId)
        }
    }

    fun getComments(postId: Int): Flow<List<Comment>> = repository.getCommentsForPost(postId)

    fun addComment(postId: Int, username: String, content: String) {
        viewModelScope.launch {
            repository.addComment(postId, username, content)
            
            // Simulating reply and notification
            delay(4000)
            triggerReplyNotification(postId, username, content)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Synchronizing favorites and custom playlists with Cloud..."
            delay(2000)
            _syncStatus.value = "Cloud Sync successful. All playlists secured!"
            _isSyncing.value = false
        }
    }

    private fun triggerReplyNotification(postId: Int, commenter: String, originalComment: String) {
        val context = getApplication<Application>().applicationContext
        
        // Simulating a reply from other user
        val replies = listOf(
            "Wow! I absolutely agree with that.",
            "Thanks for the comment, I will check out this channel too!",
            "I noticed that too, stream resolution is really solid.",
            "Exactly, theater mode with that background glow makes a huge difference."
        )
        val replyContent = replies.random()
        val replier = listOf("IPTV_Guru", "AuraStreams", "StreamMaster", "Ch1_Watcher").random()

        viewModelScope.launch {
            // Save the reply as a comment on the same post
            repository.addComment(postId, replier, replyContent)

            // Trigger actual Android System Notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, "mrb_replies_channel")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("New reply from @$replier on MRB-TV")
                .setContentText(replyContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(postId + 1000, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>().applicationContext
            val name = "MRB-TV Social Replies"
            val descriptionText = "Notifications when someone replies to your channel comments"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("mrb_replies_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDeviceScan()
        player.release()
    }
}
