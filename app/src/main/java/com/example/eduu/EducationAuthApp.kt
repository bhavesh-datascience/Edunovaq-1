package com.example.eduu

import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eduu.ui.theme.EduuTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
// REQUIRED CHANGE: Import PDFBox
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==========================================
// 1. The Main Activity
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // REQUIRED CHANGE: Initialize the PDF library context
        // This is crucial for "Chat with PDF" to work.
        PDFBoxResourceLoader.init(applicationContext)

        setContent {
            EduuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    AppRoot()
                }
            }
        }
    }
}

// ==========================================
// 2. The App Root
// ==========================================
@Composable
fun AppRoot(viewModel: AuthViewModel = viewModel()) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        EdunovaqAppFlow(viewModel)
    }
}

// ==========================================
// 3. Splash Screen
// ==========================================
@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Edunovaq",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}

// ==========================================
// 4. The Navigation Flow
// ==========================================
@Composable
fun EdunovaqAppFlow(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    if (state.isCheckingProfile) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF6366F1))
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // A. Main Dashboard Layer
        AnimatedVisibility(
            visible = state.isLoggedIn && state.isProfileComplete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DashboardScreen(
                userEmail = state.email,
                userName = state.name.ifBlank { "Student" },
                onLogout = { viewModel.logout() },
                onProfileClick = { viewModel.openProfile() },
                onCalendarClick = { viewModel.openCalendar() }
            )
        }

        // B. Registration Layer
        AnimatedVisibility(
            visible = state.isLoggedIn && !state.isProfileComplete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Ensure StudentRegistration.kt exists in the same package
            StudentRegistrationScreen(
                userEmail = state.email,
                onComplete = { viewModel.markProfileComplete() }
            )
        }

        // C. Auth Layer
        AnimatedVisibility(
            visible = !state.isLoggedIn,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AuthScreen(viewModel = viewModel)
        }

        // D. Profile Overlay
        AnimatedVisibility(
            visible = state.isProfileOpen,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Ensure ProfileScreen.kt exists in the same package
            ProfileScreen(
                onBack = { viewModel.closeProfile() },
                onLogout = {
                    viewModel.closeProfile()
                    viewModel.logout()
                }
            )
        }

        // E. Calendar Overlay
        AnimatedVisibility(
            visible = state.isCalendarOpen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            // Ensure CalendarScreen.kt exists in the same package
            CalendarScreen(onBack = { viewModel.closeCalendar() })
        }
    }
}

// ==========================================
// 5. Auth Screen
// ==========================================
@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Icon(Icons.Rounded.AutoAwesome, "Logo", tint = Color.White, modifier = Modifier.size(60.dp))
            Text("Edunovaq", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(30.dp))

            AuthGlassCard {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.isLoginMode) "Welcome Back" else "Join the Future", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (!state.isLoginMode) {
                        AuthGlassTextField(state.name, { viewModel.onEvent(AuthEvent.NameChanged(it)) }, "Full Name", Icons.Default.Person)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    AuthGlassTextField(state.email, { viewModel.onEvent(AuthEvent.EmailChanged(it)) }, "Email Address", Icons.Default.Email, KeyboardType.Email)
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthGlassPasswordField(state.password, { viewModel.onEvent(AuthEvent.PasswordChanged(it)) }, "Password")

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.error ?: "", color = Color(0xFFFF8A80), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.onEvent(AuthEvent.Submit) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isLoading
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text(if (state.isLoginMode) "LOG IN" else "SIGN UP", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (state.isLoginMode) "New to Edunovaq? " else "Already a member? ", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = if (state.isLoginMode) "Sign Up" else "Log In",
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.onEvent(AuthEvent.ToggleMode) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// ==========================================
// 6. Components
// ==========================================
@Composable
fun AuthGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), content = content)
}

@Composable
fun AuthGlassTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(icon, null, tint = Color.White.copy(alpha = 0.7f)) },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1), unfocusedBorderColor = Color.White.copy(alpha = 0.2f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White)
    )
}

@Composable
fun AuthGlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.7f)) },
        trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.White.copy(alpha = 0.7f)) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1), unfocusedBorderColor = Color.White.copy(alpha = 0.2f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White)
    )
}

// ==========================================
// 7. ViewModel
// ==========================================

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isCheckingProfile: Boolean = false,
    val isProfileComplete: Boolean = false,
    val isProfileOpen: Boolean = false,
    val isCalendarOpen: Boolean = false
)

sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class NameChanged(val name: String) : AuthEvent()
    object ToggleMode : AuthEvent()
    object Submit : AuthEvent()
    object DismissError : AuthEvent()
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                email = currentUser.email ?: "",
                name = currentUser.displayName ?: ""
            )
            checkUserProfile(currentUser.uid)
        }
    }

    private fun checkUserProfile(uid: String) {
        _uiState.value = _uiState.value.copy(isCheckingProfile = true)
        db.collection("students").document(uid).get()
            .addOnSuccessListener { document ->
                val exists = document.exists()
                _uiState.value = _uiState.value.copy(isCheckingProfile = false, isProfileComplete = exists)
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isCheckingProfile = false, isProfileComplete = false)
            }
    }

    fun markProfileComplete() {
        _uiState.value = _uiState.value.copy(isProfileComplete = true)
    }

    fun openProfile() {
        _uiState.value = _uiState.value.copy(isProfileOpen = true)
    }

    fun closeProfile() {
        _uiState.value = _uiState.value.copy(isProfileOpen = false)
    }

    fun openCalendar() {
        _uiState.value = _uiState.value.copy(isCalendarOpen = true)
    }

    fun closeCalendar() {
        _uiState.value = _uiState.value.copy(isCalendarOpen = false)
    }

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false, isProfileComplete = false)
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> _uiState.value = _uiState.value.copy(email = event.email)
            is AuthEvent.PasswordChanged -> _uiState.value = _uiState.value.copy(password = event.password)
            is AuthEvent.NameChanged -> _uiState.value = _uiState.value.copy(name = event.name)
            is AuthEvent.ToggleMode -> _uiState.value = _uiState.value.copy(isLoginMode = !_uiState.value.isLoginMode, error = null)
            is AuthEvent.Submit -> performAuth()
            is AuthEvent.DismissError -> _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun performAuth() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) { _uiState.value = s.copy(error = "Please fill all fields"); return }

        _uiState.value = s.copy(isLoading = true, error = null)

        if (s.isLoginMode) {
            auth.signInWithEmailAndPassword(s.email, s.password).addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    val user = auth.currentUser
                    _uiState.value = s.copy(isLoading = false, isLoggedIn = true, name = user?.displayName ?: "")
                    checkUserProfile(user!!.uid)
                } else _uiState.value = s.copy(isLoading = false, error = t.exception?.message ?: "Login failed")
            }
        } else {
            auth.createUserWithEmailAndPassword(s.email, s.password).addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(s.name).build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        _uiState.value = s.copy(isLoading = false, isLoggedIn = true, isProfileComplete = false)
                    }
                } else {
                    _uiState.value = s.copy(isLoading = false, error = t.exception?.message ?: "Sign up failed")
                }
            }
        }
    }
}