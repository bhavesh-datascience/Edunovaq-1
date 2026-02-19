package com.example.eduu

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ==========================================
// Data Models
// ==========================================
data class BlockableApp(
    val id: Long,
    val name: String,
    val icon: ImageVector,
    var isBlocked: Boolean
)

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isFolder: Boolean,
    val parentId: String? = null,
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 1. Main Tools Controller
// ==========================================
@Composable
fun ToolsScreen(
    onToggleNavBar: (Boolean) -> Unit = {},
    sharedUri: Uri? = null,
    onSharedUriHandled: () -> Unit = {}
) {
    // 0 = Menu, 1 = Pomodoro, 2 = Scroll Block, 3 = Notes Organizer
    var activeTool by remember { mutableIntStateOf(0) }

    // Auto-hide Navigation Bar
    LaunchedEffect(activeTool) {
        onToggleNavBar(activeTool == 0)
    }

    // NEW: If a shared file arrives, instantly open the Notes Organizer tab
    LaunchedEffect(sharedUri) {
        if (sharedUri != null) {
            activeTool = 3
        }
    }

    // Intercept hardware back button to return to the tools menu
    BackHandler(enabled = activeTool != 0) {
        activeTool = 0
    }

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
            3 -> NotesOrganizerScreen(
                onBack = { activeTool = 0 },
                sharedUri = sharedUri,
                onSharedUriHandled = onSharedUriHandled
            )
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

        ToolCard(
            icon = Icons.Rounded.Timer,
            title = "Pomodoro Focus",
            desc = "Stay focused with the classic technique.",
            color = Color(0xFF6366F1),
            onClick = { onNavigate(1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        ToolCard(
            icon = Icons.Rounded.Block,
            title = "Scroll Block",
            desc = "Block distracting apps & Focus.",
            color = Color(0xFFF43F5E),
            onClick = { onNavigate(2) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        ToolCard(
            icon = Icons.Rounded.FolderSpecial,
            title = "Notes Organizer",
            desc = "Save and arrange your class notes.",
            color = Color(0xFF0EA5E9),
            onClick = { onNavigate(3) }
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
// 3. Scroll Block Screen
// ==========================================
@Composable
fun ScrollBlockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val prefs = remember {
        if (!isPreview) {
            try { context.getSharedPreferences("study_tools_prefs", Context.MODE_PRIVATE) }
            catch (e: Exception) { null }
        } else null
    }

    var isServiceEnabled by remember {
        mutableStateOf(
            if (isPreview) false
            else isAccessibilityServiceEnabled(context, "com.example.eduu.BlockerService")
        )
    }

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

    LaunchedEffect(isFocusMode, apps.toList()) {
        if (prefs != null) {
            try {
                prefs.edit().putBoolean("is_focus_active", isFocusMode).apply()
                val blockedNames = apps.filter { it.isBlocked }.map { it.name }.toSet()
                prefs.edit().putStringSet("blocked_packages", blockedNames).apply()
            } catch (e: Exception) {}
        }

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

            Surface(
                color = if(isServiceEnabled) Color(0xFF00C853).copy(alpha = 0.1f) else Color(0xFFFFAB00).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if(isServiceEnabled) Color(0xFF00C853).copy(alpha = 0.3f) else Color(0xFFFFAB00).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable {
                        if (!isServiceEnabled && !isPreview) {
                            try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                            catch (e: Exception) {}
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
                            Text("Required to close other apps", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

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
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        if (showAddDialog) {
            AddAppDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name ->
                    apps.add(BlockableApp(System.currentTimeMillis(), name, Icons.Rounded.Android, true))
                    showAddDialog = false
                }
            )
        }

        AnimatedVisibility(
            visible = isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FocusModeOverlay(quote = currentQuote, seconds = timerSeconds, onStop = { isFocusMode = false })
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
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
        return false
    }
}

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
// 4. Pomodoro Focus Screen
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

fun playNotificationSound(context: Context) {
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) {}
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
    } catch (e: Exception) {}
}

// ==========================================
// 5. Notes Organizer Screen (Smart Local Storage)
// ==========================================
@Composable
fun NotesOrganizerScreen(
    onBack: () -> Unit,
    sharedUri: Uri? = null,
    onSharedUriHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val allNotes = remember { mutableStateListOf<NoteItem>() }

    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var folderBreadcrumbs by remember { mutableStateOf(listOf<Pair<String?, String>>(Pair(null, "My Notes"))) }

    var showAddFolderDialog by remember { mutableStateOf(false) }

    // Auto-load saved notes
    LaunchedEffect(Unit) {
        val savedNotes = loadNotesFromPrefs(context)
        allNotes.clear()
        allNotes.addAll(savedNotes)
    }

    // Auto-save notes whenever the list changes
    LaunchedEffect(allNotes.toList()) {
        saveNotesToPrefs(context, allNotes.toList())
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileName = getFileName(context, selectedUri) ?: "Untitled_Document.pdf"
            val localPath = copyFileToInternalStorage(context, selectedUri, fileName)

            if (localPath != null) {
                allNotes.add(
                    NoteItem(
                        name = fileName,
                        isFolder = false,
                        parentId = currentFolderId,
                        localFilePath = localPath
                    )
                )
            }
        }
    }

    val currentItems = allNotes.filter { it.parentId == currentFolderId }.sortedByDescending { it.isFolder }

    BackHandler {
        if (currentFolderId == null) {
            onBack()
        } else {
            val newBreadcrumbs = folderBreadcrumbs.dropLast(1)
            folderBreadcrumbs = newBreadcrumbs
            currentFolderId = newBreadcrumbs.last().first
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentFolderId == null) onBack()
                    else {
                        val newBreadcrumbs = folderBreadcrumbs.dropLast(1)
                        folderBreadcrumbs = newBreadcrumbs
                        currentFolderId = newBreadcrumbs.last().first
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))

                val breadcrumbText = folderBreadcrumbs.joinToString(" > ") { it.second }
                Text(
                    text = breadcrumbText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (currentItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("This folder is empty", color = Color.Gray, fontSize = 16.sp)
                        Text("Add a sub-folder or import a note", color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentItems) { item ->
                        NoteGridItem(item) {
                            if (item.isFolder) {
                                currentFolderId = item.id
                                folderBreadcrumbs = folderBreadcrumbs + Pair(item.id, item.name)
                            } else {
                                // Trigger the file opener intent
                                openFile(context, item.localFilePath)
                            }
                        }
                    }
                }
            }
        }

        // --- NEW: FLOATING "SAVE SHARED FILE" BANNER ---
        if (sharedUri != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 110.dp) // Hovers just above standard buttons
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable {
                            val fileName = getFileName(context, sharedUri) ?: "Shared_Note_${System.currentTimeMillis()}"
                            val localPath = copyFileToInternalStorage(context, sharedUri, fileName)

                            if (localPath != null) {
                                allNotes.add(
                                    NoteItem(name = fileName, isFolder = false, parentId = currentFolderId, localFilePath = localPath)
                                )
                                Toast.makeText(context, "Saved to current folder!", Toast.LENGTH_SHORT).show()
                                onSharedUriHandled() // Clear the state
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF10B981), // Emerald Green
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Save Shared File Here", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Standard Bottom Navigation for Notes Screen
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp).clickable { showAddFolderDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B262C).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.CreateNewFolder, null, tint = Color(0xFF0EA5E9))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Folder", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).height(56.dp).clickable {
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain"))
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0EA5E9)))?.let { Color.Transparent } ?: Color(0xFF0EA5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0EA5E9))))) {
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.UploadFile, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Note", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showAddFolderDialog) {
            AddFolderDialog(
                onDismiss = { showAddFolderDialog = false },
                onAdd = { folderName ->
                    allNotes.add(NoteItem(name = folderName, isFolder = true, parentId = currentFolderId))
                    showAddFolderDialog = false
                }
            )
        }
    }
}

@Composable
fun NoteGridItem(item: NoteItem, onClick: () -> Unit) {
    val bgColor = if (item.isFolder) Color(0xFF0EA5E9).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
    val iconColor = if (item.isFolder) Color(0xFF38BDF8) else Color(0xFFF87171)
    val icon = if (item.isFolder) Icons.Rounded.Folder else Icons.Rounded.PictureAsPdf

    Box(
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(bgColor).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AddFolderDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1E1E2E), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Create New Folder", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, label = { Text("Subject or Topic Name", color = Color.Gray) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0EA5E9), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (text.isNotBlank()) onAdd(text) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) { Text("Create", color = Color.White) }
                }
            }
        }
    }
}

// --- Helper Functions for File Operations & SharedPreferences ---

fun saveNotesToPrefs(context: Context, notes: List<NoteItem>) {
    val prefs = context.getSharedPreferences("edunovaq_notes_prefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(notes)
    prefs.edit().putString("saved_notes_data", json).apply()
}

fun loadNotesFromPrefs(context: Context): List<NoteItem> {
    val prefs = context.getSharedPreferences("edunovaq_notes_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("saved_notes_data", null) ?: return emptyList()

    val type = object : TypeToken<List<NoteItem>>() {}.type
    return try {
        Gson().fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}

fun copyFileToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val notesDir = File(context.filesDir, "edunovaq_notes")
        if (!notesDir.exists()) notesDir.mkdirs()

        val destFile = File(notesDir, "${System.currentTimeMillis()}_$fileName")
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(destFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Generates a secure URI using FileProvider and opens it with compatible apps
fun openFile(context: Context, filePath: String?) {
    if (filePath == null) return
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Must match the authority declared in AndroidManifest.xml
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open Note with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}