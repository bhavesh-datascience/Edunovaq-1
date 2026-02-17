package com.example.eduu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==========================================
// 1. Data Model (Shared)
// ==========================================
data class StudentProfile(
    var fullName: String = "",
    var gender: String = "",
    var dob: String = "",
    var mobile: String = "",
    var parentMobile: String = "",
    var email: String = "",
    var city: String = "",
    var state: String = "",
    var standard: String = "",
    var board9_10: String = "",
    var board11_12: String = "", // Also used for University Name
    var stream: String = "",
    var subjects: List<String> = emptyList(),
    var exams: List<String> = emptyList(),
    var language: String = "",
    var profileCompleted: Boolean = true
)

// ==========================================
// 2. ViewModel
// ==========================================
class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(StudentProfile())
    val state = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun updateField(block: StudentProfile.() -> Unit) {
        _state.value = _state.value.copy().apply(block)
    }

    fun submitData(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true
        val s = _state.value

        if (s.fullName.isBlank() || s.mobile.length < 10 || s.standard.isBlank()) {
            _isLoading.value = false
            onError("Please fill Name, Mobile, and Class")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            onError("User not logged in")
            return
        }

        FirebaseFirestore.getInstance().collection("students").document(userId)
            .set(s)
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e.message ?: "Failed to save data")
            }
    }
}

// ==========================================
// 3. Main Screen
// ==========================================
@Composable
fun StudentRegistrationScreen(
    userEmail: String,
    onComplete: () -> Unit
) {
    val viewModel: ProfileViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (state.email.isBlank()) viewModel.updateField { email = userEmail }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Icon(Icons.Rounded.School, null, tint = Color(0xFF6366F1), modifier = Modifier.size(50.dp))
            Text("Complete Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Let's personalize your learning experience", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(30.dp))

            // --- Personal Details ---
            ProfileSectionHeader("Personal Details")
            ProfileTextField(state.fullName, { v -> viewModel.updateField { fullName = v } }, "Full Name", Icons.Default.Person)
            Spacer(modifier = Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    ProfileDropdown(state.gender, listOf("Male", "Female", "Other"), "Gender") { viewModel.updateField { gender = it } }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    ProfileTextField(state.dob, { v -> viewModel.updateField { dob = v } }, "DOB", Icons.Default.CalendarToday)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(state.mobile, { v -> viewModel.updateField { mobile = v } }, "Mobile Number", Icons.Default.Phone, KeyboardType.Phone)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(state.parentMobile, { v -> viewModel.updateField { parentMobile = v } }, "Parent Mobile", Icons.Default.Phone, KeyboardType.Phone)

            Spacer(modifier = Modifier.height(30.dp))

            // --- Academic Details ---
            ProfileSectionHeader("Academic Details")

            // Added "Undergraduate" to the options
            val standardOptions = (1..12).map { it.toString() } + "Undergraduate (Bachelors)"

            ProfileDropdown(state.standard, standardOptions, "Class / Standard") { sel ->
                viewModel.updateField { standard = sel; stream = ""; subjects = emptyList(); board9_10 = ""; board11_12 = "" }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val classInt = state.standard.toIntOrNull() ?: 0
            val isBachelors = state.standard == "Undergraduate (Bachelors)"

            // LOGIC: Class 9-10
            if (classInt in 9..10) {
                ProfileDropdown(state.board9_10, listOf("CBSE", "ICSE", "State Board (SSC)", "Other"), "Board") { viewModel.updateField { board9_10 = it } }
            }

            // LOGIC: Class 11-12
            if (classInt in 11..12) {
                ProfileDropdown(state.board11_12, listOf("CBSE", "ICSE", "State Board (HSC)", "NIOS", "Other"), "Board") { viewModel.updateField { board11_12 = it } }
                Spacer(modifier = Modifier.height(12.dp))
                ProfileDropdown(state.stream, listOf("Science", "Commerce", "Arts"), "Stream") { viewModel.updateField { stream = it; subjects = emptyList() } }

                if (state.stream.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Subjects", color = Color.White, fontSize = 14.sp)
                    val options = when(state.stream) {
                        "Commerce" -> listOf("Accountancy", "Business Studies", "Economics", "Maths", "IP")
                        "Arts" -> listOf("History", "Geography", "Pol Science", "Psychology", "Economics")
                        "Science" -> listOf("Physics", "Chemistry", "Maths", "Biology", "Computer Science")
                        else -> emptyList()
                    }
                    options.forEach { subj ->
                        ProfileCheckbox(subj, state.subjects.contains(subj)) { checked ->
                            val current = state.subjects.toMutableList()
                            if (checked) current.add(subj) else current.remove(subj)
                            viewModel.updateField { subjects = current }
                        }
                    }
                }
            }

            // LOGIC: Bachelors (Undergraduate)
            if (isBachelors) {
                ProfileDropdown(
                    selected = state.board11_12, // Using board11_12 field to store University
                    options = listOf("Mumbai University", "Savitribai Phule Pune University", "Nagpur University", "Shivaji University", "Autonomous / Deemed", "Other"),
                    label = "University / Board"
                ) { viewModel.updateField { board11_12 = it } }

                Spacer(modifier = Modifier.height(12.dp))

                ProfileDropdown(
                    selected = state.stream,
                    options = listOf("B.Sc IT / CS", "B.Sc General", "B.E. / B.Tech", "B.Com", "BMS / BBA", "B.A."),
                    label = "Degree Stream"
                ) { viewModel.updateField { stream = it; subjects = emptyList() } }

                if (state.stream.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Subjects (Major)", color = Color.White, fontSize = 14.sp)

                    val options = when(state.stream) {
                        "B.Sc IT / CS" -> listOf("Java Programming", "Python", "Data Structures", "Database / SQL", "Networking", "Web Dev")
                        "B.Sc General" -> listOf("Physics", "Chemistry", "Maths", "Botany", "Zoology")
                        "B.E. / B.Tech" -> listOf("Engineering Maths", "Physics", "Mechanics", "Basic Electrical", "Programming (C/C++)")
                        "B.Com" -> listOf("Financial Accounting", "Business Economics", "Commerce", "Business Law", "Maths & Stats")
                        "BMS / BBA" -> listOf("Management Principles", "Marketing", "Finance", "HRM", "Business Communication")
                        "B.A." -> listOf("Psychology", "Sociology", "Economics", "Political Science", "History", "English Lit")
                        else -> emptyList()
                    }

                    options.forEach { subj ->
                        ProfileCheckbox(subj, state.subjects.contains(subj)) { checked ->
                            val current = state.subjects.toMutableList()
                            if (checked) current.add(subj) else current.remove(subj)
                            viewModel.updateField { subjects = current }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Preferences ---
            ProfileSectionHeader("Preferences")
            ProfileDropdown(state.language, listOf("English", "Hindi", "Marathi"), "Preferred Language") { viewModel.updateField { language = it } }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Preparing For (Optional)", color = Color.White, fontSize = 14.sp)
            val exams = listOf("JEE", "NEET", "MHT-CET", "GATE", "CAT", "UPSC/MPSC", "Placement")
            exams.forEach { exam ->
                ProfileCheckbox(exam, state.exams.contains(exam)) { checked ->
                    val current = state.exams.toMutableList()
                    if (checked) current.add(exam) else current.remove(exam)
                    viewModel.updateField { this.exams = current }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Submit ---
            Button(
                onClick = {
                    viewModel.submitData(
                        onSuccess = { onComplete() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("SAVE PROFILE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// ==========================================
// 4. Helper Components (Public)
// ==========================================

@Composable
fun ProfileSectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun ProfileTextField(
    value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = Color.White.copy(alpha = 0.7f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6366F1),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdown(selected: String, options: List<String>, label: String, onSelection: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E293B))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = { onSelection(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ProfileCheckbox(label: String, isChecked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChecked(!isChecked) }.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Rounded.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isChecked) Color(0xFF4ADE80) else Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f))
    }
}