package com.example.eduu

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * A reusable Permission Manager.
 * * Usage:
 * RequestPermissions(
 * permissions = listOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA),
 * onPermissionResult = { deniedPermissions ->
 * if (deniedPermissions.isEmpty()) {
 * // All Good!
 * } else {
 * // Show explanation for denied permissions
 * }
 * }
 * )
 */
@Composable
fun RequestPermissions(
    permissions: List<String>,
    onPermissionResult: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var hasRequested by remember { mutableStateOf(false) }

    // Filter out permissions we ALREADY have
    val permissionsToRequest = remember(permissions) {
        permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // 'result' is a Map<String, Boolean>. Filter to find denied ones.
        val denied = result.filterValues { !it }.keys.toList()
        onPermissionResult(denied)
    }

    // Launch ONLY if we haven't asked yet and there are permissions needed
    LaunchedEffect(permissionsToRequest) {
        if (!hasRequested && permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
            hasRequested = true
        } else if (permissionsToRequest.isEmpty()) {
            // If all were already granted, notify immediately with empty denied list
            onPermissionResult(emptyList())
        }
    }
}