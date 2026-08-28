package com.livescore.football.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livescore.football.ui.components.AllowAllButton
import com.livescore.football.ui.components.PermissionRequestContainer

@Composable
fun PermissionScreen() {
    val notificationsGranted = remember { mutableStateOf(false) }
    val locationGranted = remember { mutableStateOf(false) }
    val cameraGranted = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Live Score Permissions",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Notifications Permission
            PermissionRequestContainer(
                title = "📢 Notifications",
                description = "Get live match alerts and score updates instantly",
                onAllowClick = {
                    notificationsGranted.value = true
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Location Permission
            PermissionRequestContainer(
                title = "📍 Location",
                description = "Show nearby matches and local team information",
                onAllowClick = {
                    locationGranted.value = true
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Camera Permission
            PermissionRequestContainer(
                title = "📷 Camera",
                description = "Enable video highlights and match streaming",
                onAllowClick = {
                    cameraGranted.value = true
                },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Allow All Button
            AllowAllButton(
                onClick = {
                    notificationsGranted.value = true
                    locationGranted.value = true
                    cameraGranted.value = true
                },
                modifier = Modifier.padding(top = 16.dp)
            )

            // Status Text
            if (notificationsGranted.value && locationGranted.value && cameraGranted.value) {
                Text(
                    text = "✅ All permissions granted!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
