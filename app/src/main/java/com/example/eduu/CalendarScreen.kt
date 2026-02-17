package com.example.eduu

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. VIEW MODEL
// ==========================================
class CalendarViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var eventListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null

    private val _rawEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _rawTasks = MutableStateFlow<List<Task>>(emptyList())

    val allTasks = _rawTasks.asStateFlow()
    private val _displayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val displayEvents = _displayEvents.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar
        refreshEvents()
    }

    // --- SECURE DATA LOADING ---
    fun loadDataForUser(uid: String) {
        // Detach old listeners
        eventListener?.remove()
        taskListener?.remove()

        // Clear UI immediately
        _rawEvents.value = emptyList()
        _rawTasks.value = emptyList()
        _displayEvents.value = emptyList()

        // Fetch Events
        eventListener = db.collection("users").document(uid).collection("events")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    _rawEvents.value = snap.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }
                    refreshEvents()
                }
            }

        // Fetch Tasks
        taskListener = db.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    _rawTasks.value = snap.documents.mapNotNull { it.toObject(Task::class.java) }
                        .sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.dateMillis })
                }
            }
    }

    override fun onCleared() { super.onCleared(); eventListener?.remove(); taskListener?.remove() }

    private fun refreshEvents() {
        val target = _selectedDate.value
        val items = mutableListOf<CalendarEvent>()
        val now = System.currentTimeMillis()

        _rawEvents.value.forEach { parent ->
            RecurrenceEngine.generateInstanceForDate(parent, target)?.let { instance ->
                // --- FILTER: HIDE PAST EVENTS ---
                // Only show if End Time is in the future
                if (instance.endTimeMillis > now) {
                    items.add(instance)
                }
            }
        }
        _displayEvents.value = items.sortedBy { it.dateMillis }
    }

    // --- CRUD ---
    fun addEvent(context: Context, event: CalendarEvent) {
        val uid = auth.currentUser?.uid ?: return
        val doc = db.collection("users").document(uid).collection("events").document()
        val finalEvent = event.copy(id = doc.id)
        doc.set(finalEvent).addOnSuccessListener {
            scheduleNotification(context, finalEvent)
            Toast.makeText(context, "Event scheduled", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateEvent(context: Context, event: CalendarEvent) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("events").document(event.id).set(event)
            .addOnSuccessListener { scheduleNotification(context, event) }
    }

    fun deleteEvent(eventId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("events").document(eventId).delete()
    }

    fun addTask(task: Task) {
        val uid = auth.currentUser?.uid ?: return
        val doc = db.collection("users").document(uid).collection("tasks").document()
        doc.set(task.copy(id = doc.id))
    }

    fun updateTask(task: Task) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("tasks").document(task.id).set(task)
    }

    fun deleteTask(taskId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("tasks").document(taskId).delete()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(context: Context, event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Reminders
        event.reminders.forEachIndexed { i, mins ->
            val trigger = event.dateMillis - (mins * 60 * 1000)
            if (trigger > System.currentTimeMillis()) {
                val intent = Intent(context, EventNotificationReceiver::class.java).apply {
                    putExtra("title", "Upcoming: ${event.title}")
                    putExtra("message", "Starts in $mins minutes")
                }
                val uniqueId = (event.id + "rem" + i).hashCode()
                val pending = PendingIntent.getBroadcast(
                    context, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending) }
                catch (e: SecurityException) { e.printStackTrace() }
            }
        }

        // 2. Start Time
        if (event.dateMillis > System.currentTimeMillis()) {
            val intent = Intent(context, EventNotificationReceiver::class.java).apply {
                putExtra("title", event.title)
                putExtra("message", "Event is starting now!")
            }
            val uniqueId = (event.id + "start").hashCode()
            val pending = PendingIntent.getBroadcast(
                context, uniqueId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.dateMillis, pending) }
            catch (e: SecurityException) { e.printStackTrace() }
        }
    }
}

// ==========================================
// 2. UI SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val viewModel: CalendarViewModel = viewModel()
    val displayEvents by viewModel.displayEvents.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Load Data on User Change
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { viewModel.loadDataForUser(it) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    // --- PERMISSIONS ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var hasPermission by remember { mutableStateOf(true) }

    // Check permission on every resume
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler { onBack() }

    Scaffold(
        // FIX: Pushes content below status bar
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column(Modifier.background(Color(0xFF0F172A))) {
                TopAppBar(
                    title = { Text("Planner", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )

                // FORCE PERMISSION BUTTON
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("⚠️ Tap to Enable Notifications")
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFF6366F1))
                        }
                    }
                ) {
                    Tab(selected = selectedTab==0, onClick = { selectedTab=0 }, text = { Text("Schedule", color = if(selectedTab==0) Color(0xFF6366F1) else Color.Gray) })
                    Tab(selected = selectedTab==1, onClick = { selectedTab=1 }, text = { Text("Tasks", color = if(selectedTab==1) Color(0xFF6366F1) else Color.Gray) })
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF6366F1), contentColor = Color.White) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(padding)) {

            // --- TAB 1: SCHEDULE (Timeline View) ---
            if (selectedTab == 0) {
                Column(Modifier.fillMaxSize()) {
                    // Month Calendar
                    CalendarGridWidget(selectedDate) { viewModel.selectDate(it) }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Day Timeline", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    // TIMELINE
                    if (displayEvents.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No upcoming events today.", color = Color.White.copy(0.5f))
                        }
                    } else {
                        // Timeline List
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(24) { hour -> // 0 to 23 hours
                                TimelineHourRow(hour, displayEvents) { eventToEdit = it }
                            }
                        }
                    }
                }
            }

            // --- TAB 2: TASKS ---
            if (selectedTab == 1) {
                if (allTasks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTasks) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { viewModel.updateTask(task.copy(isCompleted = !task.isCompleted)) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || eventToEdit != null) {
        AddObjectDialog(
            selectedDate = selectedDate,
            initialEvent = eventToEdit,
            initialTab = selectedTab,
            onDismiss = { showAddDialog = false; eventToEdit = null },
            onSaveEvent = { evt ->
                if (eventToEdit == null) viewModel.addEvent(context, evt)
                else viewModel.updateEvent(context, evt.copy(id = eventToEdit!!.id))
                showAddDialog = false; eventToEdit = null
            },
            onSaveTask = { tsk -> viewModel.addTask(tsk); showAddDialog = false; eventToEdit = null },
            onDeleteEvent = { if (eventToEdit != null) { viewModel.deleteEvent(eventToEdit!!.id); eventToEdit = null } }
        )
    }
}

// ==========================================
// 3. COMPONENTS
// ==========================================

// --- NEW: TIMELINE ROW (Hour + Events) ---
@Composable
fun TimelineHourRow(hour: Int, events: List<CalendarEvent>, onEventClick: (CalendarEvent) -> Unit) {
    // Find events that START in this hour
    val eventsInHour = events.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        c.get(Calendar.HOUR_OF_DAY) == hour
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min) // Grow height if needed
    ) {
        // Hour Label (Left)
        Text(
            text = String.format("%02d:00", hour),
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .width(60.dp)
                .padding(top = 10.dp, start = 16.dp)
        )

        // Grid Line & Content
        Column(
            modifier = Modifier
                .weight(1f)
                .border(width = 0.5.dp, color = Color.White.copy(0.1f), shape = RoundedCornerShape(0.dp))
                .padding(4.dp)
        ) {
            if (eventsInHour.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp)) // Empty slot height
            } else {
                eventsInHour.forEach { event ->
                    TimelineEventCard(event) { onEventClick(event) }
                }
            }
        }
    }
}

@Composable
fun TimelineEventCard(event: CalendarEvent, onClick: () -> Unit) {
    val end = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.endTimeMillis))

    Surface(
        color = Color(android.graphics.Color.parseColor(event.colorHex)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Ends at $end", color = Color.White.copy(0.8f), fontSize = 10.sp)
        }
    }
}

// --- FIXED TASK CARD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() } // WHOLE CARD CLICKABLE
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Checkbox ignores internal clicks, passes to parent
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4ADE80), uncheckedColor = Color.Gray, checkmarkColor = Color.Black)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.title,
                color = if(task.isCompleted) Color.Gray else Color.White,
                textDecoration = if(task.isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) }
        }
    }
}

// --- CALENDAR GRID (Month View) ---
@Composable
fun CalendarGridWidget(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
    val displayMonth = remember(selectedDate.timeInMillis) { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate.time) }
    val temp = selectedDate.clone() as Calendar
    temp.set(Calendar.DAY_OF_MONTH, 1)
    val startDay = temp.get(Calendar.DAY_OF_WEEK)
    val maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

    Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { val c=selectedDate.clone() as Calendar; c.add(Calendar.MONTH, -1); onDateSelected(c) }) { Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White) }
                Text(displayMonth, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                IconButton(onClick = { val c=selectedDate.clone() as Calendar; c.add(Calendar.MONTH, 1); onDateSelected(c) }) { Icon(Icons.Rounded.ChevronRight, null, tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S","M","T","W","T","F","S").forEach {
                    Text(it, color=Color.Gray, fontSize=12.sp, modifier=Modifier.width(35.dp), textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            var dayCounter = 1
            for(row in 0 until 6) {
                if(dayCounter > maxDays) break
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    for(col in 1..7) {
                        if ((row == 0 && col < startDay) || dayCounter > maxDays) { Spacer(Modifier.width(35.dp)) }
                        else {
                            val d = dayCounter
                            val isSelected = d == selectedDate.get(Calendar.DAY_OF_MONTH)
                            Box(
                                Modifier.width(35.dp).aspectRatio(1f).clip(CircleShape)
                                    .background(if(isSelected) Color(0xFF6366F1) else Color.Transparent)
                                    .clickable { val c=selectedDate.clone() as Calendar; c.set(Calendar.DAY_OF_MONTH, d); onDateSelected(c) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d.toString(), color = if(isSelected) Color.White else Color.White.copy(0.7f))
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

// --- ADD DIALOG (Updated Logic) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectDialog(
    selectedDate: Calendar,
    initialEvent: CalendarEvent? = null,
    initialTab: Int,
    onDismiss: () -> Unit,
    onSaveEvent: (CalendarEvent) -> Unit,
    onSaveTask: (Task) -> Unit,
    onDeleteEvent: () -> Unit
) {
    var isTask by remember { mutableStateOf(if (initialEvent != null) false else initialTab == 1) }
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var startTimeMillis by remember { mutableLongStateOf(initialEvent?.dateMillis ?: System.currentTimeMillis()) }
    var endTimeMillis by remember { mutableLongStateOf(initialEvent?.endTimeMillis ?: (System.currentTimeMillis() + 3600000)) }
    var reminderIndex by remember { mutableIntStateOf(if (initialEvent?.reminders?.isNotEmpty() == true) 1 else 0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (initialEvent == null) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            c.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            c.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            startTimeMillis = c.timeInMillis
            endTimeMillis = c.timeInMillis + 3600000
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            if (initialEvent == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { isTask = false }, colors = ButtonDefaults.buttonColors(containerColor = if(!isTask) Color(0xFF6366F1) else Color.Gray)) { Text("Event") }
                    Button(onClick = { isTask = true }, colors = ButtonDefaults.buttonColors(containerColor = if(isTask) Color(0xFF6366F1) else Color.Gray)) { Text("Task") }
                }
            } else { Text("Edit Event", color = Color.White) }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(Modifier.height(16.dp))

                if (!isTask) {
                    // Time Pickers (Using mutableLongStateOf triggers UI update correctly)
                    val startCal = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
                    val endCal = Calendar.getInstance().apply { timeInMillis = endTimeMillis }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                        TimePickerDialog(context, { _, h, m ->
                            startCal.set(Calendar.HOUR_OF_DAY, h); startCal.set(Calendar.MINUTE, m)
                            startTimeMillis = startCal.timeInMillis
                        }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), false).show()
                    }) {
                        Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF6366F1))
                        Spacer(Modifier.width(8.dp))
                        Text("Start: " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startTimeMillis)), color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                        TimePickerDialog(context, { _, h, m ->
                            endCal.set(Calendar.HOUR_OF_DAY, h); endCal.set(Calendar.MINUTE, m)
                            endTimeMillis = endCal.timeInMillis
                        }, endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE), false).show()
                    }) {
                        Icon(Icons.Rounded.Schedule, null, tint = Color(0xFFEC4899))
                        Spacer(Modifier.width(8.dp))
                        Text("End:   " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(endTimeMillis)), color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Remind me:", color = Color.Gray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("None", "5m", "15m", "1h").forEachIndexed { index, label ->
                            FilterChip(
                                selected = reminderIndex == index,
                                onClick = { reminderIndex = index },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isTask) {
                    onSaveTask(Task(title = title, dateMillis = startTimeMillis))
                } else {
                    val rems = when(reminderIndex) {
                        1 -> listOf(5L); 2 -> listOf(15L); 3 -> listOf(60L); else -> emptyList()
                    }
                    onSaveEvent(CalendarEvent(title = title, dateMillis = startTimeMillis, endTimeMillis = endTimeMillis, reminders = rems))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) { Text("Save") }
        },
        dismissButton = {
            if (initialEvent != null) { TextButton(onClick = onDeleteEvent) { Text("Delete", color = Color(0xFFFF5252)) } }
            else { TextButton(onClick = onDismiss) { Text("Cancel") } }
        }
    )
}