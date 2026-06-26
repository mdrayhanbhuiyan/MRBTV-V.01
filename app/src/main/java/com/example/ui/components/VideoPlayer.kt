package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.ui.TvViewModel
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()
    val ambientColorInt by viewModel.ambientColor.collectAsState()
    val theaterMode by viewModel.theaterMode.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()

    val isCasting by viewModel.isCasting.collectAsState()
    val castedDevice by viewModel.castedDevice.collectAsState()
    val isSearchingDevices by viewModel.isSearchingDevices.collectAsState()
    var showCastDialog by remember { mutableStateOf(false) }

    val animatedGlowColor by animateColorAsState(
        targetValue = Color(ambientColorInt),
        animationSpec = tween(durationMillis = 1000),
        label = "ambientGlow"
    )

    if (currentChannel == null) {
        // Empty State visual overlay
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.getLocalizedString("select_channel"),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        var showControls by remember { mutableStateOf(true) }
        var showQualityMenu by remember { mutableStateOf(false) }
        var isLocked by remember { mutableStateOf(false) }
        var elapsedSeconds by remember { mutableStateOf(0) }

        val activity = context as? Activity
        LaunchedEffect(isFullScreen) {
            activity?.let { act ->
                val window = act.window
                val view = window.decorView
                val controller = WindowCompat.getInsetsController(window, view)
                if (isFullScreen) {
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                activity?.let { act ->
                    val window = act.window
                    val view = window.decorView
                    val controller = WindowCompat.getInsetsController(window, view)
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        LaunchedEffect(isPlaying, currentChannel) {
            if (isPlaying) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    elapsedSeconds++
                }
            } else {
                elapsedSeconds = 0
            }
        }

        val formattedTime = remember(elapsedSeconds) {
            val h = elapsedSeconds / 3600
            val m = (elapsedSeconds % 3600) / 60
            val s = elapsedSeconds % 60
            if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }

        // Trigger hidden controls after delay
        LaunchedEffect(showControls, isPlaying, isLocked) {
            if (showControls && isPlaying && !isLocked) {
                kotlinx.coroutines.delay(5000)
                showControls = false
            }
        }

        // Parent container with Ambient Light Glow Effect
        Box(
            modifier = if (isFullScreen) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (theaterMode) Modifier.height(380.dp) else Modifier.height(250.dp)
                    )
                    .background(Color.Black)
            },
            contentAlignment = Alignment.Center
        ) {
            // Ambient Glow Layer behind player (Dynamic Real-time Ambilight CSS simulation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedGlowColor.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Actual Video View Layer with Glassmorphic border
            Box(
                modifier = if (isFullScreen) {
                    Modifier.fillMaxSize()
                } else {
                    modifier
                        .fillMaxWidth(if (theaterMode) 1f else 0.96f)
                        .fillMaxHeight(0.94f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable { showControls = !showControls }
                }
            ) {
                if (isCasting) {
                    CastingActiveOverlay(
                        viewModel = viewModel,
                        castedDevice = castedDevice,
                        onStopCasting = { viewModel.stopCasting() }
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = viewModel.player
                                useController = false // Use custom beautiful overlay controls
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.resizeMode = when (resizeMode) {
                                1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Fluid Glassmorphic Control Overlay
                AnimatedVisibility(
                    visible = showControls,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Beautiful semi-transparent glass background
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(12.dp)
                    ) {
                        if (isLocked) {
                            // Locked View: display ONLY a glowing lock icon to unlock
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { isLocked = false },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                        .border(1.2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Unlock Stream Controls",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            // Top info: Channel name and Details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentChannel?.name ?: "Streaming Channel",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                Text(
                                    text = currentChannel?.groupTitle ?: "Live IPTV",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }

                            // Favorite toggle inside Player
                            IconButton(
                                onClick = { viewModel.toggleFavorite(currentChannel!!) },
                                modifier = Modifier.testTag("player_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (currentChannel?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite Toggle",
                                    tint = if (currentChannel?.isFavorite == true) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Cast options toggle inside Player
                            IconButton(
                                onClick = { showCastDialog = true },
                                modifier = Modifier.testTag("player_cast_button")
                            ) {
                                Icon(
                                    imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                                    contentDescription = "Cast Screen",
                                    tint = if (isCasting) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Picture-in-Picture mode toggle
                            IconButton(
                                onClick = {
                                    val activity = context as? com.example.MainActivity
                                    activity?.enterPipMode()
                                },
                                modifier = Modifier.testTag("player_pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Screen lock toggle
                            IconButton(
                                onClick = { isLocked = true },
                                modifier = Modifier.testTag("player_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Lock controls",
                                    tint = Color.White
                                )
                            }
                        }

                        // Center controllers: Play, Pause, Prev, Next, Seek Forward/Backward (with glassmorphic touch targets)
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Previous Channel
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("player_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Seek Backward 10s
                            IconButton(
                                onClick = { viewModel.seekBackward() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Seek Backward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Play/Pause Toggle
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(100)
                                    )
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Seek Forward 10s
                            IconButton(
                                onClick = { viewModel.seekForward() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Seek Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next Channel
                            IconButton(
                                onClick = { viewModel.playNext() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("player_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Bottom controllers: Volume slider, quality menu, aspect-ratio, speed, fullscreen, theater mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Mute / Unmute Button
                            IconButton(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted || volume <= 0f) {
                                        Icons.Default.VolumeOff
                                    } else if (volume > 0.5f) {
                                        Icons.Default.VolumeUp
                                    } else {
                                        Icons.Default.VolumeDown
                                    },
                                    contentDescription = "Toggle Mute",
                                    tint = if (isMuted) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Volume slider - compact for mobile
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(it) },
                                modifier = Modifier
                                    .width(70.dp)
                                    .testTag("volume_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (isMuted) "Muted" else "${(volume * 100).toInt()}%",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Live Session Stopwatch Counter Badge
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color(0xFF00E676), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE $formattedTime",
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Quality Option Button & Dropdown Menu
                            Box(modifier = Modifier.padding(horizontal = 2.dp)) {
                                TextButton(
                                    onClick = { showQualityMenu = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Quality Control",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = videoQuality,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    listOf("Auto", "1080p Live", "720p Live", "480p Live", "360p").forEach { quality ->
                                        DropdownMenuItem(
                                            text = { Text(quality, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                viewModel.setVideoQuality(quality)
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Speed indicator badge & cycle button
                            TextButton(
                                onClick = {
                                    val nextSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    viewModel.setPlaybackSpeed(nextSpeed)
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed Control",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Aspect Ratio Cycle Button
                            IconButton(
                                onClick = { viewModel.toggleResizeMode() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = if (resizeMode != 0) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Live aspect mode text indicator on click
                            Text(
                                text = when (resizeMode) {
                                    1 -> "Zoom"
                                    2 -> "Stretch"
                                    else -> "Fit"
                                },
                                color = Color.LightGray,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            // True Full-screen Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleFullScreen() },
                                modifier = Modifier.size(32.dp).testTag("fullscreen_mode_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen Mode Toggle",
                                    tint = if (isFullScreen) MaterialTheme.colorScheme.secondary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Only show theater mode when not full screen
                            if (!isFullScreen) {
                                IconButton(
                                    onClick = { viewModel.toggleTheaterMode() },
                                    modifier = Modifier.size(32.dp).testTag("theater_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (theaterMode) Icons.Default.Slideshow else Icons.Default.Tv,
                                        contentDescription = "Theater Mode Toggle",
                                        tint = if (theaterMode) MaterialTheme.colorScheme.secondary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        // Beautiful luxury casting dialogue
        if (showCastDialog) {
            AlertDialog(
                onDismissRequest = { showCastDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.getLocalizedString("cast_to_device"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSearchingDevices) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = viewModel.getLocalizedString("searching_cast"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Text(
                                text = "Select a wireless receiver to direct stream to your TV / AirPlay system:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Target Chromecast
                            CastDeviceItemRow(
                                name = "Living Room Chromecast Ultra",
                                icon = Icons.Default.Tv,
                                isConnected = isCasting && castedDevice == "Living Room Chromecast Ultra",
                                onClick = {
                                    viewModel.startCasting("Living Room Chromecast Ultra")
                                    showCastDialog = false
                                }
                            )

                            // Target Apple TV (AirPlay)
                            CastDeviceItemRow(
                                name = "Master Bedroom Apple TV",
                                icon = Icons.Default.CastConnected,
                                isConnected = isCasting && castedDevice == "Master Bedroom Apple TV",
                                onClick = {
                                    viewModel.startCasting("Master Bedroom Apple TV")
                                    showCastDialog = false
                                }
                            )

                            // Target Smart TV (AirCast)
                            CastDeviceItemRow(
                                name = "Samsung 4K QLED Smart TV",
                                icon = Icons.Default.Tv,
                                isConnected = isCasting && castedDevice == "Samsung 4K QLED Smart TV",
                                onClick = {
                                    viewModel.startCasting("Samsung 4K QLED Smart TV")
                                    showCastDialog = false
                                }
                            )

                            // Target Google Nest Hub
                            CastDeviceItemRow(
                                name = "Kitchen Google Nest Hub Max",
                                icon = Icons.Default.Router,
                                isConnected = isCasting && castedDevice == "Kitchen Google Nest Hub Max",
                                onClick = {
                                    viewModel.startCasting("Kitchen Google Nest Hub Max")
                                    showCastDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isCasting) {
                            TextButton(
                                onClick = {
                                    viewModel.stopCasting()
                                    showCastDialog = false
                                }
                            ) {
                                Text(
                                    text = viewModel.getLocalizedString("stop_casting"),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = { showCastDialog = false }) {
                            Text("Close", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
            )
        }
    }
}

@Composable
fun CastDeviceItemRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (isConnected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isConnected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CastingActiveOverlay(
    viewModel: TvViewModel,
    castedDevice: String?,
    onStopCasting: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1524), // Luxury deep royal sports navy
                        Color(0xFF070B14)  // Luxury deep dark navy
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-fidelity radar/ripple pulse visual effect
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        // Pulsing radar glow ring
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer(
                    scaleX = pulseScale,
                    scaleY = pulseScale
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                    shape = RoundedCornerShape(100)
                )
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .padding(20.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CastConnected,
                    contentDescription = "Casting Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = viewModel.getLocalizedString("casting_to"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray
                )
                Text(
                    text = castedDevice ?: "Smart TV Screen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStopCasting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(viewModel.getLocalizedString("stop_casting"))
                }
            }
        }
    }
}
