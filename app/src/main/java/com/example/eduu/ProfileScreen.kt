package com.example.eduu

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==========================================
// 1. View Model
// ==========================================
class UserProfileViewModel : ViewModel() {
    private val _profile = MutableStateFlow(StudentProfile())
    val profile = _profile.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init { fetchProfile() }

    fun fetchProfile() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("students").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val p = doc.toObject(StudentProfile::class.java)
                    if (p != null) _profile.value = p
                }
                _isLoading.value = false
            }
            .addOnFailureListener { _isLoading.value = false }
    }

    fun updateField(block: StudentProfile.() -> Unit) {
        _profile.value = _profile.value.copy().apply(block)
    }

    fun saveProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("students").document(uid)
            .set(_profile.value, SetOptions.merge())
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener {
                _isLoading.value = false
                onError(it.message ?: "Update failed")
            }
    }
}

// ==========================================
// 2. Profile Screen UI (Updated with Logout)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit // <--- NEW PARAMETER
) {
    val viewModel: UserProfileViewModel = viewModel()
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF6366F1))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Avatar ---
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.fullName.take(1).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Personal Info ---
                    EditSectionHeader("Personal Details")
                    EditProfileTextField(profile.fullName, { viewModel.updateField { fullName = it } }, "Full Name", Icons.Default.Person)
                    Spacer(modifier = Modifier.height(12.dp))
                    EditProfileTextField(profile.mobile, { viewModel.updateField { mobile = it } }, "Mobile", Icons.Default.Phone, KeyboardType.Phone)
                    Spacer(modifier = Modifier.height(12.dp))
                    EditProfileTextField(profile.city, { viewModel.updateField { city = it } }, "City", Icons.Default.LocationCity)

                    Spacer(modifier = Modifier.height(30.dp))

                    // --- Academic Info ---
                    EditSectionHeader("Academic Details")

                    val standardOptions = (1..12).map { it.toString() } + "Undergraduate (Bachelors)"

                    EditProfileDropdown(profile.standard, standardOptions, "Class / Standard") { sel ->
                        viewModel.updateField { standard = sel; stream = ""; subjects = emptyList(); board9_10 = ""; board11_12 = "" }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val classInt = profile.standard.toIntOrNull() ?: 0
                    val isBachelors = profile.standard == "Undergraduate (Bachelors)"

                    // LOGIC: Class 9-10
                    if (classInt in 9..10) {
                        EditProfileDropdown(profile.board9_10, listOf("CBSE", "ICSE", "State Board (SSC)", "Other"), "Board") { viewModel.updateField { board9_10 = it } }
                    }

                    // LOGIC: Class 11-12
                    if (classInt in 11..12) {
                        EditProfileDropdown(profile.board11_12, listOf("CBSE", "ICSE", "State Board (HSC)", "NIOS", "Other"), "Board") { viewModel.updateField { board11_12 = it } }
                        Spacer(modifier = Modifier.height(12.dp))
                        EditProfileDropdown(profile.stream, listOf("Science", "Commerce", "Arts"), "Stream") { viewModel.updateField { stream = it; subjects = emptyList() } }

                        if (profile.stream.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Subjects", color = Color.White, fontSize = 14.sp)
                            val options = when(profile.stream) {
                                "Commerce" -> listOf("Accountancy", "Business Studies", "Economics", "Maths", "IP")
                                "Arts" -> listOf("History", "Geography", "Pol Science", "Psychology", "Economics")
                                "Science" -> listOf("Physics", "Chemistry", "Maths", "Biology", "Computer Science")
                                else -> emptyList()
                            }
                            options.forEach { subj ->
                                EditProfileCheckbox(subj, profile.subjects.contains(subj)) { checked ->
                                    val current = profile.subjects.toMutableList()
                                    if (checked) current.add(subj) else current.remove(subj)
                                    viewModel.updateField { subjects = current }
                                }
                            }
                        }
                    }

                    // LOGIC: Bachelors
                    if (isBachelors) {
                        EditProfileDropdown(
                            selected = profile.board11_12,
                            options = listOf("Mumbai University", "Savitribai Phule Pune University", "Nagpur University", "Shivaji University", "Autonomous / Deemed", "Other"),
                            label = "University / Board"
                        ) { viewModel.updateField { board11_12 = it } }

                        Spacer(modifier = Modifier.height(12.dp))

                        EditProfileDropdown(
                            selected = profile.stream,
                            options = listOf("B.Sc IT / CS", "B.Sc General", "B.E. / B.Tech", "B.Com", "BMS / BBA", "B.A."),
                            label = "Degree Stream"
                        ) { viewModel.updateField { stream = it; subjects = emptyList() } }

                        if (profile.stream.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Subjects", color = Color.White, fontSize = 14.sp)

                            val options = when(profile.stream) {
                                "B.Sc IT / CS" -> listOf("Java Programming", "Python", "Data Structures", "Database / SQL", "Networking", "Web Dev")
                                "B.Sc General" -> listOf("Physics", "Chemistry", "Maths", "Botany", "Zoology")
                                "B.E. / B.Tech" -> listOf("Engineering Maths", "Physics", "Mechanics", "Basic Electrical", "Programming (C/C++)")
                                "B.Com" -> listOf("Financial Accounting", "Business Economics", "Commerce", "Business Law", "Maths & Stats")
                                "BMS / BBA" -> listOf("Management Principles", "Marketing", "Finance", "HRM", "Business Communication")
                                "B.A." -> listOf("Psychology", "Sociology", "Economics", "Political Science", "History", "English Lit")
                                else -> emptyList()
                            }

                            options.forEach { subj ->
                                EditProfileCheckbox(subj, profile.subjects.contains(subj)) { checked ->
                                    val current = profile.subjects.toMutableList()
                                    if (checked) current.add(subj) else current.remove(subj)
                                    viewModel.updateField { subjects = current }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- Save Button ---
                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                onSuccess = { Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SAVE CHANGES", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- SETTINGS SECTION (Logout) ---
                    EditSectionHeader("Account Settings")

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)), // Red Border
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

// ==========================================
// 3. Helper Components
// ==========================================

@Composable
fun EditSectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun EditProfileTextField(
    value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
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
fun EditProfileDropdown(selected: String, options: List<String>, label: String, onSelection: (String) -> Unit) {
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
fun EditProfileCheckbox(label: String, isChecked: Boolean, onChecked: (Boolean) -> Unit) {
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