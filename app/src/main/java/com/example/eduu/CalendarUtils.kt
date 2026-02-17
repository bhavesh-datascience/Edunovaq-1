package com.example.eduu

import java.util.*

// ==========================================
// 1. DATA MODELS
// ==========================================

enum class ItemType { EVENT, TASK }

interface CalendarItem {
    val id: String
    val title: String
    val dateMillis: Long
    val type: ItemType
}

data class CalendarEvent(
    override val id: String = "",
    override val title: String = "",
    val description: String = "",
    override val dateMillis: Long = 0L, // Start Time
    val endTimeMillis: Long = 0L,       // End Time
    val location: String = "",

    // --- RECURRENCE ---
    val rrule: String? = null,
    val parentEventId: String? = null,
    val exdate: List<Long> = emptyList(),

    // --- META ---
    val colorHex: String = "#6366F1",
    val reminders: List<Long> = listOf(10L) // Minutes before event
) : CalendarItem {
    override val type = ItemType.EVENT
}

data class Task(
    override val id: String = "",
    override val title: String = "",
    val description: String = "",
    override val dateMillis: Long = 0L, // Due Date
    val isCompleted: Boolean = false,
    val priority: Int = 1
) : CalendarItem {
    override val type = ItemType.TASK
}

// ==========================================
// 2. RECURRENCE ENGINE
// ==========================================
object RecurrenceEngine {

    fun generateInstanceForDate(
        parent: CalendarEvent,
        targetDate: Calendar
    ): CalendarEvent? {
        if (getStartOfDay(parent.dateMillis) > getEndOfDay(targetDate.timeInMillis)) return null

        val targetStartOfDay = getStartOfDay(targetDate.timeInMillis)
        if (parent.exdate.any { isSameDay(it, targetStartOfDay) }) return null

        val eventStart = Calendar.getInstance().apply { timeInMillis = parent.dateMillis }

        val isMatch = when (parent.rrule) {
            null -> isSameDay(parent.dateMillis, targetDate.timeInMillis)
            "FREQ=DAILY" -> true
            "FREQ=WEEKLY" -> eventStart.get(Calendar.DAY_OF_WEEK) == targetDate.get(Calendar.DAY_OF_WEEK)
            "FREQ=MONTHLY" -> eventStart.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)
            else -> isSameDay(parent.dateMillis, targetDate.timeInMillis)
        }

        if (!isMatch) return null

        val duration = parent.endTimeMillis - parent.dateMillis
        val newStart = targetDate.clone() as Calendar
        newStart.set(Calendar.HOUR_OF_DAY, eventStart.get(Calendar.HOUR_OF_DAY))
        newStart.set(Calendar.MINUTE, eventStart.get(Calendar.MINUTE))

        return parent.copy(
            id = "${parent.id}_virtual_${targetDate.timeInMillis}",
            dateMillis = newStart.timeInMillis,
            endTimeMillis = newStart.timeInMillis + duration,
            parentEventId = parent.id
        )
    }

    // --- Helpers ---
    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
    }
}