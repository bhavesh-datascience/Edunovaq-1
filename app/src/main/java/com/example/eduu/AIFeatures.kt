package com.example.eduu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Ensure you are NOT importing com.example.eduu.shared or similar
import com.example.eduu.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import kotlinx.coroutines.launch
import java.util.Locale

// ==========================================
// 1. MAIN AI SCREEN CONTROLLER
// ==========================================
@Composable
fun AIScreen(onToggleNavBar: (Boolean) -> Unit = {}) {
    var activeScreen by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (activeScreen) {
            0 -> AIMenu(onNavigate = { activeScreen = it })
            1 -> GeminiChatScreen(
                onBack = { activeScreen = 0 },
                onToggleNavBar = onToggleNavBar
            )
            2 -> PDFChatScreen(
                onBack = { activeScreen = 0 },
                onToggleNavBar = onToggleNavBar
            )
            3 -> YouTubeSummarizerScreen(onBack = { activeScreen = 0 })
        }
    }
}

// ==========================================
// 2. AI MENU DASHBOARD
// ==========================================
@Composable
fun AIMenu(onNavigate: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("AI Assistant", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Powered by Google Gemini", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // Feature 1: General Chat
        AIFeatureCard(
            icon = Icons.Rounded.AutoAwesome,
            title = "Ask AI",
            desc = "Voice-enabled chat with Gemini.",
            color = Color(0xFF00C853),
            onClick = { onNavigate(1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Feature 2: PDF Chat
        AIFeatureCard(
            icon = Icons.Rounded.PictureAsPdf,
            title = "Chat with PDF",
            desc = "Upload documents and ask questions.",
            color = Color(0xFFF43F5E),
            onClick = { onNavigate(2) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Feature 3: YouTube (Placeholder)
        AIFeatureCard(
            icon = Icons.Rounded.PlayCircle,
            title = "YouTube Summarizer",
            desc = "Get instant summaries of study videos.",
            color = Color(0xFF6366F1),
            onClick = { onNavigate(3) }
        )
    }
}

// ==========================================
// 3. GEMINI CHAT SCREEN (General)
// ==========================================
@Composable
fun GeminiChatScreen(onBack: () -> Unit, onToggleNavBar: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onToggleNavBar(false)
        onDispose { onToggleNavBar(true) }
    }
    BackHandler { onBack() }

    // INITIALIZE GEMINI WITH SAFETY SETTINGS
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey =   "AIzaSyBYT3fd-DE4KC2NoFbjXN4_ORIPh99zNDc",
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
            )
        )
    }
    val chatSession = remember { generativeModel.startChat() }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.US }
        tts = ttsInstance
        onDispose { ttsInstance.shutdown() }
    }

    fun sendMessage(forcedMessage: String? = null) {
        val userMsg = forcedMessage ?: message
        if (userMsg.isBlank()) return

        chatHistory = chatHistory + ChatMessage(userMsg, true)
        message = ""
        isLoading = true

        scope.launch {
            try {
                val response = chatSession.sendMessage(userMsg)
                val responseText = response.text ?: "I couldn't generate a response."
                chatHistory = chatHistory + ChatMessage(responseText, false)
                tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: Exception) {
                chatHistory = chatHistory + ChatMessage("Error: ${e.localizedMessage}", false)
            } finally {
                isLoading = false
            }
        }
    }

    // STT Logic
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) } }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(e: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) sendMessage(matches[0])
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(e: Int, p: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    ChatInterface(
        title = "Gemini Chat",
        chatHistory = chatHistory,
        message = message,
        isLoading = isLoading,
        isListening = isListening,
        onBack = onBack,
        onMessageChange = { message = it },
        onSend = { sendMessage() },
        onMicClick = { if (isListening) speechRecognizer.stopListening() else speechRecognizer.startListening(speechIntent) }
    )
}

// ==========================================
// 4. PDF CHAT SCREEN (Improved & Robust)
// ==========================================
@Composable
fun PDFChatScreen(onBack: () -> Unit, onToggleNavBar: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfText by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onToggleNavBar(false)
        onDispose { onToggleNavBar(true) }
    }
    BackHandler { onBack() }

    // INITIALIZE GEMINI WITH SAFETY SETTINGS
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
            )
        )
    }
    // We create the chat session *once*
    val chatSession = remember { generativeModel.startChat() }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.US }
        tts = ttsInstance
        onDispose { ttsInstance.shutdown() }
    }

    fun sendMessage(forcedMessage: String? = null) {
        val userMsg = forcedMessage ?: message
        if (userMsg.isBlank()) return

        chatHistory = chatHistory + ChatMessage(userMsg, true)
        message = ""
        isLoading = true

        scope.launch {
            try {
                val response = chatSession.sendMessage(userMsg)
                val responseText = response.text ?: "I couldn't generate a response."
                chatHistory = chatHistory + ChatMessage(responseText, false)
                tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null)
            }catch (e: Exception) {
                Log.e("GeminiError", "Connection Check Failed", e)
                // THIS WILL SHOW THE REAL ERROR ON YOUR SCREEN
                Toast.makeText(context, "Debug Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                isLoading = false
                return@launch

            } finally {
                isLoading = false
            }
        }
    }

    // STT Logic (Reused)
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) } }
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(e: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) sendMessage(matches[0])
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(e: Int, p: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    // FILE PICKER & PROCESSING
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    // 1. EXTRACT TEXT
                    val text = extractTextFromPdf(context, it)

                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, "PDF text is empty or unreadable.", Toast.LENGTH_LONG).show()
                        isLoading = false
                        return@launch
                    }

                    // 2. CHECK CONNECTION (Ping)
                    // We send a tiny message first to check if Key/Network/Model is working
                    try {
                        chatSession.sendMessage("Ping")
                    } catch (e: Exception) {
                        Log.e("GeminiError", "Connection Check Failed", e)
                        Toast.makeText(context, "AI Connection Failed. Check Internet & API Key.", Toast.LENGTH_LONG).show()
                        isLoading = false
                        return@launch
                    }

                    // 3. SEND PDF CONTENT
                    // Send the actual large text
                    val response = chatSession.sendMessage("I am uploading a document context. Please verify you have received it. Do not summarize yet, just say 'Document Received'.\n\nDOCUMENT CONTENT:\n$text")

                    pdfText = text
                    fileName = "Document Ready"
                    chatHistory = listOf(ChatMessage(response.text ?: "Document Loaded. Ask me anything!", false))

                } catch (e: Exception) {
                    e.printStackTrace()
                    val msg = e.localizedMessage ?: "Unknown Error"
                    if (msg.contains("safety")) {
                        Toast.makeText(context, "Content blocked by Safety Filters.", Toast.LENGTH_LONG).show()
                    } else if (msg.contains("503") || msg.contains("500")) {
                        Toast.makeText(context, "Server Busy. Try a smaller PDF.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (pdfText == null) {
        // Upload Screen
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chat with PDF", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(30.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFFF43F5E))
                    Spacer(Modifier.height(16.dp))
                    Text("Processing Document...", color = Color.Gray)
                    Text("This may take a moment", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Button(
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload PDF")
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, colors = ButtonDefaults.textButtonColors()) { Text("Back", color = Color.Gray) }
            }
        }
    } else {
        // Chat Screen
        ChatInterface(
            title = if (fileName.isNotEmpty()) fileName else "PDF Chat",
            chatHistory = chatHistory,
            message = message,
            isLoading = isLoading,
            isListening = isListening,
            onBack = onBack,
            onMessageChange = { message = it },
            onSend = { sendMessage() },
            onMicClick = { if (isListening) speechRecognizer.stopListening() else speechRecognizer.startListening(speechIntent) },
            isPdfMode = true
        )
    }
}

// ==========================================
// 5. YOUTUBE (Placeholder)
// ==========================================
@Composable
fun YouTubeSummarizerScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("YouTube Summarizer", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Coming Soon", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

// ==========================================
// 6. SHARED UI COMPONENTS
// ==========================================

@Composable
fun ChatInterface(
    title: String,
    chatHistory: List<ChatMessage>,
    message: String,
    isLoading: Boolean,
    isListening: Boolean,
    onBack: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isPdfMode: Boolean = false
) {
    val themeColor = if (isPdfMode) Color(0xFFF43F5E) else Color(0xFF00C853)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        val listState = rememberLazyListState()
        LaunchedEffect(chatHistory.size) { if (chatHistory.isNotEmpty()) listState.animateScrollToItem(chatHistory.lastIndex) }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(if (isPdfMode) "Ask about the document..." else "Tap the mic to speak...", color = Color.Gray)
                    }
                }
            }
            items(chatHistory) { msg -> ChatBubble(msg, themeColor) }
        }

        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = themeColor)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFFF43F5E) else themeColor)
            ) {
                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                GlassTextField(message, onMessageChange, "Type message...", Icons.AutoMirrored.Filled.Send, themeColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.2f)),
                shape = CircleShape,
                modifier = Modifier.size(50.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = themeColor)
            }
        }
    }
}

@Composable
fun AIFeatureCard(icon: ImageVector, title: String, desc: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatBubble(message: ChatMessage, themeColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(themeColor), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ))
                .background(if (message.isUser) Color(0xFF6366F1) else Color.White.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Text(message.text, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    activeColor: Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Send),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedContainerColor = Color.Black.copy(0.2f),
            unfocusedContainerColor = Color.Black.copy(0.2f)
        )
    )
}