package com.invoiceflow.billing.ui.screens.staff

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.ActivityLog
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.StaffViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    onBackClick: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val logsResult by viewModel.logsState.collectAsState()
    val staffResult by viewModel.staffListState.collectAsState()

    // Filters
    val selectedUser by viewModel.userFilter.collectAsState()
    val selectedAction by viewModel.actionFilter.collectAsState()
    val selectedRange by viewModel.dateRangeFilter.collectAsState()

    val actionTypes = listOf(
        "USER_LOGIN",
        "USER_LOGOUT",
        "INVOICE_CREATED",
        "PRODUCT_ADDED",
        "PRODUCT_UPDATED",
        "PRODUCT_DELETED",
        "BARCODE_SCANNED",
        "SUBSCRIPTION_CHECKED"
    )

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Activity Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val logs = (logsResult as? Result.Success)?.data ?: emptyList()
                            if (logs.isNotEmpty()) {
                                exportLogsToCsv(context, logs)
                            } else {
                                Toast.makeText(context, "No logs available to export", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV")
                    }
                },
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cashier Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All Cashiers" Chip
                    FilterChip(
                        selected = selectedUser == null,
                        onClick = { viewModel.userFilter.value = null },
                        label = { Text("All Staff") }
                    )

                    val staffMembers = (staffResult as? Result.Success)?.data ?: emptyList()
                    staffMembers.forEach { member ->
                        FilterChip(
                            selected = selectedUser == member.userId,
                            onClick = { viewModel.userFilter.value = member.userId },
                            label = { Text(member.name) }
                        )
                    }
                }

                // Action Type Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAction == null,
                        onClick = { viewModel.actionFilter.value = null },
                        label = { Text("All Actions") }
                    )

                    actionTypes.forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { viewModel.actionFilter.value = action },
                            label = { Text(action.replace("_", " ")) }
                        )
                    }
                }

                // Date Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                    val rangeText = if (selectedRange != null) {
                        "${sdf.format(selectedRange!!.first)} - ${sdf.format(selectedRange!!.second)}"
                    } else {
                        "All Dates"
                    }

                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = rangeText, fontSize = 12.sp)
                    }

                    if (selectedRange != null) {
                        IconButton(onClick = { viewModel.dateRangeFilter.value = null }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Dates")
                        }
                    }
                }
            }

            // Results List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                when (logsResult) {
                    is Result.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Result.Error -> {
                        Text(
                            text = "Error loading activity logs: ${(logsResult as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }
                    is Result.Success -> {
                        val logs = (logsResult as Result.Success<List<ActivityLog>>).data
                        
                        if (logs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No logs match the active filters.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(logs, key = { it.logId }) { log ->
                                    LogGridItem(log = log)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date Pickers Dialogs
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMs = datePickerState.selectedDateMillis
                            if (selectedMs != null) {
                                showStartDatePicker = false
                                showEndDatePicker = true
                                viewModel.dateRangeFilter.value = Pair(Date(selectedMs), Date())
                            }
                        }
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState, title = { Text("Select Start Date", modifier = Modifier.padding(16.dp)) })
            }
        }

        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMs = datePickerState.selectedDateMillis
                            if (selectedMs != null && viewModel.dateRangeFilter.value != null) {
                                showEndDatePicker = false
                                val start = viewModel.dateRangeFilter.value!!.first
                                viewModel.dateRangeFilter.value = Pair(start, Date(selectedMs + 24 * 60 * 60 * 1000 - 1000)) // Include whole day
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState, title = { Text("Select End Date", modifier = Modifier.padding(16.dp)) })
            }
        }
    }
}

@Composable
fun LogGridItem(log: ActivityLog) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val logTime = sdf.format(log.timestamp.toDate())

    val (icon, tint) = when (log.actionType) {
        "USER_LOGIN" -> Pair(Icons.Default.Login, Color(0xFF2E7D32))
        "USER_LOGOUT" -> Pair(Icons.Default.Logout, Color(0xFFC62828))
        "INVOICE_CREATED" -> Pair(Icons.Default.ReceiptLong, Color(0xFF1976D2))
        "BARCODE_SCANNED" -> Pair(Icons.Default.QrCodeScanner, Color(0xFFE65100))
        "PRODUCT_ADDED" -> Pair(Icons.Default.AddBox, Color(0xFF00ACC1))
        "PRODUCT_UPDATED" -> Pair(Icons.Default.Edit, Color(0xFF8E24AA))
        "USER_CREATED" -> Pair(Icons.Default.PersonAdd, Color(0xFF2E7D32))
        else -> Pair(Icons.Default.Info, Color(0xFF546E7A))
    }

    val actionText = when (log.actionType) {
        "USER_LOGIN" -> "Logged In"
        "USER_LOGOUT" -> "Logged Out"
        "USER_CREATED" -> "Created Cashier: ${log.details["email"]}"
        "INVOICE_CREATED" -> {
            val total = log.details["grandTotal"] ?: "0"
            "Checked Out Invoice for ₹$total"
        }
        "PRODUCT_ADDED" -> "Added Product '${log.details["productName"] ?: "Item"}'"
        "PRODUCT_UPDATED" -> "Updated Product '${log.details["productName"] ?: "Item"}'"
        "BARCODE_SCANNED" -> "Scanned Barcode: ${log.details["barcode"]}"
        else -> log.actionType.replace("_", " ")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${log.userName} • $logTime",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun exportLogsToCsv(context: Context, logs: List<ActivityLog>) {
    try {
        val csv = StringBuilder()
        csv.append("Log ID,Timestamp,User Name,User Email,Action Type,Device,Details\n")
        logs.forEach { log ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(log.timestamp.toDate())
            val detailsCleaned = log.details.toString().replace("\"", "\"\"")
            csv.append("\"${log.logId}\",\"$dateStr\",\"${log.userName}\",\"${log.userEmail}\",\"${log.actionType}\",\"${log.deviceInfo}\",\"$detailsCleaned\"\n")
        }

        val file = File(context.cacheDir, "InvoiceFlow_AuditLogs_${System.currentTimeMillis()}.csv")
        file.writeText(csv.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "com.invoiceflow.billing.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Audit Logs CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
