package com.invoiceflow.billing.ui.screens.staff

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.StaffViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierProfileScreen(
    cashierId: String,
    onBackClick: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val staffListResult by viewModel.staffListState.collectAsState()
    val performanceResult by viewModel.cashierPerformanceState.collectAsState()
    val timelineLogs by viewModel.cashierLogsState.collectAsState()

    // Find the current cashier details from staff list state
    val cashier = remember(staffListResult, cashierId) {
        (staffListResult as? Result.Success)?.data?.find { it.userId == cashierId }
    }

    LaunchedEffect(cashierId) {
        viewModel.loadCashierProfileData(cashierId, cashier?.name ?: "Cashier")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cashier?.name ?: "Cashier Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (cashier == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val initials = cashier.name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Header Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = cashier.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cashier.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cashier.phone.isNotEmpty()) {
                        Text(
                            text = cashier.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status and Role
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleBadge(role = cashier.role)
                        
                        val statusText = if (cashier.isActive) "Active" else "Inactive"
                        val statusColor = if (cashier.isActive) Color(0xFF2E7D32) else Color(0xFF78909C)
                        val statusBg = if (cashier.isActive) Color(0xFFE8F5E9) else Color(0xFFECEFF1)
                        
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Active Toggle switch with confirm dialog
                    var showConfirmDialog by remember { mutableStateOf(false) }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account Access Allowed",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = cashier.isActive,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    showConfirmDialog = true
                                } else {
                                    viewModel.toggleUserStatus(
                                        userId = cashier.userId,
                                        isActive = true,
                                        onSuccess = {
                                            Toast.makeText(context, "Account activated", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            }
                        )
                    }

                    if (showConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDialog = false },
                            title = { Text("Deactivate Cashier?") },
                            text = { Text("Deactivating this account stops all POS sales access for this cashier and forces logout.") },
                            confirmButton = {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        showConfirmDialog = false
                                        viewModel.toggleUserStatus(
                                            userId = cashier.userId,
                                            isActive = false,
                                            onSuccess = {
                                                Toast.makeText(context, "Account deactivated", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                        )
                                    }
                                ) {
                                    Text("Deactivate")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            // Stats grid section
            item {
                Text(
                    text = "Performance Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                when (performanceResult) {
                    is Result.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Result.Error -> {
                        Text(
                            text = "Failed to load metrics: ${(performanceResult as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is Result.Success -> {
                        val stats = (performanceResult as Result.Success<CashierPerformance>).data
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // All time stats
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileMetricCard(
                                    title = "All-Time Sales",
                                    value = "₹${String.format("%.2f", stats.totalRevenue)}",
                                    subText = "${stats.totalBills} bills total",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF1976D2)
                                )
                                ProfileMetricCard(
                                    title = "Avg Ticket Size",
                                    value = "₹${String.format("%.2f", stats.averageBillValue)}",
                                    subText = "Per invoice",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFBB86FC)
                                )
                            }
                            // Today stats
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileMetricCard(
                                    title = "Today's Sales",
                                    value = "₹${String.format("%.2f", stats.revenueToday)}",
                                    subText = "${stats.billsToday} bills generated",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF2E7D32)
                                )
                                ProfileMetricCard(
                                    title = "Peak Sales Day",
                                    value = stats.bestPerformingDay,
                                    subText = "Highest day total",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            }

            // Timeline logs section
            item {
                Text(
                    text = "Cashier Activity Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (timelineLogs.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity recorded for this cashier.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(timelineLogs) { log ->
                    TimelineItem(log = log)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileMetricCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(text = subText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun TimelineItem(log: ActivityLog) {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val logTime = sdf.format(log.timestamp.toDate())

    val (icon, tint) = when (log.actionType) {
        "USER_LOGIN" -> Pair(Icons.Default.Login, Color(0xFF2E7D32))
        "USER_LOGOUT" -> Pair(Icons.Default.Logout, Color(0xFFC62828))
        "INVOICE_CREATED" -> Pair(Icons.Default.ReceiptLong, Color(0xFF1976D2))
        "BARCODE_SCANNED" -> Pair(Icons.Default.QrCodeScanner, Color(0xFFE65100))
        "PRODUCT_ADDED" -> Pair(Icons.Default.AddBox, Color(0xFF00ACC1))
        "PRODUCT_UPDATED" -> Pair(Icons.Default.Edit, Color(0xFF8E24AA))
        else -> Pair(Icons.Default.Info, Color(0xFF546E7A))
    }

    val actionText = when (log.actionType) {
        "USER_LOGIN" -> "Logged In"
        "USER_LOGOUT" -> "Logged Out"
        "INVOICE_CREATED" -> {
            val total = log.details["grandTotal"] ?: "0"
            "Created Invoice for ₹$total"
        }
        "PRODUCT_ADDED" -> "Added Product '${log.details["productName"] ?: "Item"}'"
        "PRODUCT_UPDATED" -> "Updated Product '${log.details["productName"] ?: "Item"}'"
        "BARCODE_SCANNED" -> "Scanned Barcode: ${log.details["barcode"]}"
        else -> log.actionType.replace("_", " ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Vertical Timeline Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Vertical dotted connector line
            Canvas(modifier = Modifier.height(30.dp).width(2.dp)) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Log Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$logTime • ${log.deviceInfo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
