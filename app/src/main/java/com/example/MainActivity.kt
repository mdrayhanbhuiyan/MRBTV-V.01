package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TvViewModel
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.StreamsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import android.app.PictureInPictureParams
import android.util.Rational
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Live alerts and stream updates notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val tvViewModel = ViewModelProvider(this)[TvViewModel::class.java]
        if (tvViewModel.isPlaying.value && tvViewModel.currentChannel.value != null) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val tvViewModel = ViewModelProvider(this)[TvViewModel::class.java]
        tvViewModel.setInSystemPip(isInPictureInPictureMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkAndRequestNotificationPermission()

        setContent {
            val tvViewModel: TvViewModel = viewModel()
            val isDarkMode by tvViewModel.isDarkMode.collectAsState()
            val isInSystemPip by tvViewModel.isInSystemPip.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                if (isInSystemPip) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = tvViewModel.player
                                    useController = false
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                // Keep updated
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    var currentTab by remember { mutableStateOf("Home") }
                    val isFullScreen by tvViewModel.isFullScreen.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            topBar = {
                                // Completely hide Top Bar in fullscreen mode or on Home and Live screens
                                if (!isFullScreen && currentTab != "Home" && currentTab != "Live") {
                                    CenterAlignedTopAppBar(
                                        title = {
                                            // Menubar middle: World Cup 2026 icon button to filter channels directly
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable {
                                                        currentTab = "Streams"
                                                        tvViewModel.selectGroup("FIFA World Cup 2026")
                                                        tvViewModel.updateSearchQuery("")
                                                    }
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    .testTag("menubar_worldcup_direct_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EmojiEvents,
                                                    contentDescription = "World Cup 2026",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tvViewModel.getLocalizedString("wc2026"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        navigationIcon = {
                                            Text(
                                                text = "MRB-TV",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = { currentTab = "Settings" },
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Quick Settings",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }
                            },
                            bottomBar = {
                                // Completely hide bottom navigation in fullscreen mode for immersive video
                                if (!isFullScreen) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Transparent)
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .testTag("app_navigation_bar")
                                    ) {
                                        // Floating Glass Capsule with double-layer background
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(68.dp)
                                                .clip(RoundedCornerShape(34.dp))
                                                .background(Color(0xE5070B12)) // Deep obsidian-glass translucency
                                                .border(
                                                    width = 1.2.dp,
                                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.12f),
                                                            Color.White.copy(alpha = 0.02f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(34.dp)
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. Home
                                                val isHome = currentTab == "Home"
                                                val homeScale by animateFloatAsState(
                                                    targetValue = if (isHome) 1.12f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                                    label = "homeScale"
                                                )
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentTab = "Home" }
                                                        .graphicsLayer(scaleX = homeScale, scaleY = homeScale)
                                                        .testTag("nav_home_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isHome) Icons.Filled.Home else Icons.Default.Home,
                                                        contentDescription = "Home",
                                                        tint = if (isHome) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "HOME",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        color = if (isHome) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f)
                                                    )
                                                }

                                                // 2. Streams (Live TV)
                                                val isStreams = currentTab == "Streams"
                                                val streamsScale by animateFloatAsState(
                                                    targetValue = if (isStreams) 1.12f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                                    label = "streamsScale"
                                                )
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentTab = "Streams" }
                                                        .graphicsLayer(scaleX = streamsScale, scaleY = streamsScale)
                                                        .testTag("nav_streams_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isStreams) Icons.Filled.LiveTv else Icons.Default.LiveTv,
                                                        contentDescription = "Streams",
                                                        tint = if (isStreams) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "LIVE TV",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        color = if (isStreams) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f)
                                                    )
                                                }

                                                // 3. Center big floating green play button
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .offset(y = (-14).dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(Color(0xFF00FF87), Color(0xFF00E676))
                                                            )
                                                        )
                                                        .border(3.dp, Color(0xFF040608), CircleShape)
                                                        .clickable {
                                                            currentTab = "Streams"
                                                            if (tvViewModel.allChannels.value.isNotEmpty()) {
                                                                tvViewModel.selectChannel(tvViewModel.allChannels.value.first())
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Main Play",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }

                                                // 4. Favorites
                                                val isFavorites = currentTab == "Favorites"
                                                val favoritesScale by animateFloatAsState(
                                                    targetValue = if (isFavorites) 1.12f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                                    label = "favoritesScale"
                                                )
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentTab = "Favorites" }
                                                        .graphicsLayer(scaleX = favoritesScale, scaleY = favoritesScale)
                                                        .testTag("nav_favorites_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFavorites) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Favorites",
                                                        tint = if (isFavorites) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "FAVORITES",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        color = if (isFavorites) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f)
                                                    )
                                                }

                                                // 5. Settings / Profile
                                                val isSettings = currentTab == "Settings"
                                                val settingsScale by animateFloatAsState(
                                                    targetValue = if (isSettings) 1.12f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                                    label = "settingsScale"
                                                )
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentTab = "Settings" }
                                                        .graphicsLayer(scaleX = settingsScale, scaleY = settingsScale)
                                                        .testTag("nav_settings_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSettings) Icons.Filled.Person else Icons.Default.Person,
                                                        contentDescription = "Profile",
                                                        tint = if (isSettings) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "PROFILE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        color = if (isSettings) Color(0xFF00E676) else Color.White.copy(alpha = 0.45f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = if (isFullScreen) 0.dp else innerPadding.calculateTopPadding(),
                                        bottom = if (isFullScreen) 0.dp else innerPadding.calculateBottomPadding()
                                    )
                            ) {
                                when (currentTab) {
                                    "Home" -> HomeScreen(
                                        viewModel = tvViewModel,
                                        onNavigateToTab = { tab -> currentTab = tab }
                                    )
                                    "Streams" -> StreamsScreen(viewModel = tvViewModel)
                                    "Favorites" -> FavoritesScreen(viewModel = tvViewModel)
                                    "Settings" -> SettingsScreen(viewModel = tvViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun MiniPlayerFloatingBar(
    channelName: String,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .testTag("floating_mini_player")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Now Playing (Background)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colorScheme.primary)
                    .size(36.dp)
                    .testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Stream",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
