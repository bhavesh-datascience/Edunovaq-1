package com.example.eduu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.net.URL
import org.jitsi.meet.sdk.JitsiMeet

// ==========================================
// Main Controller for Section 4
// ==========================================
@Composable
fun StudyMeetsScreen(userEmail: String, userName: String, onToggleNavBar: (Boolean) -> Unit) {
    // 0 = Dashboard, 1 = Friend Group, 2 = Global Study, 3 = Ephemeral Chat
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
                onNavigateGlobal = { currentView = 2 },
                onNavigateChat = { currentView = 3 }
            )
            1 -> FriendGroupCallUI(
                onBack = { currentView = 0 },
                userEmail = userEmail,
                userName = userName
            )
            2 -> GlobalStudyUI(
                onBack = { currentView = 0 },
                userEmail = userEmail,
                userName = userName
            )
            3 -> EphemeralChatUI(
                onBack = { currentView = 0 },
                userEmail = userEmail,
                onToggleNavBar = onToggleNavBar
            )
        }
    }
}

// ==========================================
// 1. Dashboard (The Selection Menu)
// ==========================================
@Composable
fun StudyMeetsDashboard(onNavigateFriends: () -> Unit, onNavigateGlobal: () -> Unit, onNavigateChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Study Together", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Rounded.Groups, null, tint = Color(0xFF4DB6AC), modifier = Modifier.size(32.dp))
        }

        Text("Connect with your squad or find a study buddy.", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        // --- Option 1: Friend Group Call ---
        GlassCard(onClick = onNavigateFriends, modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape))
                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    Text("Friend Group Call", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Private video rooms & screen share", color = Color.Gray, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.VideoCall, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Option 2: Study with Others ---
        GlassCard(onClick = onNavigateGlobal, modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color(0xFF00BFA5).copy(alpha = 0.3f), CircleShape))
                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    Text("Online Public Rooms", color = Color(0xFF00BFA5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Study with Others", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Join silent global libraries", color = Color.Gray, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.Public, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Option 3: Chat to Friend ---
        GlassCard(onClick = onNavigateChat, modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color(0xFFEC4899).copy(alpha = 0.3f), CircleShape))
                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    Text("View-Once Messages", color = Color(0xFFEC4899), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chat to Friend", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Distraction-free, disappears on close", color = Color.Gray, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(48.dp))
            }
        }
    }
}

// ==========================================
// 2. Friend Group UI
// ==========================================
@Composable
fun FriendGroupCallUI(onBack: () -> Unit, userEmail: String, userName: String) {
    val context = LocalContext.current
    var roomCode by remember { mutableStateOf("") }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Friend Groups", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("JOIN EXISTING", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it },
                placeholder = { Text("Room code (e.g. abc-def)", color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha = 0.05f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true
            )

            Button(
                onClick = {
                    if (roomCode.isNotBlank()) {
                        val clip = ClipData.newPlainText("Room Code", roomCode)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                        launchJitsiMeet(context, "Edunovaq-Private-$roomCode", false, userName, userEmail)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(56.dp)
            ) { Text("Join") }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("OR CREATE NEW", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth().height(160.dp), onClick = {
            val chars = ('a'..'z')
            val r = (1..6).map { chars.random() }.joinToString("")
            val newCode = "${r.substring(0, 3)}-${r.substring(3, 6)}"
            roomCode = newCode

            val clip = ClipData.newPlainText("Room Code", newCode)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()

            launchJitsiMeet(context, "Edunovaq-Private-$newCode", false, userName, userEmail)
        }) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(56.dp).background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.VideoCall, null, tint = Color(0xFF818CF8), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Start New Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Generates a code & copies it", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// 3. Global Study UI
// ==========================================
@Composable
fun GlobalStudyUI(onBack: () -> Unit, userEmail: String, userName: String) {
    val context = LocalContext.current
    val roomNames = listOf("Coding", "Maths", "Science")

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Global Library", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(3) { index ->
                val name = roomNames[index]
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {
                    val safeRoomName = "Edunovaq-Global-${name}"
                    launchJitsiMeet(context, safeRoomName, true, userName, userEmail)
                }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF26A69A), Color(0xFF00695C)))), contentAlignment = Alignment.Center) {
                            val icon = when (name) { "Coding" -> Icons.Rounded.Code; "Maths" -> Icons.Rounded.Calculate; else -> Icons.Rounded.Science }
                            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "$name Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Rounded.MicOff, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Silent Focus Area", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Ephemeral Chat UI (No Database Storage)
// ==========================================
data class ChatMessage(val text: String, val isMine: Boolean)

@Composable
fun EphemeralChatUI(onBack: () -> Unit, userEmail: String, onToggleNavBar: (Boolean) -> Unit) {
    var friendEmail by remember { mutableStateOf("") }
    var isChatActive by remember { mutableStateOf(false) }

    // Auto-hide the navigation bar when this screen opens
    LaunchedEffect(Unit) {
        onToggleNavBar(false)
    }

    // Restore the navigation bar when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            onToggleNavBar(true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f)).padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Column {
                Text(if (isChatActive) friendEmail else "Connect with Friend", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (isChatActive) {
                    Text("Ephemeral Mode - Messages disappear on exit", color = Color(0xFFEC4899), fontSize = 12.sp)
                }
            }
        }

        if (!isChatActive) {
            // Setup Screen
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LockClock, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("View-Once Chat", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Messages are not stored anywhere and will be deleted permanently when you leave this screen.", color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

                OutlinedTextField(
                    value = friendEmail,
                    onValueChange = { friendEmail = it },
                    placeholder = { Text("Enter friend's email", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha = 0.05f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { if (friendEmail.isNotBlank()) isChatActive = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Distraction-Free Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Active Chat Screen
            var messageText by remember { mutableStateOf("") }
            val messages = remember { mutableStateListOf<ChatMessage>() }
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            Column(modifier = Modifier.fillMaxSize()) {
                // Chat List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
                        val backgroundColor = if (message.isMine) Color(0xFF6366F1) else Color(0xFF334155)
                        val shape = if (message.isMine) {
                            RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
                        } else {
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
                        }

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                            Box(modifier = Modifier.background(backgroundColor, shape).padding(12.dp).widthIn(max = 280.dp)) {
                                Text(text = message.text, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0F172A)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF6366F1)).clickable {
                            if (messageText.isNotBlank()) {
                                messages.add(ChatMessage(messageText, isMine = true))
                                // Note: Simulating a received message for demonstration.
                                // Replace with FCM / Peer-to-Peer trigger in production.
                                if (messageText.lowercase() == "ping") {
                                    messages.add(ChatMessage("Pong! I am studying right now.", isMine = false))
                                }
                                messageText = ""
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp).padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// Jitsi Meet Configuration Helper
// ==========================================
private fun launchJitsiMeet(context: Context, roomName: String, isStudyWithOthers: Boolean, userName: String, userEmail: String) {
    try {
        val serverURL = URL("https://meet.ffmuc.net")
        val safeRoomName = roomName.replace("-", "").replace(" ", "")

        val defaultOptions = JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            .setFeatureFlag("welcomepage.enabled", false)
            .build()
        JitsiMeet.setDefaultConferenceOptions(defaultOptions)

        val userInfo = JitsiMeetUserInfo().apply {
            displayName = if (userName.isNotBlank()) userName else "Edunovaq Student"
            email = userEmail
        }

        val optionsBuilder = JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            .setRoom(safeRoomName)
            .setUserInfo(userInfo)
            .setVideoMuted(false)
            .setFeatureFlag("prejoinpage.enabled", true)
            .setFeatureFlag("meeting-name.enabled", true)
            .setFeatureFlag("security-options.enabled", false)

        if (isStudyWithOthers) {
            optionsBuilder.setAudioMuted(true)
        } else {
            optionsBuilder.setAudioMuted(false)
        }

        JitsiMeetActivity.launch(context, optionsBuilder.build())
    } catch (e: Exception) {
        Toast.makeText(context, "Connection Error. Try again.", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Box(content = content)
    }
}