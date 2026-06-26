package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.ui.TvViewModel
import com.example.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TvViewModel,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val playlistGroups by viewModel.groups.collectAsState()
    val context = LocalContext.current

    // Setup beautiful category tabs as requested: "LIVE TV", "MOVIES", "SERIES", "SPORTS", "KIDS"
    var selectedCategory by remember { mutableStateOf("LIVE TV") }

    // Dynamic Live TV channels filtering
    val liveChannels = remember(allChannels) {
        val filtered = allChannels.filter { 
            it.groupTitle?.contains("Sport", ignoreCase = true) == true || 
            it.groupTitle?.contains("Bangla", ignoreCase = true) == true ||
            it.groupTitle?.contains("Entertainment", ignoreCase = true) == true
        }
        if (filtered.isNotEmpty()) filtered.take(6) else getFallbackLiveChannels()
    }

    // Dynamic Featured Content
    val featuredMovies = remember { getFeaturedMovies() }

    val listState = rememberLazyListState()
    val isInlinePlayerVisible by remember {
        derivedStateOf {
            if (currentChannel == null) false
            else listState.firstVisibleItemIndex <= 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040608)) // Dark luxury black background
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Hamburger | Logo (MRB TV) | Search & Notifications
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu Hamburger Button
                    IconButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Settings Panel opened", android.widget.Toast.LENGTH_SHORT).show()
                            onNavigateToTab("Settings")
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Logo: "MRB TV" exactly as shown in mockup
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            viewModel.selectGroup("All")
                        }
                    ) {
                        // Custom television icon on top
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 28.dp)
                                .drawBehind {
                                    val strokeWidth = 1.8.dp.toPx()
                                    val greenGlow = Color(0xFF00E676)
                                    // Antennas
                                    drawLine(
                                        color = greenGlow,
                                        start = androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.15f),
                                        end = androidx.compose.ui.geometry.Offset(size.width * 0.2f, -6.dp.toPx()),
                                        strokeWidth = strokeWidth
                                    )
                                    drawCircle(
                                        color = greenGlow,
                                        radius = 2.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, -6.dp.toPx())
                                    )
                                    drawLine(
                                        color = greenGlow,
                                        start = androidx.compose.ui.geometry.Offset(size.width * 0.55f, size.height * 0.15f),
                                        end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, -6.dp.toPx()),
                                        strokeWidth = strokeWidth
                                    )
                                    drawCircle(
                                        color = greenGlow,
                                        radius = 2.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, -6.dp.toPx())
                                    )
                                    // TV Frame
                                    drawRoundRect(
                                        color = greenGlow,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.2f),
                                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.8f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                        style = Stroke(width = strokeWidth)
                                    )
                                    // TV screen glowing filling
                                    drawRoundRect(
                                        color = greenGlow.copy(alpha = 0.15f),
                                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.32f),
                                        size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.56f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "MRB",
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TV",
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color(0xFF00E676),
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Text(
                            text = "LIVE ANYWHERE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 2.5.sp
                        )
                    }

                    // Right Side: Search and Notification with Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search Button
                        IconButton(
                            onClick = { onNavigateToTab("Streams") },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.2.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Notification bell
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "No new alerts. You are up to date with live streams!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.2.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Box(modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00E676), CircleShape)
                                        .border(1.5.dp, Color(0xFF040608), CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }

            // Active Video Player on Home Screen (Plays beautiful Live TV directly)
            if (currentChannel != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        VideoPlayer(
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.2.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        )
                    }
                }
            }

            // Hero space/stadium banner (Draw beautiful neon soccer field and World Cup 2026 trophy)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF04180A), Color(0xFF020406))
                            )
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                ) {
                    // Backstage subtle glowing grid lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Subtle stadium perspective lines
                        drawLine(
                            color = Color(0xFF00E676).copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(w * 0.1f, h),
                            end = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.4f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFF00E676).copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(w * 0.9f, h),
                            end = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.4f),
                            strokeWidth = 2f
                        )
                    }

                    // Massive backdrop glowing "2026" text as shown in screenshot
                    Text(
                        text = "2026",
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        fontSize = 110.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                            .offset(y = (-5).dp)
                    )

                    // Draw a gorgeous custom shining Gold FIFA World Cup Trophy
                    Canvas(
                        modifier = Modifier
                            .width(130.dp)
                            .height(190.dp)
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        
                        // Glowing stadium green backdrop light
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E676).copy(alpha = 0.3f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                                radius = w * 0.9f
                            ),
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                            radius = w * 0.9f
                        )

                        // Base pedestal of the Trophy
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF10361C), Color(0xFF05150A))),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.83f),
                            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.09f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        // Pedestal gold rings
                        drawRect(
                            color = Color(0xFFFFD700),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.81f),
                            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.02f)
                        )

                        // Main golden cup body/stem rising upwards (sculpted wings)
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.35f, h * 0.81f)
                            quadraticTo(w * 0.3f, h * 0.55f, w * 0.25f, h * 0.35f)
                            quadraticTo(w * 0.35f, h * 0.18f, w * 0.5f, h * 0.18f)
                            quadraticTo(w * 0.65f, h * 0.18f, w * 0.75f, h * 0.35f)
                            quadraticTo(w * 0.7f, h * 0.55f, w * 0.65f, h * 0.81f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFD84315))
                            )
                        )

                        // Globe at the very top (representing Earth held up by the wings)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFE65100)),
                                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f),
                                radius = w * 0.24f
                            ),
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f),
                            radius = w * 0.24f
                        )
                    }

                    // Top-left "LIVE NOW" badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp, start = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .border(1.2.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00E676), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE NOW",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Left Column: Text & Watch Live
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.62f)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Title space
                        Text(
                            text = "FIFA",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "WORLD CUP",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "2026",
                            color = Color(0xFF00E676),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 38.sp,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Canada | USA | Mexico",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Watch Live Pill Button
                        Button(
                            onClick = { onNavigateToTab("Streams") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WATCH LIVE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Slide indicators (dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp, 4.dp).background(Color(0xFF00E676), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                    }
                }
            }

            // Premium Glassmorphic Quick Menu Row: LIVE TV | MOVIES | SPORTS | FAVORITES | CATEGORIES
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xD0060A13))
                        .border(1.2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. LIVE TV
                        val isLiveSelected = selectedCategory == "LIVE TV"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedCategory = "LIVE TV"
                                    viewModel.selectGroup("All")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Live TV",
                                tint = if (isLiveSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "LIVE TV",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiveSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(if (isLiveSelected) Color(0xFF00E676) else Color.Transparent, RoundedCornerShape(1.dp))
                            )
                        }

                        // 2. MOVIES
                        val isMoviesSelected = selectedCategory == "MOVIES"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedCategory = "MOVIES"
                                    viewModel.selectGroup("Entertainment")
                                    onNavigateToTab("Streams")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Movies",
                                tint = if (isMoviesSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "MOVIES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMoviesSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(if (isMoviesSelected) Color(0xFF00E676) else Color.Transparent, RoundedCornerShape(1.dp))
                            )
                        }

                        // 3. SPORTS
                        val isSportsSelected = selectedCategory == "SPORTS"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedCategory = "SPORTS"
                                    viewModel.selectGroup("Sports")
                                    onNavigateToTab("Streams")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = "Sports",
                                tint = if (isSportsSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "SPORTS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSportsSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(if (isSportsSelected) Color(0xFF00E676) else Color.Transparent, RoundedCornerShape(1.dp))
                            )
                        }

                        // 4. FAVORITES
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    onNavigateToTab("Favorites")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Favorites",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "FAVORITES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Box(modifier = Modifier.height(2.dp))
                        }

                        // 5. CATEGORIES
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    onNavigateToTab("Streams")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Categories",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "CATEGORIES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Box(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            // Section 1: Live TV
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF00E676), RoundedCornerShape(100.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE TV",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToTab("Streams") }
                        ) {
                            Text(
                                text = "View All",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "View All",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(liveChannels) { channel ->
                            val isPlaying = currentChannel?.url == channel.url
                            AnimatedLiveChannelCard(
                                channel = channel,
                                isPlaying = isPlaying,
                                onClick = { viewModel.selectChannel(channel) }
                            )
                        }
                    }
                }
            }

            // Section: Watch History
            if (watchHistory.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .background(Color(0xFFFFB300), RoundedCornerShape(100.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WATCH HISTORY",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.clearWatchHistory() }
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(watchHistory) { channel ->
                                val isPlaying = currentChannel?.url == channel.url
                                AnimatedLiveChannelCard(
                                    channel = channel,
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.selectChannel(channel) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Now Playing (Champions League Showdown Card)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF07111E), Color(0xFF04070D))
                                )
                            )
                            .border(1.2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                            .clickable {
                                // Select Champions League match channel
                                viewModel.selectChannel(
                                    Channel(
                                        name = "Champions League HD",
                                        logoUrl = "",
                                        groupTitle = "Sports",
                                        url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
                                    )
                                )
                            }
                    ) {
                        // Drawing custom stadium light & VS graphic backdrop
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle neon blue glowing base light on left (Man City)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF00D2FF).copy(alpha = 0.15f), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.5f),
                                    radius = w * 0.4f
                                ),
                                center = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.5f),
                                radius = w * 0.4f
                            )

                            // Draw subtle neon gold glowing base light on right (Real Madrid)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFB300).copy(alpha = 0.15f), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.5f),
                                    radius = w * 0.4f
                                ),
                                center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.5f),
                                radius = w * 0.4f
                            )
                        }

                        // Right side graphics (VS and players glowing layout)
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sky blue glow crest (Man City mockup)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFF00D2FF).copy(alpha = 0.15f), CircleShape)
                                    .border(1.2.dp, Color(0xFF00D2FF).copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MCFC",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // VS Glowing middle
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "VS",
                                    color = Color(0xFF00E676),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            // White crest (Real Madrid mockup)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .border(1.2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "RMCF",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Left Side details
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.55f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Badge "NOW PLAYING"
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "NOW PLAYING",
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Champions League",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Man City vs Real Madrid",
                                    color = Color(0xFF00E676),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // State details
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFFF1744), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color(0xFFFF1744),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "90:35",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Timeline progress and play/pause
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFF00FF87), Color(0xFF00E676))
                                                ),
                                                RoundedCornerShape(50.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Explore Categories
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF00E676), RoundedCornerShape(100.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXPLORE CATEGORIES",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToTab("Streams") }
                        ) {
                            Text(
                                text = "View All",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "View All",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5 glowing cards of News, Entertainment, Kids, Music, Documentary
                    val exploreCategories = listOf(
                        ExploreCategoryItem("NEWS", Icons.Default.Language, Color(0xFF007BFF), "News"),
                        ExploreCategoryItem("ENTERTAINMENT", Icons.Default.MovieFilter, Color(0xFF9C27B0), "Entertainment"),
                        ExploreCategoryItem("KIDS", Icons.Default.Face, Color(0xFFFF9800), "Kids"),
                        ExploreCategoryItem("MUSIC", Icons.Default.MusicNote, Color(0xFFE91E63), "Music"),
                        ExploreCategoryItem("DOCUMENTARY", Icons.Default.Videocam, Color(0xFF4CAF50), "Documentary")
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(exploreCategories) { item ->
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF131822).copy(alpha = 0.85f),
                                                Color(0xFF070A10).copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                item.glowColor.copy(alpha = 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        viewModel.selectGroup(item.groupName)
                                        onNavigateToTab("Streams")
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Glowing background aura behind the icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .drawBehind {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(item.glowColor.copy(alpha = 0.25f), Color.Transparent),
                                                    radius = size.width * 0.8f
                                                ),
                                                radius = size.width * 0.8f
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = item.glowColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = item.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Spacers to prevent floating navigation bar overlap
            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        // Draggable Floating POPUP PIP Player overlay (activated on scrolling)
        AnimatedVisibility(
            visible = currentChannel != null && !isInlinePlayerVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // elevate above bottom bar
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .width(200.dp)
                    .height(140.dp)
                    .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoPlayer(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Floating close button overlay
                    IconButton(
                        onClick = { viewModel.stopPlayback() },
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

@Composable
fun LuxuryCategoryCard(
    groupName: String,
    channelCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = remember(groupName) {
        when {
            groupName.contains("FIFA", ignoreCase = true) -> Icons.Default.EmojiEvents
            groupName.contains("Sport", ignoreCase = true) -> Icons.Default.SportsSoccer
            groupName.contains("Entertainment", ignoreCase = true) -> Icons.Default.Movie
            groupName.contains("Bangla", ignoreCase = true) -> Icons.Default.Tv
            groupName.contains("News", ignoreCase = true) -> Icons.Default.Info
            groupName.contains("Kids", ignoreCase = true) -> Icons.Default.ChildCare
            else -> Icons.Default.Tv
        }
    }

    val glowColor = remember(groupName) {
        when {
            groupName.contains("FIFA", ignoreCase = true) -> Color(0xFFFFD700) // Gold glow
            groupName.contains("Sport", ignoreCase = true) -> Color(0xFF00E676) // Neon Green glow
            groupName.contains("Entertainment", ignoreCase = true) -> Color(0xFF00B0FF) // Sky Blue glow
            else -> Color(0xFF00E676).copy(alpha = 0.8f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C101B), // Translucent premium card top
                        Color(0xFF05070C)  // Translucent premium card bottom
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        // Decorative canvas glow effect in the card corner
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f),
                    radius = size.width * 0.4f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.width * 0.4f
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon on left, Channel count on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow Icon frame
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(glowColor.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, glowColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = groupName,
                        tint = glowColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tiny glowing count capsule
                Box(
                    modifier = Modifier
                        .background(glowColor.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                        .border(0.8.dp, glowColor.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$channelCount CH",
                        color = glowColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom Row: Group Name and Action Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Premium Live",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PulsingLiveIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .size(7.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            .background(Color(0xFFFF1744), CircleShape)
    )
}

@Composable
fun AnimatedCategoryCard(
    item: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val containerColor = if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
    val borderColor = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f)
    val iconColor = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f)
    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .width(82.dp)
            .height(92.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(width = 1.2.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimatedLiveChannelCard(
    channel: Channel,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    var isStarred by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .width(155.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10161E),
                        Color(0xFF0A0E14)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = if (isPlaying) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingLiveIndicator()
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        color = Color(0xFFFF1744),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Icon(
                    imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isStarred) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { isStarred = !isStarred }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF060B12).copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        channel.name.contains("Sony Ten 2", ignoreCase = true) || channel.name.contains("Star Jalsha", ignoreCase = true) -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SONY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                Text("TEN 2", color = Color(0xFF00E676), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                        channel.name.contains("Sony Ten 1", ignoreCase = true) || channel.name.contains("Sony TV", ignoreCase = true) -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SONY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                Text("TEN 1", color = Color(0xFF00E676), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                        channel.name.contains("astro", ignoreCase = true) || channel.name.contains("Cricket", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE91E63)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("astro", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 9.sp)
                                    Text("SUPER SPORT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                        channel.name.contains("beIN", ignoreCase = true) || channel.name.contains("Cartoon", ignoreCase = true) -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("beIN", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text("SPORTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }
                        else -> {
                            if (channel.logoUrl != null && channel.logoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(6.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676).copy(alpha = 0.8f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "FIFA World Cup 2026",
                color = if (isPlaying) Color(0xFF00E676) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Live Match",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1
            )

            Text(
                text = "Sports",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            val progressVal = when {
                channel.name.contains("Sony Ten 2") || channel.name.contains("Star Jalsha") -> 0.75f
                channel.name.contains("Sony Ten 1") || channel.name.contains("Sony TV") -> 0.45f
                channel.name.contains("astro") || channel.name.contains("Cricket") -> 0.60f
                else -> 0.30f
            }
            val progressText = when {
                channel.name.contains("Sony Ten 2") || channel.name.contains("Star Jalsha") -> "75'"
                channel.name.contains("Sony Ten 1") || channel.name.contains("Sony TV") -> "45'"
                channel.name.contains("astro") || channel.name.contains("Cricket") -> "60%"
                else -> "30%"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = progressVal,
                    color = Color(0xFF00E676),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = progressText,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val targetGroup: String
)

data class ExploreCategoryItem(
    val name: String,
    val icon: ImageVector,
    val glowColor: Color,
    val groupName: String
)

data class FeaturedMovieItem(
    val title: String,
    val year: String,
    val genre: String,
    val badgeType: String,
    val thumbnailUrl: String
)

// Fallback high-fidelity local channels that look exactly like the screenshot out-of-the-box
fun getFallbackLiveChannels(): List<Channel> {
    return listOf(
        Channel(
            name = "Star Jalsha HD",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/f/ff/Star_Jalsha_logo.png",
            groupTitle = "Entertainment",
            url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
        ),
        Channel(
            name = "Sony TV HD",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/0/09/Sony_Entertainment_Television_logo.svg",
            groupTitle = "Entertainment",
            url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
        ),
        Channel(
            name = "Sky Sports Cricket",
            logoUrl = "https://upload.wikimedia.org/wikipedia/en/9/91/Sky_Sports_logo.svg",
            groupTitle = "Sports",
            url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
        ),
        Channel(
            name = "Cartoon Network",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/80/Cartoon_Network_2010_logo.svg",
            groupTitle = "Kids",
            url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
        )
    )
}

fun getFeaturedMovies(): List<FeaturedMovieItem> {
    return listOf(
        FeaturedMovieItem(
            title = "Avatar: The Way of Water",
            year = "2022",
            genre = "Action, Adventure",
            badgeType = "MOVIE",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop"
        ),
        FeaturedMovieItem(
            title = "The Witcher Season 3",
            year = "2023",
            genre = "Action, Adventure, Drama",
            badgeType = "SERIES",
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop"
        )
    )
}
