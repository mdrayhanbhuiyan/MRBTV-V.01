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
                                    Column {
                                        // Custom Glassmorphic Premium Bottom Navigation bar
                                        Surface(
                                            color = Color(0xEB060A13), // Deep slate translucent dark base
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = 1.dp,
                                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                                                    ),
                                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                                )
                                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .testTag("app_navigation_bar")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceAround,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. Home Tab
                                                val isHome = currentTab == "Home"
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clickable { currentTab = "Home" }
                                                        .testTag("nav_home_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Home,
                                                        contentDescription = "Home",
                                                        tint = if (isHome) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Home",
                                                        color = if (isHome) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                // 2. Streams Tab
                                                val isStreams = currentTab == "Streams"
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clickable { currentTab = "Streams" }
                                                        .testTag("nav_streams_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LiveTv,
                                                        contentDescription = "Streams",
                                                        tint = if (isStreams) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Streams",
                                                        color = if (isStreams) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        fontWeight = if (isStreams) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                // 3. Favorites Tab
                                                val isFavorites = currentTab == "Favorites"
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clickable { currentTab = "Favorites" }
                                                        .testTag("nav_favorites_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Favorites",
                                                        tint = if (isFavorites) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Favorites",
                                                        color = if (isFavorites) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        fontWeight = if (isFavorites) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                // 4. Settings / Profile Tab
                                                val isSettings = currentTab == "Settings"
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clickable { currentTab = "Settings" }
                                                        .testTag("nav_settings_tab")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Profile",
                                                        tint = if (isSettings) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Profile",
                                                        color = if (isSettings) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                        fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.sp
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
