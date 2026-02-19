package com.example.eduu

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import com.google.ai.client.generativeai.GenerativeModel
import com.example.eduu.BuildConfig
// ==========================================
// 1. DATA MODELS & VIEW MODEL
// ==========================================
data class AIGeneratedTask(
    val title: String, val startTimeStr: String, val endTimeStr: String, val type: String
)

enum class AIState { INPUT, LOADING, REVIEW, SUCCESS }



// ... (keep your data models here) ...

class AIScheduleViewModel : ViewModel() {
    var uiState by mutableStateOf(AIState.INPUT)
        private set

    var generatedSchedule = mutableStateListOf<AIGeneratedTask>()
        private set

    // Initialize Gemini
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun generateScheduleFromAI(prompt: String) {
        if (prompt.isBlank()) return
        uiState = AIState.LOADING

        viewModelScope.launch {
            try {
                // 1. Give Gemini strict instructions to act as a planner and return JSON
                val systemPrompt = """
                    You are an expert student productivity coach.
                    Based on the user's request, create a smart, balanced daily schedule.
                    Include appropriate breaks (e.g., Pomodoro technique).
                    
                    Return ONLY a valid JSON array of objects. Do not use markdown blocks like ```json. Just the raw array.
                    Each object must have exactly these keys:
                    - "title": (String) The specific task name
                    - "startTimeStr": (String) in exactly "hh:mm a" format (e.g., "09:00 AM")
                    - "endTimeStr": (String) in exactly "hh:mm a" format (e.g., "10:30 AM")
                    - "type": (String) Must be exactly "Study", "Break", or "Task"
                    
                    User Request: $prompt
                """.trimIndent()

                // 2. Call Gemini API
                val response = generativeModel.generateContent(systemPrompt)
                var responseText = response.text?.trim() ?: "[]"

                // 3. Clean up the response in case Gemini adds markdown formatting
                responseText = responseText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                // 4. Parse the JSON into our Kotlin Objects
                val jsonArray = JSONArray(responseText)
                val newList = mutableListOf<AIGeneratedTask>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    newList.add(
                        AIGeneratedTask(
                            title = obj.getString("title"),
                            startTimeStr = obj.getString("startTimeStr"),
                            endTimeStr = obj.getString("endTimeStr"),
                            type = obj.getString("type")
                        )
                    )
                }

                // 5. Update the UI
                generatedSchedule.clear()
                generatedSchedule.addAll(newList)
                uiState = AIState.REVIEW

            } catch (e: Exception) {
                e.printStackTrace()
                // If the AI fails or returns bad JSON, reset so the user can try again
                uiState = AIState.INPUT
            }
        }
    }

    fun acceptAndSaveSchedule(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val today = Calendar.getInstance()
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())

        generatedSchedule.forEach { aiTask ->
            try {
                val startCal = Calendar.getInstance().apply { time = format.parse(aiTask.startTimeStr)!! }
                val endCal = Calendar.getInstance().apply { time = format.parse(aiTask.endTimeStr)!! }

                val finalStart = today.clone() as Calendar
                finalStart.set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY))
                finalStart.set(Calendar.MINUTE, startCal.get(Calendar.MINUTE))
                finalStart.set(Calendar.SECOND, 0)

                val finalEnd = today.clone() as Calendar
                finalEnd.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY))
                finalEnd.set(Calendar.MINUTE, endCal.get(Calendar.MINUTE))
                finalEnd.set(Calendar.SECOND, 0)

                val color = when(aiTask.type) {
                    "Break" -> "#10B981"
                    "Study" -> "#8B5CF6"
                    else -> "#3B82F6"
                }

                val newEvent = CalendarEvent(
                    title = aiTask.title,
                    description = "AI Generated",
                    dateMillis = finalStart.timeInMillis,
                    endTimeMillis = finalEnd.timeInMillis,
                    colorHex = color,
                    reminders = listOf(5L)
                )

                val docRef = db.collection("users").document(uid).collection("events").document()
                docRef.set(newEvent.copy(id = docRef.id))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        uiState = AIState.SUCCESS
        // Toast message handled correctly via context passed from Compose
    }

    fun reset() {
        uiState = AIState.INPUT
        generatedSchedule.clear()
    }
}
// ==========================================
// 2. UI SCREEN (RENAMED to avoid conflict)
// ==========================================
@Composable
fun AIScheduleMakerScreen(
    onBack: () -> Unit, // Added so user can go back to the main AI page
    viewModel: AIScheduleViewModel = viewModel(),
    onToggleNavBar: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 20.dp, end = 20.dp)) {

        // --- HEADER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Edunovaq AI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Smart Schedule Maker", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- ANIMATED CONTENT ---
        AnimatedContent(targetState = viewModel.uiState, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "AI_State") { state ->
            when (state) {
                AIState.INPUT -> AIInputView(onGenerate = { viewModel.generateScheduleFromAI(it) })
                AIState.LOADING -> AILoadingView()
                AIState.REVIEW -> AIReviewView(
                    schedule = viewModel.generatedSchedule,
                    onAccept = { viewModel.acceptAndSaveSchedule(context) },
                    onReject = { viewModel.reset() }
                )
                AIState.SUCCESS -> AISuccessView(onDone = { viewModel.reset() })
            }
        }
    }
}

// ... SUB-VIEWS (Input, Loading, Review, Success) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInputView(onGenerate: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("What's on your plate today?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Tell me your tasks, assignments, and how much time you have. I'll build the perfect routine.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("e.g., I need to study 2 hours of Physics, finish my English essay, and I have basketball practice at 5 PM.", color = Color.Gray.copy(0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1), unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color(0xFF6366F1), focusedContainerColor = Color.White.copy(0.02f), unfocusedContainerColor = Color.White.copy(0.02f)),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onGenerate(prompt) }, modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(), shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Magic Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun AILoadingView() {
    Column(modifier = Modifier.fillMaxWidth().height(300.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = Color(0xFFEC4899), strokeWidth = 4.dp, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(24.dp))
        Text("AI is analyzing your tasks...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("Optimizing for focus and breaks.", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun AIReviewView(schedule: List<AIGeneratedTask>, onAccept: () -> Unit, onReject: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Here is your optimized routine:", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().weight(1f), color = Color.White.copy(0.03f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f))) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
                schedule.forEachIndexed { index, task ->
                    AITableRow(task)
                    if (index < schedule.size - 1) { HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(vertical = 12.dp)) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(55.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(16.dp)) { Text("Discard") }
            Button(onClick = onAccept, modifier = Modifier.weight(1f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80)), shape = RoundedCornerShape(16.dp)) { Text("Accept & Save", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun AITableRow(task: AIGeneratedTask) {
    val color = when(task.type) { "Break" -> Color(0xFF10B981); "Study" -> Color(0xFF8B5CF6); else -> Color(0xFF3B82F6) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.width(80.dp), horizontalAlignment = Alignment.End) {
            Text(task.startTimeStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(task.endTimeStr, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.width(4.dp).height(40.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(task.type, color = color.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AISuccessView(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().height(400.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF4ADE80).copy(0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Schedule Applied!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Your events are now synced with your calendar.", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)), shape = RoundedCornerShape(50.dp)) {
            Text("Create Another", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
    }
}