package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.ui.TvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TvViewModel,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val playlistGroups by viewModel.groups.collectAsState()

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040608)) // Dark luxury black background
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Hamburger | Logo (MRB TV) | Search & Notifications
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Hamburger click action placeholder */ },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Logo: "MRB TV" with green glow look
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            // Reset group filter
                            viewModel.selectGroup("All")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "MRB",
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Glowing green outline box around "TV" with custom antennae
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp) // Space for antennae
                                    .drawBehind {
                                        val strokeWidth = 1.5.dp.toPx()
                                        val color = Color(0xFF00E676)
                                        // Left antenna
                                        drawLine(
                                            color = color,
                                            start = androidx.compose.ui.geometry.Offset(size.width * 0.35f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(size.width * 0.1f, -8.dp.toPx()),
                                            strokeWidth = strokeWidth
                                        )
                                        drawCircle(
                                            color = color,
                                            radius = 2.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, -8.dp.toPx())
                                        )
                                        // Right antenna
                                        drawLine(
                                            color = color,
                                            start = androidx.compose.ui.geometry.Offset(size.width * 0.65f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(size.width * 0.9f, -8.dp.toPx()),
                                            strokeWidth = strokeWidth
                                        )
                                        drawCircle(
                                            color = color,
                                            radius = 2.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, -8.dp.toPx())
                                        )
                                    }
                                    .border(2.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TV",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LIVE TV | MOVIES | SERIES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Right Search & Notifications bell (with badge '3')
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { onNavigateToTab("Streams") },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { onNavigateToTab("Favorites") },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Hero space/astronaut banner (Draw beautiful neon soccer field and World Cup 2026 trophy)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF051F0D), Color(0xFF020406))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                ) {
                    // Massive backdrop glowing "26" text as shown in screenshot
                    Text(
                        text = "26",
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        fontSize = 150.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 40.dp)
                            .offset(y = (-10).dp)
                    )

                    // Draw a gorgeous custom shining Gold FIFA World Cup Trophy
                    Canvas(
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp)
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        
                        // Glowing stadium green backdrop light
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E676).copy(alpha = 0.25f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                                radius = w * 1.0f
                            ),
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                            radius = w * 1.0f
                        )

                        // 1. Dark base pedestal of the Trophy
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF0E2818), Color(0xFF051109))),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.85f),
                            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.1f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        // Pedestal gold rings
                        drawRect(
                            color = Color(0xFFFFD700),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.83f),
                            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.02f)
                        )

                        // 2. Main golden cup body/stem rising upwards (sculpted wings)
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.35f, h * 0.83f)
                            quadraticTo(w * 0.3f, h * 0.6f, w * 0.25f, h * 0.4f)
                            quadraticTo(w * 0.35f, h * 0.22f, w * 0.5f, h * 0.22f)
                            quadraticTo(w * 0.65f, h * 0.22f, w * 0.75f, h * 0.4f)
                            quadraticTo(w * 0.7f, h * 0.6f, w * 0.65f, h * 0.83f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFB7791F))
                            )
                        )

                        // 3. Globe at the very top (representing Earth held up by the wings)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFFE65100)),
                                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.22f),
                                radius = w * 0.22f
                            ),
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.22f),
                            radius = w * 0.22f
                        )

                        // 4. Fine gold accents and wings details
                        drawArc(
                            color = Color.White.copy(alpha = 0.5f),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.15f),
                            size = androidx.compose.ui.geometry.Size(w * 0.24f, w * 0.15f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Top-right mini "FIFA WORLD CUP 2026" badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FIFA WORLD CUP\n2026",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Left Column: Text & Call to Action
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.62f)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header text
                        Column {
                            Text(
                                text = "FIFA",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "WORLD CUP",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "2026",
                                color = Color(0xFF00E676),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }

                        // Subtitle
                        Text(
                            text = "Every Match. Every Moment.\nLive & Exclusive on MRB TV",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.sp
                        )

                        // Watch Live Pill Button
                        Button(
                            onClick = { onNavigateToTab("Streams") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Watch Live",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
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
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
                    }
                }
            }

            // Categories Row: LIVE TV | MOVIES | SERIES | SPORTS | KIDS
            item {
                val categoryItems = listOf(
                    CategoryItem("LIVE TV", Icons.Default.Tv, "All"),
                    CategoryItem("MOVIES", Icons.Default.Movie, "Entertainment"),
                    CategoryItem("SERIES", Icons.Default.PlayCircle, "Entertainment"),
                    CategoryItem("SPORTS", Icons.Default.SportsSoccer, "Sports"),
                    CategoryItem("KIDS", Icons.Default.ChildCare, "KIDS")
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryItems) { item ->
                        val isSelected = selectedCategory == item.name
                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .height(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0x1A00E676) else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedCategory = item.name
                                    if (item.targetGroup != "KIDS") {
                                        viewModel.selectGroup(item.targetGroup)
                                        onNavigateToTab("Streams")
                                    } else {
                                        // KIDS maps to general entertainment / fun animations
                                        viewModel.selectGroup("Entertainment")
                                        onNavigateToTab("Streams")
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    tint = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.name,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Live Now (With beautiful custom brand cards)
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
                            // Left bold vertical bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF00E676), RoundedCornerShape(100.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Now",
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
                            var isStarred by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .width(145.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0C1014)) // Beautiful deep slate card
                                    .border(
                                        width = 1.dp,
                                        color = if (isPlaying) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.selectChannel(channel) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    // Row for LIVE badge and star favorite
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(Color.Red, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "LIVE",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isStarred) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { isStarred = !isStarred }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Channel logo in the middle
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(55.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            channel.name.contains("Sony Ten 2", ignoreCase = true) || channel.name.contains("Star Jalsha", ignoreCase = true) -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .background(Color(0xFF060B12), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("SONY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
                                                        Text("TEN 2", color = Color(0xFF00E676), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                            channel.name.contains("Sony Ten 1", ignoreCase = true) || channel.name.contains("Sony TV", ignoreCase = true) -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .background(Color(0xFF060B12), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("SONY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
                                                        Text("TEN 1", color = Color(0xFF00E676), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                            channel.name.contains("astro", ignoreCase = true) || channel.name.contains("Cricket", ignoreCase = true) -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .background(Color(0xFFE91E63), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("astro", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 8.sp)
                                                        Text("SUPER SPORT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                                                    }
                                                }
                                            }
                                            channel.name.contains("beIN", ignoreCase = true) || channel.name.contains("Cartoon", ignoreCase = true) -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .background(Color(0xFF0B1220), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("beIN", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                        Text("SPORTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
                                                    }
                                                }
                                            }
                                            else -> {
                                                if (channel.logoUrl != null && channel.logoUrl.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = channel.logoUrl,
                                                        contentDescription = channel.name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Tv,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00E676).copy(alpha = 0.8f),
                                                        modifier = Modifier.size(34.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Event Description
                                    Text(
                                        text = "FIFA World Cup 2026",
                                        color = if (isPlaying) Color(0xFF00E676) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Live Match",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = "Sports",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Bottom progress bar with time timeline matching 75', 45' from screenshot
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
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = progressText,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: World Cup 2026 Highlights Content
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
                                text = "World Cup 2026 Highlights",
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
                        items(featuredMovies) { movie ->
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        // Play standard demo stream URL
                                        viewModel.selectChannel(
                                            Channel(
                                                url = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8",
                                                name = movie.title,
                                                logoUrl = movie.thumbnailUrl,
                                                groupTitle = "Sports"
                                            )
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    AsyncImage(
                                        model = movie.thumbnailUrl,
                                        contentDescription = movie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Content Category Badge (e.g. HIGHLIGHTS)
                                    Box(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .background(Color(0xFF00E676), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = movie.badgeType,
                                            color = Color.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Circular play button in bottom-right corner matching screenshot
                                    Box(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .align(Alignment.BottomEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = movie.year, // Displays "Group Stage"
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Custom Match Reminder and Premium Upgrade widgets
            // Widget 1: Never Miss a Match!
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(115.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F1E15), Color(0xFF050D08))
                            )
                        )
                        .border(1.5.dp, Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Glowing golden trophy box
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFFFD700).copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                    .border(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Never Miss a Match!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Set reminders for live matches and get notified before the game starts.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // "Set Reminder" Button with notification bell icon
                        Button(
                            onClick = { /* Action to trigger notification permission / scheduler */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Set Reminder",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Widget 2: GO PREMIUM
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(115.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0A131F), Color(0xFF04080E))
                            )
                        )
                        .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Glowing golden crown box
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFFFD700).copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                    .border(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw beautiful premium crown
                                Canvas(modifier = Modifier.size(24.dp)) {
                                    val cw = size.width
                                    val ch = size.height
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, ch)
                                        lineTo(0f, ch * 0.35f)
                                        lineTo(cw * 0.25f, ch * 0.6f)
                                        lineTo(cw * 0.5f, ch * 0.15f)
                                        lineTo(cw * 0.75f, ch * 0.6f)
                                        lineTo(cw, ch * 0.35f)
                                        lineTo(cw, ch)
                                        close()
                                    }
                                    drawPath(path, Color(0xFFFFD700))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "GO PREMIUM",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Watch all FIFA World Cup 2026 matches Live in HD/4K. Ad-free Experience.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // "Upgrade Now" Button with arrow chevron
                        Button(
                            onClick = { onNavigateToTab("Settings") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Upgrade Now",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Add a generous bottom spacer to keep spacing clean above NavigationBar
            item {
                Spacer(modifier = Modifier.height(100.dp))
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

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val targetGroup: String
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
