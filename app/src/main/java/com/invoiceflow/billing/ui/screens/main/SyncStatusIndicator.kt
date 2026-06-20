package com.invoiceflow.billing.ui.screens.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Top bar Sync status status dot / spinning circle indicator
 */
@Composable
fun SyncStatusIndicator(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingQueueCount: Int,
    modifier: Modifier = Modifier
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    val statusColor = when {
        isSyncing -> Color(0xFFF57F17) // Syncing -> Yellow
        isOnline -> Color(0xFF2E7D32)  // Connected -> Green
        else -> Color.Gray             // Offline -> Grey
    }
    
    val statusDesc = when {
        isSyncing -> "Syncing data to cloud..."
        isOnline -> "Online - synced"
        else -> "Offline - caching locally"
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { showDetailsDialog = true }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSyncing) {
            val infiniteTransition = rememberInfiniteTransition(label = "indicator_sync")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = statusDesc,
                tint = statusColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = angle)
            )
        } else {
            // Pulse standard dot if offline with pending writes
            val dotModifier = if (!isOnline && pendingQueueCount > 0) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_offline")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                Modifier.scale(scale)
            } else {
                Modifier
            }
            
            Box(
                modifier = dotModifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
        }
    }
    
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Cloud Sync Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isOnline) "Connectivity: Online" else "Connectivity: Offline",
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text("Pending uploads: $pendingQueueCount invoices")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pendingQueueCount > 0) {
                            "All pending updates are cached locally on your device and will be synced to the database once connection is established."
                        } else {
                            "All transactions are safely backed up and synced to your cloud database."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
