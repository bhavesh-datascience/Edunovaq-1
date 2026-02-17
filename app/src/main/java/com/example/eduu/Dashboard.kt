package com.example.eduu

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel

// ==========================================
// 1. MAIN DASHBOARD CONTAINER
// ==========================================
@Composable
fun DashboardScreen(
    userEmail: String,
    userName: String,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dashboardViewModel: DashboardViewModel = viewModel()

    // Initialize Streak and Data
    LaunchedEffect(Unit) {
        val streak = updateAndGetStreak(context)
        dashboardViewModel.updateStreakUI(streak)
        dashboardViewModel.loadRealStats()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1E2E))))
            .systemBarsPadding()
    ) {
        // Ambient Glow Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF6366F1).copy(alpha = 0.1f), radius = 900f, center = Offset(size.width, 0f))
            drawCircle(color = Color(0xFFEC4899).copy(alpha = 0.05f), radius = 600f, center = Offset(0f, size.height))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tabIndex ->
                when (tabIndex) {
                    0 -> HomeTab(userName, onProfileClick, onCalendarClick, dashboardViewModel)
                    1 -> AITab()
                    2 -> ToolsTab()
                    3 -> MeetsTab(userEmail, onLogout)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp, start = 20.dp, end = 20.dp)
        ) {
            GlassNavigationPill(currentTab) { currentTab = it }
        }
    }
}

// ==========================================
// 2. HOME TAB (The New UI)
// ==========================================
@Composable
fun HomeTab(
    userName: String,
    onProfileClick: () -> Unit,
    onCalendarClick: () -> Unit,
    viewModel: DashboardViewModel
) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // --- 1. HEADER ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Edunovaq", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Hello, $userName", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderIconButton(Icons.Rounded.CalendarMonth, onCalendarClick)
                HeaderIconButton(Icons.Rounded.Notifications, { /* TODO */ })
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8))))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = userName.take(1).uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 2. TOP STAT CARDS (FIXED LAYOUT) ---
        // Increased height slightly to 160.dp to ensure text fits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Study Streak
            GradientStatCard(
                modifier = Modifier.weight(1f).height(160.dp),
                title = "Study Streak",
                value = stats.studyStreak,
                icon = "🔥",
                subtext = "Keep it up!",
                colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
            )

            // Card 2: Total Hours
            GradientStatCard(
                modifier = Modifier.weight(1f).height(160.dp),
                title = "Total Hours",
                value = stats.totalHours,
                icon = null,
                subtext = "Great progress!",
                colors = listOf(Color(0xFF06B6D4), Color(0xFF22C55E))
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 3. DYNAMIC GRAPH ---
        Text("Study Hours (Last 7 Days)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DashboardGlassCard(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            Column(Modifier.padding(20.dp)) {
                StudyLineChart(
                    dataPoints = stats.weeklyData,
                    labels = stats.dayLabels,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 4. PERFORMANCE ---
        Text("Performance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DashboardGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                PerformanceRow("Study (Today)", "${stats.studyHours}h",
                    progress = (stats.studyHours.toFloatOrNull() ?: 0f) / 8f,
                    color = Color(0xFF8B5CF6)
                )
                Spacer(modifier = Modifier.height(20.dp))
                PerformanceRow("Task Completion", "${(stats.taskCompletionRate * 100).toInt()}%",
                    progress = stats.taskCompletionRate,
                    color = Color(0xFF06B6D4)
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

// ==========================================
// 3. FIXED COMPONENTS
// ==========================================

@Composable
fun GradientStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: String?,
    subtext: String,
    colors: List<Color>
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp) // Reduced padding slightly to give content more room
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // Use full size of box
            verticalArrangement = Arrangement.Center, // Center vertically
            horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 36.sp, // Slightly smaller font to prevent overflow
                    fontWeight = FontWeight.Bold
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = icon, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtext,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StudyLineChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier
) {
    val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(1500), label = "graph")

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (dataPoints.size - 1)
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value / maxVal * height * 0.8f * progress)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * spacing
                val prevY = height - (dataPoints[index - 1] / maxVal * height * 0.8f * progress)
                val controlX1 = prevX + (x - prevX) / 2
                val controlX2 = prevX + (x - prevX) / 2
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
            drawCircle(color = Color(0xFF8B5CF6), radius = 6f, center = Offset(x, y))
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(path = path, color = Color(0xFF8B5CF6), style = Stroke(width = 5f, cap = StrokeCap.Round))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label -> Text(label, color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
fun PerformanceRow(label: String, valueText: String, progress: Float, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontWeight = FontWeight.Medium)
            Text(valueText, color = Color.White.copy(alpha = 0.8f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f),
        )
    }
}

// --- KEEP EXISTING HELPERS ---
@Composable
fun HeaderIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.1f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DashboardGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), content = content)
}

@Composable fun AITab() { AIScreen() }
@Composable fun ToolsTab() { ToolsScreen() }
@Composable fun MeetsTab(email: String, onLogout: () -> Unit) { StudyMeetsScreen() }

@Composable fun GlassNavigationPill(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(color = Color(0xFF0F172A).copy(0.9f), shape = RoundedCornerShape(50.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f)), modifier = Modifier.height(70.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavIcon(Icons.Filled.Home, "Home", selectedTab == 0) { onTabSelected(0) }
            NavIcon(Icons.Filled.AutoAwesome, "AI", selectedTab == 1) { onTabSelected(1) }
            NavIcon(Icons.Filled.Construction, "Tools", selectedTab == 2) { onTabSelected(2) }
            NavIcon(Icons.Filled.VideoCall, "Meets", selectedTab == 3) { onTabSelected(3) }
        }
    }
}
@Composable fun NavIcon(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF6366F1) else Color.Gray
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f, label = "scale")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClickable { onClick() }) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp * scale))
        if (isSelected) { Spacer(Modifier.height(4.dp)); Box(Modifier.size(4.dp).clip(CircleShape).background(color)) }
    }
}
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
}
@SuppressLint("UseKtx")
fun updateAndGetStreak(context: Context): Int {
    val prefs = context.getSharedPreferences("edunovaq_prefs", Context.MODE_PRIVATE)
    val lastLogin = prefs.getLong("last_login_day", 0L)
    val currentStreak = prefs.getInt("user_streak", 0)
    val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    if (lastLogin == today) return currentStreak
    val newStreak = if (lastLogin == today - 1) currentStreak + 1 else 1
    prefs.edit { putLong("last_login_day", today); putInt("user_streak", newStreak) }
    return newStreak
}