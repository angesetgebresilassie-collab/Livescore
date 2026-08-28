package com.livescore.football

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.livescore.football.ui.screens.PermissionScreen
import com.livescore.football.utils.PermissionHelper

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissionsGranted = remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all permissions were granted
        val allGranted = permissions.values.all { it }
        permissionsGranted.value = allGranted
    }
    
    LaunchedEffect(Unit) {
        if (PermissionHelper.hasAllPermissions(context)) {
            permissionsGranted.value = true
        } else {
            // Only request permissions on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val missingPermissions = PermissionHelper.getMissingPermissions(context)
                if (missingPermissions.isNotEmpty()) {
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                }
            } else {
                // For older Android versions, skip permission screen
                permissionsGranted.value = true
            }
        }
    }
    
    if (permissionsGranted.value) {
        content()
    } else {
        PermissionScreen()
    }
}
