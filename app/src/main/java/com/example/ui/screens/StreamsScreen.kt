package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.ui.TvViewModel
import com.example.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()

    val chunkedChannels = remember(filteredChannels) { filteredChannels.chunked(2) }

    // Scroll state observation to enable floating PIP popup player on scroll
    val listState = rememberLazyListState()
    val isPlayerVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    if (isFullScreen) {
        // Immersive Full Screen Player Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        val premiumStadiumBg = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0C091C), // Deep premium sports indigo
                Color(0xFF070511), // Midnight stadium twilight
                Color(0xFF04030A)  // Rich Pitch Obsidian Black
            )
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(premiumStadiumBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Render Player at the top when first item is visible
                AnimatedVisibility(
                    visible = isPlayerVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    VideoPlayer(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // LazyColumn containing Search, Categories and the Channels List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("channel_list")
                ) {
                    // Item 1: Empty state placeholder if the player is scrolled offscreen (so list size doesn't jitter)
                    item {
                        if (!isPlayerVisible) {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // Item 2: Search Layout & Playlist Refresh Sync Button (Polished UX)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(viewModel.getLocalizedString("search_placeholder"), color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = Color.Gray)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("channel_search_bar")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Sync Playlist button with mint styling
                            IconButton(
                                onClick = { viewModel.refreshPlaylist() },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
                                    .size(52.dp)
                                    .testTag("refresh_m3u_button")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sync Playlist", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Featured Sports Networks & Real-time FIFA channels (visual matching aesthetic)
                    item {
                        val allChannels by viewModel.allChannels.collectAsState()
                        val fifaChannels = remember(allChannels) {
                            allChannels.filter { it.groupTitle == "FIFA World Cup 2026" }
                        }
                        FeaturedBroadcastersWidget(
                            fifaChannels = fifaChannels,
                            onChannelClick = { channel ->
                                viewModel.selectChannel(channel)
                            },
                            onSelectNetwork = { networkName ->
                                viewModel.updateSearchQuery(networkName)
                            }
                        )
                    }

                    // Item 3: Category horizontal selector bar
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(groups) { group ->
                                val isSelected = group == selectedGroup
                                val translatedGroup = if (group == "All") viewModel.getLocalizedString("all") else group
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectGroup(group) },
                                    label = { Text(translatedGroup, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("category_chip_$group")
                                )
                            }
                        }
                    }

                    // Channels List Header text
                    item {
                        Text(
                            text = if (selectedGroup == "All") "FIFA World Cup & Featured" else selectedGroup ?: "Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Channel items mapping
                    if (filteredChannels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = viewModel.getLocalizedString("no_channels"),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    } else {
                        items(chunkedChannels) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val channel1 = pair[0]
                                Box(modifier = Modifier.weight(1f)) {
                                    ChannelGridCard(
                                        channel = channel1,
                                        isPlaying = currentChannel?.url == channel1.url,
                                        viewModel = viewModel,
                                        onSelect = { viewModel.selectChannel(channel1) },
                                        onToggleFavorite = { viewModel.toggleFavorite(channel1) },
                                        onDownload = {
                                            if (channel1.isDownloaded) {
                                                viewModel.deleteDownload(channel1)
                                            } else {
                                                viewModel.startDownload(channel1)
                                            }
                                        }
                                    )
                                }
                                if (pair.size > 1) {
                                    val channel2 = pair[1]
                                    Box(modifier = Modifier.weight(1f)) {
                                        ChannelGridCard(
                                            channel = channel2,
                                            isPlaying = currentChannel?.url == channel2.url,
                                            viewModel = viewModel,
                                            onSelect = { viewModel.selectChannel(channel2) },
                                            onToggleFavorite = { viewModel.toggleFavorite(channel2) },
                                            onDownload = {
                                                if (channel2.isDownloaded) {
                                                    viewModel.deleteDownload(channel2)
                                                } else {
                                                    viewModel.startDownload(channel2)
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Draggable Floating POPUP PIP Player overlay (activated on scrolling)
            AnimatedVisibility(
                visible = currentChannel != null && !isPlayerVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 70.dp) // elevate above bottom bar
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier
                        .width(200.dp)
                        .height(140.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayer(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Floating close button overlay
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(100))
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Popup",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelGridCard(
    channel: Channel,
    isPlaying: Boolean,
    viewModel: TvViewModel,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("channel_grid_${channel.name}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Image/Logo area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null && channel.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "Channel Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (channel.groupTitle == "FIFA World Cup 2026") Icons.Default.EmojiEvents else Icons.Default.Tv,
                        contentDescription = "Default TV",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PLAYING",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (channel.groupTitle == "FIFA World Cup 2026") viewModel.getLocalizedString("world_cup") else channel.groupTitle ?: "Live TV",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Download progress bar
            if (channel.downloadProgress > 0f && channel.downloadProgress < 100f) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = channel.downloadProgress / 100f,
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.DarkGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actions (Favorite & Download)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.FileDownload,
                            contentDescription = "Download",
                            tint = if (channel.isDownloaded) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Small Play Control Action
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .clickable { onSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedMatchWidget(
    onWatchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700), // Pure Gold
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FIFA WORLD CUP 2026",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Live pulsing dot
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val liveAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red.copy(alpha = liveAlpha), shape = RoundedCornerShape(100))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LIVE MATCH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13112E).copy(alpha = 0.85f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF007F), // Glowing pink
                            Color(0xFF00FFCC)  // Neon teal
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .clickable { onWatchClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B).copy(alpha = 0.9f),
                                Color(0xFF0F0E26).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Match Bracket layout (Team A - TIME - Team B)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Team A: Argentina
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF00D2FF), Color(0xFF0066FF).copy(alpha = 0.1f))
                                        ),
                                        shape = RoundedCornerShape(100)
                                    )
                                    .border(1.5.dp, Color(0xFF00D2FF), RoundedCornerShape(100)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LM 10",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🇦🇷", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Argentina",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // TIME / SCORE
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Quarter-Final",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "21:00",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "LUSAIL STADIUM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Team B: Brazil
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFFFFFF00), Color(0xFF009900).copy(alpha = 0.1f))
                                        ),
                                        shape = RoundedCornerShape(100)
                                    )
                                    .border(1.5.dp, Color(0xFFFFFF00), RoundedCornerShape(100)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NJ 10",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🇧🇷", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Brazil",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Premium button link
                    Button(
                        onClick = onWatchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Premium Broadcast",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedBroadcastersWidget(
    fifaChannels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onSelectNetwork: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "FEATURED SPORTS NETWORKS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // First, show the actual real FIFA Channels with gorgeous gold trophies / icons
            items(fifaChannels) { channel ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onChannelClick(channel) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF1E2638), shape = RoundedCornerShape(100))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(100))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F1524), shape = RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "FIFA WC Channel",
                                tint = Color(0xFFD4AF37), // Luxury gold finish
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val shortTitle = when {
                        channel.name.contains("Live Match", ignoreCase = true) -> "USA vs BAN"
                        channel.name.contains("Ceremony", ignoreCase = true) -> "Opening"
                        else -> "Highlights"
                    }
                    Text(
                        text = shortTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.width(90.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Normal sports networks
            val networks = listOf(
                Pair("FOX", Color(0xFF003366)) to "FOX Sports",
                Pair("ESPN", Color(0xFFCC0000)) to "ESPN Live",
                Pair("Sky", Color(0xFF00A2E8)) to "Sky Sports",
                Pair("EBU", Color(0xFF6B3FA0)) to "EBU Sports"
            )

            items(networks) { network ->
                val pair = network.first
                val fullname = network.second
                val shortname = pair.first
                val color = pair.second

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectNetwork(shortname) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White, shape = RoundedCornerShape(100))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(100))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, shape = RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortname,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fullname,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
