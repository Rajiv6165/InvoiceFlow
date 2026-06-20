package com.invoiceflow.billing.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Slide-in Offline Warning Banner at screen tops
 */
@Composable
fun OfflineBanner(
    isOnline: Boolean,
    isSyncing: Boolean
) {
    AnimatedVisibility(
        visible = !isOnline || isSyncing,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val bgColor = if (isSyncing) Color(0xFF00897B) else Color(0xFFC62828)
        val text = if (isSyncing) "Syncing pending offline bills to cloud..." else "Working offline - changes will sync when connected"
        val icon = if (isSyncing) Icons.Default.Sync else Icons.Default.WifiOff
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .statusBarsPadding() // Keep clear of status bar overlaps
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Apply rotation if syncing
            val modifier = if (isSyncing) {
                val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "angle"
                )
                Modifier.graphicsLayer(rotationZ = angle)
            } else {
                Modifier
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
