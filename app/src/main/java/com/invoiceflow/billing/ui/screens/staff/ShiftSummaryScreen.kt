package com.invoiceflow.billing.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.Shift
import com.invoiceflow.billing.viewmodel.StaffViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftSummaryScreen(
    storeId: String,
    userId: String,
    userName: String,
    onClockOut: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel()
) {
    LaunchedEffect(storeId) {
        viewModel.setStoreId(storeId)
    }

    // Attempt to start a shift if not already open (for cashiers)
    LaunchedEffect(userId, userName) {
        viewModel.startShift(userId, userName)
    }

    val activeShift by viewModel.activeShiftState.collectAsState()
    val shiftsResult by viewModel.shiftsState.collectAsState()

    // Calculate real-time duration and stats since shift start
    var shiftDuration by remember { mutableStateOf("00h 00m") }
    LaunchedEffect(activeShift) {
        val shift = activeShift
        if (shift != null) {
            val startTimeMs = shift.startTime.toDate().time
            while (true) {
                val elapsedMs = System.currentTimeMillis() - startTimeMs
                val hours = elapsedMs / (1000 * 60 * 60)
                val minutes = (elapsedMs / (1000 * 60)) % 60
                shiftDuration = String.format("%02dh %02dm", hours, minutes)
                kotlinx.coroutines.delay(60 * 1000) // update every minute
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Work Shift", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val shift = activeShift

            if (shift == null) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Initiating Shift session...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Scaffold
            }

            // Shift Card Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2E7D32), // Secondary brand color (Green)
                                    Color(0xFF1B5E20)
                                )
                            )
                        )
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHIFT OPERATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Active since " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(shift.startTime.toDate()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Stats grid layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Shift Live Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Uptime
                ShiftStatRow(
                    icon = Icons.Default.Alarm,
                    label = "Uptime Duration",
                    value = shiftDuration,
                    color = Color(0xFFE65100)
                )

                // Bills processed
                ShiftStatRow(
                    icon = Icons.Default.ReceiptLong,
                    label = "Bills Checked Out",
                    value = "${shift.totalBills} bills",
                    color = Color(0xFF1976D2)
                )

                // Revenue
                ShiftStatRow(
                    icon = Icons.Default.Payments,
                    label = "Calculated Revenue",
                    value = "₹${String.format("%.2f", shift.totalRevenue)}",
                    color = Color(0xFF2E7D32)
                )
            }

            // End Shift Button
            var showClockOutConfirm by remember { mutableStateOf(false) }
            var isClosing by remember { mutableStateOf(false) }

            Button(
                onClick = { showClockOutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isClosing
            ) {
                if (isClosing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Closing Shift...")
                } else {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Shift & Clock Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (showClockOutConfirm) {
                AlertDialog(
                    onDismissRequest = { showClockOutConfirm = false },
                    title = { Text("End Shift?") },
                    text = { Text("Are you sure you want to end your shift? This will log your final transactions, record your total sales of ₹${String.format("%.2f", shift.totalRevenue)} for today, and log you out.") },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                showClockOutConfirm = false
                                isClosing = true
                                viewModel.closeShift(
                                    userId = userId,
                                    userName = userName,
                                    onComplete = {
                                        isClosing = false
                                        onClockOut()
                                    }
                                )
                            }
                        ) {
                            Text("End Shift")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClockOutConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ShiftStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color)
                    }
                }

                Text(text = label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
