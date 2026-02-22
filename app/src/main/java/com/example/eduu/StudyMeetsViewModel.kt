package com.example.eduu

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudyMeetsViewModel : ViewModel() {

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    fun updateRoomCode(code: String) {
        _roomCode.value = code
    }

    /**
     * Generates a random 9-letter lowercase code formatted as "abc-def-ghi"
     */
    fun generateRandomCode() {
        val allowedChars = ('a'..'z')
        val randomString = (1..9)
            .map { allowedChars.random() }
            .joinToString("")

        val formattedCode = "${randomString.substring(0, 3)}-${randomString.substring(3, 6)}-${randomString.substring(6, 9)}"
        _roomCode.value = formattedCode
    }
}