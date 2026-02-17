package com.example.eduu



import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// Main Controller for Section 4
// ==========================================
@Composable
fun StudyMeetsScreen() {
    // 0 = Dashboard, 1 = Friend Group, 2 = Global Study
    var currentView by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1E2E))))
            .systemBarsPadding()
    ) {
        when (currentView) {
            0 -> StudyMeetsDashboard(
                onNavigateFriends = { currentView = 1 },
                onNavigateGlobal = { currentView = 2 }
            )
            1 -> FriendGroupCallUI(onBack = { currentView = 0 })
            2 -> GlobalStudyUI(onBack = { currentView = 0 })
        }
    }
}

// ==========================================
// 1. Dashboard (The Selection Menu)
// ==========================================
@Composable
fun StudyMeetsDashboard(
    onNavigateFriends: () -> Unit,
    onNavigateGlobal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Study Together",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Rounded.Groups, null, tint = Color(0xFF4DB6AC), modifier = Modifier.size(32.dp))
        }

        Text(
            "Connect with your squad or find a study buddy.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // --- Option 1: Friend Group Call ---
        GlassCard(
            onClick = onNavigateFriends,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Decorative Gradient Blob
                Box(modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape)
                    .blur(20.dp)
                )

                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    // Avatar Stack Mock
                    Row(modifier = Modifier.padding(bottom = 16.dp)) {
                        AvatarCircle(Color(0xFFFF8A80))
                        AvatarCircle(Color(0xFF82B1FF), (-10).dp)
                        AvatarCircle(Color(0xFFA5D6A7), (-20).dp)
                        Box(
                            modifier = Modifier
                                .offset(x = (-30).dp)
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .border(2.dp, Color(0xFF1E1E2E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+3", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Text("Friend Group Call", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Private video rooms & screen share", color = Color.Gray, fontSize = 13.sp)
                }

                Icon(
                    Icons.Rounded.VideoCall,
                    null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Option 2: Study with Others ---
        GlassCard(
            onClick = onNavigateGlobal,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Decorative Gradient Blob
                Box(modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(Color(0xFF00BFA5).copy(alpha = 0.3f), CircleShape)
                    .blur(20.dp)
                )

                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF00BFA5).copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00BFA5), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1,204 Online", color = Color(0xFF00BFA5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Study with Others", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Join silent global libraries", color = Color.Gray, fontSize = 13.sp)
                }

                Icon(
                    Icons.Rounded.Public,
                    null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(48.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. Friend Group UI
// ==========================================
@Composable
fun FriendGroupCallUI(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Nav Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Friend Groups", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Rooms
        Text("ACTIVE NOW", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFF6366F1), CircleShape), contentAlignment = Alignment.Center) {
                    Text("CS", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("CS Exams Squad", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("3 members • 2h 10m", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC)),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Join")
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Create New
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            onClick = { /* TODO: Create Logic */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.AddIcCall, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Start New Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Invite friends to study", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// 3. Global Study UI
// ==========================================
@Composable
fun GlobalStudyUI(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Nav Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Global Library", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Categories
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            FilterChip(selected = false, onClick = {}, label = { Text("Silent") })
            FilterChip(selected = false, onClick = {}, label = { Text("Lofi Music") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rooms List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) { index ->
                PublicRoomItem(index)
            }
        }
    }
}

@Composable
fun PublicRoomItem(index: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Room Icon/Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF26A69A), Color(0xFF00695C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = Color.White.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    if (index == 0) "Lofi Study Girl 24/7" else "Late Night Grind #${index+1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Rounded.Person, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(
                        " ${(100..500).random()} studying",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
        }
    }
}

// ==========================================
// Helper Components (Glassmorphism)
// ==========================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(content = content)
    }
}

@Composable
fun AvatarCircle(color: Color, xOffset: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        modifier = Modifier
            .offset(x = xOffset)
            .size(36.dp)
            .background(color, CircleShape)
            .border(2.dp, Color(0xFF1E1E2E), CircleShape)
    )
}

// Extension for Modifier.blur if not using Android 12+ usually requires a library or custom render effect.
// For standard Compose without external libs, we might skip actual blur or use alpha overlay.
// I'll simulate blur with a simple modifier placeholder or just alpha for compatibility.
fun Modifier.blur(radius: androidx.compose.ui.unit.Dp): Modifier {
    // Note: Real blur requires API 31+ or RenderEffect.
    // Just returning self for compatibility in this snippet.
    return this
}