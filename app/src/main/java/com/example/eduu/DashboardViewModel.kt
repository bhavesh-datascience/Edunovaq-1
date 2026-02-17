package com.example.eduu

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale

data class DashboardStats(
    val studyHours: String = "0",
    val studyStreak: String = "0",
    val totalHours: String = "0",
    // Graph Data: 7 floats representing hours for the last 7 days
    val weeklyData: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val dayLabels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val taskCompletionRate: Float = 0f
)

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    init {
        loadRealStats()
    }

    fun loadRealStats() {
        val uid = auth.currentUser?.uid ?: return
        val endOfToday = getEndOfDay(System.currentTimeMillis())
        val sevenDaysAgo = endOfToday - (7 * 24 * 60 * 60 * 1000)

        // Fetch Events (For Graph & Total Hours)
        db.collection("users").document(uid).collection("events")
            .addSnapshotListener { eventSnap, _ ->
                val events = eventSnap?.documents?.mapNotNull { it.toObject(CalendarEvent::class.java) } ?: emptyList()

                // 1. Calculate Total Lifetime Hours
                val totalMillis = events.sumOf { it.endTimeMillis - it.dateMillis }
                val totalHours = totalMillis / (1000 * 60 * 60)

                // 2. Calculate Last 7 Days Data (Graph)
                val weeklyMap = mutableMapOf<Int, Float>() // DayOfYear -> Hours
                val labels = mutableListOf<String>()
                val dataPoints = mutableListOf<Float>()

                // Pre-fill map with 0f
                val calendar = Calendar.getInstance()
                for (i in 6 downTo 0) {
                    calendar.timeInMillis = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000)
                    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                    weeklyMap[dayOfYear] = 0f
                    // Create Label (e.g., "Mon")
                    labels.add(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "?")
                }

                // Sum up hours per day
                events.forEach { event ->
                    if (event.dateMillis > sevenDaysAgo) {
                        calendar.timeInMillis = event.dateMillis
                        val day = calendar.get(Calendar.DAY_OF_YEAR)
                        if (weeklyMap.containsKey(day)) {
                            val hours = (event.endTimeMillis - event.dateMillis) / (1000f * 60 * 60)
                            weeklyMap[day] = weeklyMap[day]!! + hours
                        }
                    }
                }

                // Extract ordered data
                weeklyMap.toSortedMap().forEach { (_, hours) -> dataPoints.add(hours) }

                // Ensure we have exactly 7 points (fallback)
                val finalData = if (dataPoints.size == 7) dataPoints else List(7) { 0f }

                // 3. Task Completion (For Distribution Bar)
                db.collection("users").document(uid).collection("tasks")
                    .addSnapshotListener { taskSnap, _ ->
                        val tasks = taskSnap?.documents?.mapNotNull { it.toObject(Task::class.java) } ?: emptyList()
                        val total = tasks.size
                        val done = tasks.count { it.isCompleted }
                        val rate = if (total > 0) done.toFloat() / total else 0f

                        _stats.value = _stats.value.copy(
                            totalHours = totalHours.toString(),
                            weeklyData = finalData,
                            dayLabels = labels,
                            taskCompletionRate = rate,
                            studyHours = String.format("%.1f", finalData.lastOrNull() ?: 0f) // Today's hours
                        )
                    }
            }
    }

    // Helper to get streak (Connecting to your existing SharedPrefs logic in UI)
    fun updateStreakUI(streak: Int) {
        _stats.value = _stats.value.copy(studyStreak = streak.toString())
    }

    private fun getEndOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        return c.timeInMillis
    }
}