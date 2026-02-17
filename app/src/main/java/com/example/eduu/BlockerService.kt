package com.example.eduu

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.SharedPreferences
import android.content.Context

class BlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("study_tools_prefs", Context.MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Check if Focus Mode is actually active
        val isFocusActive = prefs.getBoolean("is_focus_active", false)
        if (!isFocusActive) return

        // 2. Check if the event is a window state change (app opening)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // 3. Check if this package is in our block list
            // Note: In a real app, you'd store the set of blocked package names.
            // For this demo, we check against a hardcoded list or a simplified string set.
            val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()

            // Add common package names for testing if list is empty or matching logic
            // Instagram: com.instagram.android
            // YouTube: com.google.android.youtube
            // TikTok: com.zhiliaoapp.musically

            if (isPackageBlocked(packageName, blockedApps)) {
                // 4. ACTION: Go Home immediately (The "Block")
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Optional: You could also launch an Intent to your own "BlockedActivity" here
                // val intent = Intent(this, MainActivity::class.java)
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // startActivity(intent)
            }
        }
    }

    private fun isPackageBlocked(packageName: String, blockedList: Set<String>): Boolean {
        // Simple check: does the blocked list contain the package?
        // Or specific hardcoded checks for testing:
        if (packageName.contains("instagram") && blockedList.contains("Instagram")) return true
        if (packageName.contains("youtube") && blockedList.contains("YouTube")) return true
        if (packageName.contains("tiktok") && blockedList.contains("TikTok")) return true
        if (packageName.contains("facebook") && blockedList.contains("Facebook")) return true

        return false
    }

    override fun onInterrupt() {
        // Required method
    }
}