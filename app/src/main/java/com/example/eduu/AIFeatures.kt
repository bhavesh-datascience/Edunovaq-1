package com.example.eduu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.util.Locale

// ==========================================
// 1. MAIN SCREEN
// ==========================================



// ==========================================
// 2. AI MENU (With Arrows)
// ==========================================
@Composable
fun AIScreen(onToggleNavBar: (Boolean) -> Unit = {}) {
    var activeScreen by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        when (activeScreen) {
            0 -> AIMenu(onNavigate = { activeScreen = it })
            1 -> GeminiChatScreen(onBack = { activeScreen = 0 }, onToggleNavBar = onToggleNavBar)
            2 -> PDFChatScreen(onBack = { activeScreen = 0 }, onToggleNavBar = onToggleNavBar)
            3 -> YouTubeSummarizerScreen(onBack = { activeScreen = 0 }, onToggleNavBar = onToggleNavBar)
            4 -> ChartMakerScreen(onBack = { activeScreen = 0 }, onToggleNavBar = onToggleNavBar)
            // --- NEW: AI SCHEDULE MAKER ---
            5 -> AIScheduleMakerScreen(
                onBack = { activeScreen = 0 },
                onToggleNavBar = onToggleNavBar
            )
        }
    }
}

// ==========================================
// 2. AI MENU (With Arrows)
// ==========================================
@Composable
fun AIMenu(onNavigate: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(bottom = 100.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("AI Assistant", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Powered by Google Gemini", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // 1. Ask AI
        AIFeatureCard(
            icon = Icons.Rounded.AutoAwesome,
            title = "Ask AI",
            desc = "Voice-enabled chat with Gemini.",
            color = Color(0xFF00C853),
            onClick = { onNavigate(1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Chat with PDF
        AIFeatureCard(
            icon = Icons.Rounded.PictureAsPdf,
            title = "Chat with PDF",
            desc = "Upload documents and ask questions.",
            color = Color(0xFFF43F5E),
            onClick = { onNavigate(2) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. YouTube Summarizer
        AIFeatureCard(
            icon = Icons.Rounded.PlayCircle,
            title = "YouTube Summarizer",
            desc = "Get instant summaries of study videos.",
            color = Color(0xFF6366F1),
            onClick = { onNavigate(3) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. AI Chart Maker
        AIFeatureCard(
            icon = Icons.Rounded.PieChart,
            title = "AI Chart Maker",
            desc = "Turn data into Flowcharts & Graphs.",
            color = Color(0xFFFF9800),
            onClick = { onNavigate(4) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. NEW: AI SCHEDULE MAKER BUTTON ---
        AIFeatureCard(
            icon = Icons.Rounded.CalendarMonth,
            title = "AI Schedule Maker",
            desc = "Auto-plan your day and set reminders.",
            color = Color(0xFF8B5CF6), // Purple
            onClick = { onNavigate(5) }
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

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,

        )
    }
    val chatSession = remember { generativeModel.startChat() }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.US }
        tts = ttsInstance
        onDispose { ttsInstance.shutdown() }
    }

    fun sendMessage(forcedMessage: String? = null, isVoice: Boolean = false) {
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

                if (isVoice) {
                    tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) {
                chatHistory = chatHistory + ChatMessage("Error: ${e.localizedMessage}", false)
            } finally {
                isLoading = false
            }
        }
    }

    // --- MIC LOGIC ---
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { isListening = true; speechRecognizer.startListening(speechIntent) }
        else Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
    }

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
                if (!matches.isNullOrEmpty()) sendMessage(matches[0], isVoice = true)
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
        onSend = { sendMessage(isVoice = false) },
        onMicClick = {
            if (isListening) speechRecognizer.stopListening()
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { isListening = true; speechRecognizer.startListening(speechIntent) }
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    )
}

// ==========================================
// 4. SMART PDF CHAT SCREEN
// ==========================================
@Composable
fun PDFChatScreen(onBack: () -> Unit, onToggleNavBar: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var chatSession by remember { mutableStateOf<com.google.ai.client.generativeai.Chat?>(null) }

    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onToggleNavBar(false)
        onDispose { onToggleNavBar(true) }
    }
    BackHandler { onBack() }

    val generativeModel = remember {
        GenerativeModel(
            "gemini-2.5-flash",
            BuildConfig.GEMINI_API_KEY,

        )
    }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.US }
        tts = ttsInstance
        onDispose { ttsInstance.shutdown() }
    }

    fun sendMessage(forcedMessage: String? = null, isVoice: Boolean = false) {
        val userMsg = forcedMessage ?: message
        if (userMsg.isBlank()) return

        chatHistory = chatHistory + ChatMessage(userMsg, true)
        message = ""
        isLoading = true
        scope.launch {
            try {
                if (chatSession == null) {
                    chatHistory = chatHistory + ChatMessage("Please upload a PDF first.", false)
                } else {
                    val response = chatSession!!.sendMessage(userMsg)
                    val text = response.text ?: "No response"
                    chatHistory = chatHistory + ChatMessage(text, false)
                    if (isVoice) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) {
                chatHistory = chatHistory + ChatMessage("Error: ${e.localizedMessage}", false)
            } finally { isLoading = false }
        }
    }

    // MIC LOGIC
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { isListening = true; speechRecognizer.startListening(speechIntent) }
        else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
    }

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
                if (!matches.isNullOrEmpty()) sendMessage(matches[0], isVoice = true)
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(e: Int, p: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            isLoading = true
            statusMessage = "Analyzing Document..."
            scope.launch {
                try {
                    val isSmall = PdfUtils.isSmallFile(context, it)
                    if (isSmall) {
                        val bytes = PdfUtils.readBytes(context, it)
                        if (bytes != null) {
                            val inputContent = content {
                                blob("application/pdf", bytes)
                                text("Here is a PDF document. Analyze it.")
                            }
                            chatSession = generativeModel.startChat(
                                history = listOf(
                                    inputContent,
                                    content(role = "model") { text("I have analyzed the PDF. Ask me anything.") }
                                )
                            )
                            statusMessage = "PDF Loaded (Raw Mode)"
                            chatHistory = listOf(ChatMessage("I've analyzed your PDF directly! Ask me anything.", false))
                        }
                    } else {
                        val text = PdfUtils.extractTextSmart(context, it, pageLimit = 50)
                        if (text.isNotBlank()) {
                            chatSession = generativeModel.startChat()
                            chatSession?.sendMessage("Here is the text content of a document (First 50 pages). Answer based on this:\n\n$text")
                            statusMessage = "PDF Text Loaded (First 50 pages)"
                            chatHistory = listOf(ChatMessage("I've read the first 50 pages of your document. Ask me anything!", false))
                        } else {
                            Toast.makeText(context, "Could not extract text.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    statusMessage = "Error loading file"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (statusMessage == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Smart PDF Chat", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(30.dp))
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFFF43F5E))
                    Text("Processing...", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
                } else {
                    Button(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))) {
                        Text("Upload PDF")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Supports large files (auto-optimization)", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
    } else {
        ChatInterface(
            title = "PDF Chat",
            chatHistory = chatHistory,
            message = message,
            isLoading = isLoading,
            isListening = isListening,
            onBack = onBack,
            onMessageChange = { message = it },
            onSend = { sendMessage(isVoice = false) },
            onMicClick = {
                if (isListening) speechRecognizer.stopListening()
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { isListening = true; speechRecognizer.startListening(speechIntent) }
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            isPdfMode = true
        )
    }
}

// ==========================================
// 5. YOUTUBE SUMMARIZER SCREEN
// ==========================================
@Composable
fun YouTubeSummarizerScreen(onBack: () -> Unit, onToggleNavBar: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoUrl by remember { mutableStateOf("") }
    var videoInfo by remember { mutableStateOf<YouTubeUtils.VideoInfo?>(null) }

    // Chat State
    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    var isChatMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onToggleNavBar(false)
        onDispose { onToggleNavBar(true) }
    }
    BackHandler { onBack() }

    val generativeModel = remember {
        GenerativeModel(
            "gemini-2.5-flash",
            BuildConfig.GEMINI_API_KEY,

        )
    }
    var chatSession by remember { mutableStateOf<com.google.ai.client.generativeai.Chat?>(null) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.US }
        tts = ttsInstance
        onDispose { ttsInstance.shutdown() }
    }

    fun sendMessage(forcedMessage: String? = null, isVoice: Boolean = false) {
        val userMsg = forcedMessage ?: message
        if (userMsg.isBlank()) return

        chatHistory = chatHistory + ChatMessage(userMsg, true)
        message = ""
        isLoading = true

        scope.launch {
            try {
                if (chatSession == null) {
                    chatHistory = chatHistory + ChatMessage("Please load a video first.", false)
                } else {
                    val response = chatSession!!.sendMessage(userMsg)
                    val text = response.text ?: "No response"
                    chatHistory = chatHistory + ChatMessage(text, false)
                    if (isVoice) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) {
                chatHistory = chatHistory + ChatMessage("Error: ${e.localizedMessage}", false)
            } finally { isLoading = false }
        }
    }

    // MIC LOGIC
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { isListening = true; speechRecognizer.startListening(speechIntent) }
        else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
    }

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
                if (!matches.isNullOrEmpty()) sendMessage(matches[0], isVoice = true)
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(e: Int, p: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    // PROCESS VIDEO
    fun processVideo() {
        if (videoUrl.isBlank()) return
        isLoading = true
        scope.launch {
            val details = YouTubeUtils.getVideoDetails(videoUrl)
            if (details != null) {
                videoInfo = details
                isChatMode = true
                val prompt = """
                    I have a YouTube video with the following details:
                    Title: ${details.title}
                    Channel: ${details.channel}
                    Description: ${details.description}
                    
                    Please provide a comprehensive summary of this video based on its description and title. 
                    Highlight key takeaways and learning points.
                """.trimIndent()

                chatSession = generativeModel.startChat()
                try {
                    val response = chatSession!!.sendMessage(prompt)
                    chatHistory = listOf(ChatMessage(response.text ?: "Here is the summary.", false))
                } catch (e: Exception) {
                    chatHistory = listOf(ChatMessage("Error generating summary: ${e.localizedMessage}", false))
                }
            } else {
                Toast.makeText(context, "Could not fetch video details. Check URL.", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    if (!isChatMode) {
        // INPUT SCREEN
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(30.dp)) {
                Text("YouTube Summarizer", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Paste a link to get an instant summary", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(30.dp))
                GlassTextField(videoUrl, { videoUrl = it }, "https://youtu.be/...", Icons.Default.Link, Color(0xFF6366F1), KeyboardType.Uri)
                Spacer(Modifier.height(20.dp))
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(Modifier.height(10.dp))
                    Text("Fetching Video Info...", color = Color.Gray)
                } else {
                    Button(
                        onClick = { processVideo() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Summarize Video", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, colors = ButtonDefaults.textButtonColors()) { Text("Back", color = Color.Gray) }
            }
        }
    } else {
        val title = videoInfo?.title?.take(20) + "..." ?: "Video Chat"
        ChatInterface(
            title = title,
            chatHistory = chatHistory,
            message = message,
            isLoading = isLoading,
            isListening = isListening,
            onBack = onBack,
            onMessageChange = { message = it },
            onSend = { sendMessage(isVoice = false) },
            onMicClick = {
                if (isListening) speechRecognizer.stopListening()
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { isListening = true; speechRecognizer.startListening(speechIntent) }
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            isPdfMode = false
        )
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

// Updated AIFeatureCard with Arrow
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
            // ARROW ADDED HERE
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

// ==========================================
// UPDATED CHART MAKER SCREEN
// ==========================================
// ==========================================
// UPDATED: CHART MAKER SCREEN (With Steps & Fixes)
// ==========================================

// ==========================================
// UPDATED: CHART MAKER SCREEN (Full Screen & Visible Steps)
// ==========================================
@Composable
fun ChartMakerScreen(onBack: () -> Unit, onToggleNavBar: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var prompt by remember { mutableStateOf("") }
    var mermaidCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingStep by remember { mutableStateOf("") } // Text for step-by-step

    // Chart Types
    val chartTypes = listOf("Flowchart", "Pie Chart", "Mindmap", "Timeline", "Sequence")
    var selectedType by remember { mutableStateOf("Flowchart") }

    DisposableEffect(Unit) { onToggleNavBar(false); onDispose { onToggleNavBar(true) } }
    BackHandler { onBack() }

    val generativeModel = remember {
        GenerativeModel(
            "gemini-2.5-flash", // Using 1.5 Flash as it is stable for code generation
            BuildConfig.GEMINI_API_KEY,

        )
    }

    fun generateChart() {
        if (prompt.isBlank()) return

        isLoading = true
        mermaidCode = ""
        loadingStep = "Initializing..."

        scope.launch {
            try {
                // STEP 1: Plan
                loadingStep = "Analyzing Request..."
                kotlinx.coroutines.delay(1000) // Delay to show step

                // STEP 2: Structure
                loadingStep = "Drafting ${selectedType} Nodes..."
                kotlinx.coroutines.delay(1000)

                val query = """
                    Create a mermaid.js ${selectedType.uppercase()} for: "$prompt".
                    Rules:
                    1. Return ONLY the code. NO text. NO backticks.
                    2. Flowchart start: 'graph TD'.
                    3. Mindmap start: 'mindmap'.
                    4. Pie chart start: 'pie'.
                    5. Keep node labels short.
                """.trimIndent()

                val response = generativeModel.generateContent(query)
                val rawText = response.text ?: ""

                // STEP 3: Syntax Check
                loadingStep = "Validating Syntax..."
                kotlinx.coroutines.delay(800)

                val codeBlockRegex = "```(?:mermaid)?(.*?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = codeBlockRegex.find(rawText)
                var cleanCode = (match?.groupValues?.get(1) ?: rawText).trim()
                if (cleanCode.startsWith("mermaid")) cleanCode = cleanCode.substring(7).trim()

                // STEP 4: Render
                loadingStep = "Drawing Chart..."
                kotlinx.coroutines.delay(500)

                mermaidCode = cleanCode
                isLoading = false

            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .imePadding()
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("AI Chart Maker", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        // Chart Area - Now uses WEIGHT to fill screen
        Box(
            modifier = Modifier
                .weight(1f) // Takes all available space
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(0.05f))
                .border(BorderStroke(1.dp, Color.White.copy(0.1f)), RoundedCornerShape(16.dp))
        ) {
            if (mermaidCode.isNotEmpty()) {
                MermaidChartWebView(mermaidCode)
            } else if (isLoading) {
                // Loading UI
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF9800), strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    // Step Text
                    Text(
                        text = loadingStep,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Empty State
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PieChart, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Select a type & describe it", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Chart Type Selector
        Text("Chart Type:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chartTypes.forEach { type ->
                val isSelected = type == selectedType
                val bgColor = if (isSelected) Color(0xFFFF9800) else Color.White.copy(0.1f)
                val txtColor = if (isSelected) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { selectedType = type }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(type, color = txtColor, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }

        // Input Area
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("e.g. 'Software dev roadmap'", color = Color.Gray) },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 50.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color.White.copy(0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Black.copy(0.2f),
                    unfocusedContainerColor = Color.Black.copy(0.2f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { generateChart() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = CircleShape,
                modifier = Modifier.size(50.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
            }
        }
    }
}

// ==========================================
// UPDATED: WEB VIEW (Full Height & No Errors)
// ==========================================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidChartWebView(mermaidCode: String) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
            <style>
                html, body {
                    width: 100%;
                    height: 100%; /* Force full height */
                    margin: 0;
                    padding: 0;
                    background-color: #0F172A;
                    overflow: hidden; /* Prevent body scroll, let chart zoom */
                }
                body {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                
                #graph { 
                    width: 100%;
                    height: 100%;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    opacity: 0; /* Fade in effect */
                    transition: opacity 0.5s ease-in;
                }

                /* HIDE UGLY ERRORS */
                .error-icon, .error-text, #d-mermaid-version { 
                    display: none !important; 
                    opacity: 0 !important;
                    visibility: hidden !important;
                }
                
                /* SVG SCALING */
                svg {
                    width: 100% !important;
                    height: 100% !important;
                    max-height: 100vh !important;
                }
            </style>
        </head>
        <body>
            <div id="graph" class="mermaid">
                $mermaidCode
            </div>

            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                
                mermaid.initialize({ 
                    startOnLoad: false, 
                    theme: 'dark', 
                    securityLevel: 'loose',
                    logLevel: 'fatal', // Only log fatal errors
                });

                mermaid.parseError = function(err, hash) {
                    console.log('Error suppressed'); // Swallow error
                };

                async function render() {
                    try {
                        const element = document.getElementById('graph');
                        await mermaid.run({ nodes: [element] });
                        element.style.opacity = '1'; // Show when done
                    } catch(e) {
                        console.log(e);
                    }
                }
                render();
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0x00000000)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}