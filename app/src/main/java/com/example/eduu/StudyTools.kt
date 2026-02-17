package com.example.eduu

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

// ==========================================
// Data Models
// ==========================================
data class BlockableApp(
    val id: Long,
    val name: String,
    val icon: ImageVector,
    var isBlocked: Boolean
)

// ==========================================
// 1. Main Tools Controller
// ==========================================
@Composable
fun ToolsScreen() {
    // 0 = Menu, 1 = Pomodoro, 2 = Scroll Block
    var activeTool by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1E2E))))
            .systemBarsPadding()
    ) {
        when (activeTool) {
            0 -> ToolsMenu(onNavigate = { activeTool = it })
            1 -> PomodoroFocusScreen(onBack = { activeTool = 0 })
            2 -> ScrollBlockScreen(onBack = { activeTool = 0 })
        }
    }
}

// ==========================================
// 2. Tools Menu
// ==========================================
@Composable
fun ToolsMenu(onNavigate: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Study Tools", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Boost your productivity", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // Tool 1: Pomodoro
        ToolCard(
            icon = Icons.Rounded.Timer,
            title = "Pomodoro Focus",
            desc = "Stay focused with the classic technique.",
            color = Color(0xFF6366F1), // Indigo
            onClick = { onNavigate(1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tool 2: Scroll Block
        ToolCard(
            icon = Icons.Rounded.Block,
            title = "Scroll Block",
            desc = "Block distracting apps & Focus.",
            color = Color(0xFFF43F5E), // Red/Pink
            onClick = { onNavigate(2) }
        )
    }
}

@Composable
fun ToolCard(icon: ImageVector, title: String, desc: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// 3. Scroll Block Screen (Updated with Safety Checks)
// ==========================================
@Composable
fun ScrollBlockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current // Check if running in Android Studio Preview

    // SAFE ACCESS: Shared Preferences
    val prefs = remember {
        if (!isPreview) {
            try { context.getSharedPreferences("study_tools_prefs", Context.MODE_PRIVATE) }
            catch (e: Exception) { null }
        } else null
    }

    // SAFE ACCESS: Service Checking Logic
    // We default to 'false' if in preview mode to prevent render crashes
    var isServiceEnabled by remember {
        mutableStateOf(
            if (isPreview) false
            else isAccessibilityServiceEnabled(context, "com.example.eduu.BlockerService")
        )
    }

    // Lifecycle observer to auto-refresh status (Skipped in Preview)
    if (!isPreview) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isServiceEnabled = isAccessibilityServiceEnabled(context, "com.example.eduu.BlockerService")
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    val apps = remember {
        mutableStateListOf(
            BlockableApp(1, "Instagram", Icons.Rounded.PhotoCamera, true),
            BlockableApp(2, "YouTube", Icons.Rounded.PlayArrow, true),
            BlockableApp(3, "TikTok", Icons.Rounded.MusicNote, true),
            BlockableApp(4, "Facebook", Icons.Rounded.Facebook, false),
            BlockableApp(5, "Snapchat", Icons.Rounded.ChatBubble, false),
            BlockableApp(6, "Games", Icons.Rounded.SportsEsports, false)
        )
    }

    var isFocusMode by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableLongStateOf(0L) }
    var currentQuote by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val quotes = listOf(
        "Focus is the key to all success.",
        "Simplicity is the ultimate sophistication.",
        "Distraction is the enemy of direction.",
        "The best way out is always through.",
        "Your future is created by what you do today."
    )

    // Sync Logic (Safe)
    LaunchedEffect(isFocusMode, apps.toList()) {
        if (prefs != null) {
            try {
                // 1. Save Focus State
                prefs.edit().putBoolean("is_focus_active", isFocusMode).apply()
                // 2. Save Blocked List
                val blockedNames = apps.filter { it.isBlocked }.map { it.name }.toSet()
                prefs.edit().putStringSet("blocked_packages", blockedNames).apply()
            } catch (e: Exception) {
                // Ignore prefs errors in unstable environments
            }
        }

        // 3. Timer Logic
        if (isFocusMode) {
            val startTime = System.currentTimeMillis()
            while (isFocusMode) {
                timerSeconds = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        } else {
            timerSeconds = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // --- Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scroll Block", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- Status Card (Service Indicator) ---
            Surface(
                color = if(isServiceEnabled) Color(0xFF00C853).copy(alpha = 0.1f) else Color(0xFFFFAB00).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if(isServiceEnabled) Color(0xFF00C853).copy(alpha = 0.3f) else Color(0xFFFFAB00).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable {
                        // Open Settings if not enabled, skip if in preview
                        if (!isServiceEnabled && !isPreview) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if(isServiceEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        null,
                        tint = if(isServiceEnabled) Color(0xFF00C853) else Color(0xFFFFAB00),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if(isServiceEnabled) "System Blocker Active" else "Tap to enable Blocking Service",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        if(!isServiceEnabled) {
                            Text(
                                "Required to close other apps",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // --- App Grid ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 220.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(apps) { app ->
                    AppGridCard(app) {
                        val index = apps.indexOf(app)
                        apps[index] = app.copy(isBlocked = !app.isBlocked)
                    }
                }
            }
        }

        // --- Bottom Floating Buttons ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp)
        ) {
            Surface(
                color = Color(0xFF1B262C).copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showAddDialog = true }
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add App", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Focus Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (apps.any { it.isBlocked }) {
                                    currentQuote = quotes.random()
                                    isFocusMode = true
                                }
                            }
                            .background(Brush.horizontalGradient(listOf(Color(0xFF00695C), Color(0xFF4DB6AC)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Focus", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Dialog: Add Custom App ---
        if (showAddDialog) {
            AddAppDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name ->
                    apps.add(BlockableApp(System.currentTimeMillis(), name, Icons.Rounded.Android, true))
                    showAddDialog = false
                }
            )
        }

        // --- Focus Overlay ---
        AnimatedVisibility(
            visible = isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FocusModeOverlay(
                quote = currentQuote,
                seconds = timerSeconds,
                onStop = { isFocusMode = false }
            )
        }
    }
}

// --- Helper Functions (Safeguarded) ---

fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
    // 1. Catch specific exceptions for previews
    try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager ?: return false
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.contains(serviceName, ignoreCase = true)) return true
        }
        return false
    } catch (e: Exception) {
        // Return false if any system call fails (common in preview/simulators)
        return false
    }
}

// ... (Rest of UI Components: AppGridCard, AddAppDialog, FocusModeOverlay, PomodoroFocusScreen remain consistent)

@Composable
fun AppGridCard(app: BlockableApp, onToggle: () -> Unit) {
    val bgAlpha = if (app.isBlocked) 0.15f else 0.05f
    val bgColor = if (app.isBlocked) Color(0xFFF43F5E) else Color.White
    val borderColor = if (app.isBlocked) Color(0xFFF43F5E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(app.icon, null, tint = if(app.isBlocked) Color(0xFFFF5252) else Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if(app.isBlocked) Color(0xFFFF5252) else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (app.isBlocked) {
                Icon(Icons.Rounded.Lock, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun AddAppDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add Custom App", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("App Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4DB6AC),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (text.isNotBlank()) onAdd(text) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))) { Text("Add", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun FocusModeOverlay(quote: String, seconds: Long, onStop: () -> Unit) {
    val mins = seconds / 60
    val secs = seconds % 60
    val timeString = "%02d:%02d".format(mins, secs)
    // Only intercept Back press if NOT in preview mode (prevents getting stuck in preview)
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        BackHandler(enabled = true) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .clickable(enabled = true) { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF4DB6AC).copy(alpha = 0.1f), Color.Transparent)))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.Lock, null, tint = Color(0xFF4DB6AC), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("FOCUS MODE ACTIVE", color = Color(0xFF4DB6AC), style = MaterialTheme.typography.titleMedium, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(40.dp))
            Text(timeString, color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Spacer(modifier = Modifier.height(40.dp))
            Text("\"$quote\"", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, lineHeight = 28.sp)
            Spacer(modifier = Modifier.height(80.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable { onStop() }
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text("GIVE UP SESSION", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

// ==========================================
// 4. Pomodoro Focus Screen (With Safety Wrappers)
// ==========================================
@Composable
fun PomodoroFocusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusTime = 25 * 60L
    val shortBreakTime = 5 * 60L

    var timeLeft by remember { mutableLongStateOf(focusTime) }
    var totalTime by remember { mutableLongStateOf(focusTime) }
    var isRunning by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf("Start Time") }

    val targetColor = when (currentMode) {
        "Start Time" -> Color(0xFF6366F1)
        else -> Color(0xFF10B981)
    }

    val themeColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(1000), label = "color")

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft == 0L && isRunning) {
            isRunning = false
            playNotificationSound(context)
            sendNotification(context, "Session Complete", "$currentMode finished!")
        }
    }

    val progress = animateFloatAsState(targetValue = timeLeft.toFloat() / totalTime.toFloat(), label = "Progress")
    val minutes = (timeLeft / 60).toString().padStart(2, '0')
    val seconds = (timeLeft % 60).toString().padStart(2, '0')

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Focus Timer", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.padding(bottom = 40.dp)) {
                StatText("0/4", "Rounds")
                StatText("0/15", "Goals")
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(280.dp)) {
                    drawCircle(color = Color.White.copy(alpha = 0.05f), style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round))
                }
                Canvas(modifier = Modifier.size(280.dp)) {
                    drawArc(color = themeColor, startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false, style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if(currentMode == "Start Time") Icons.Rounded.SelfImprovement else Icons.Rounded.Coffee, null, tint = themeColor.copy(alpha = 0.8f), modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$minutes:$seconds", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                    Text(if(isRunning) "Running..." else "Paused", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                IconButton(
                    onClick = { isRunning = false; timeLeft = totalTime },
                    modifier = Modifier.size(50.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White) }

                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(60.dp).width(140.dp)
                ) {
                    Icon(if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 110.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimerModeButton("Start Time", currentMode == "Start Time") {
                currentMode = "Start Time"; isRunning = false; totalTime = focusTime; timeLeft = focusTime
            }
            TimerModeButton("Break Time", currentMode == "Break Time") {
                currentMode = "Break Time"; isRunning = false; totalTime = shortBreakTime; timeLeft = shortBreakTime
            }
        }
    }
}

@Composable
fun RowScope.TimerModeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).height(45.dp).clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun StatText(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

// SAFE ACCESS: System Calls
fun playNotificationSound(context: Context) {
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) {
        // Ignore sound failures in preview
    }
}

fun sendNotification(context: Context, title: String, message: String) {
    try {
        val channelId = "pomodoro_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pomodoro Timer", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1, notification)
    } catch (e: Exception) {
        // Ignore notification failures in preview
    }
}