package com.invoiceflow.billing.ui.screens.staff

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.Role
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.StaffViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    storeId: String,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToShifts: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(storeId) {
        viewModel.setStoreId(storeId)
    }

    val staffListResult by viewModel.staffListState.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var credentialsDialogUser by remember { mutableStateOf<User?>(null) }
    var credentialsDialogPass by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToShifts) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "View Shifts")
                    }
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = "Activity Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Staff", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (staffListResult) {
                is Result.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Result.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error loading staff members: ${(staffListResult as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is Result.Success -> {
                    val staff = (staffListResult as Result.Success<List<User>>).data
                    
                    if (staff.isEmpty()) {
                        EmptyStaffState(onAddClick = { showAddSheet = true })
                    } else {
                        val activeCount = staff.count { it.isActive }
                        val inactiveCount = staff.size - activeCount

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Breakdown Card
                            item {
                                StaffSummaryHeader(
                                    totalCount = staff.size,
                                    activeCount = activeCount,
                                    inactiveCount = inactiveCount
                                )
                            }

                            item {
                                Text(
                                    text = "Store Team Members",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Staff Members Lists
                            items(staff, key = { it.userId }) { member ->
                                StaffMemberCard(
                                    member = member,
                                    onCardClick = { onNavigateToProfile(member.userId) },
                                    onToggleStatus = { isActive ->
                                        viewModel.toggleUserStatus(
                                            userId = member.userId,
                                            isActive = isActive,
                                            onSuccess = {
                                                val msg = if (isActive) "Activated" else "Deactivated"
                                                Toast.makeText(context, "${member.name} $msg successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Sheets and Dialogs
            if (showAddSheet) {
                AddCashierBottomSheet(
                    onDismiss = { showAddSheet = false },
                    onCashierCreated = { cashier, password ->
                        showAddSheet = false
                        credentialsDialogUser = cashier
                        credentialsDialogPass = password
                    },
                    viewModel = viewModel
                )
            }

            if (credentialsDialogUser != null && credentialsDialogPass != null) {
                AlertDialog(
                    onDismissRequest = {
                        credentialsDialogUser = null
                        credentialsDialogPass = null
                    },
                    title = { Text("Account Created Successfully") },
                    text = {
                        Column {
                            Text("A cashier account has been created with the following temporary credentials. Copy these details and share them with the cashier:")
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = credentialsDialogUser!!.email,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Email / Login ID") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(credentialsDialogUser!!.email))
                                            Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = credentialsDialogPass!!,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Password") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(credentialsDialogPass!!))
                                            Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                credentialsDialogUser = null
                                credentialsDialogPass = null
                            }
                        ) {
                            Text("Done")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StaffSummaryHeader(
    totalCount: Int,
    activeCount: Int,
    inactiveCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF1565C0)
                        )
                    )
                )
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TOTAL TEAM MEMBERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$totalCount Staff",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryStatBadge(label = "Active", count = activeCount, color = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                SummaryStatBadge(label = "Inactive", count = inactiveCount, color = Color(0xFFECEFF1), textColor = Color(0xFF546E7A))
            }
        }
    }
}

@Composable
fun SummaryStatBadge(
    label: String,
    count: Int,
    color: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun StaffMemberCard(
    member: User,
    onCardClick: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    val initials = member.name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")

    // Generate deterministic background color for avatar
    val colorIndex = Math.abs(member.userId.hashCode()) % 5
    val avatarColor = when (colorIndex) {
        0 -> Color(0xFF1976D2) // Blue
        1 -> Color(0xFF2E7D32) // Green
        2 -> Color(0xFFE65100) // Orange
        3 -> Color(0xFF6A1B9A) // Purple
        else -> Color(0xFFC62828) // Red
    }

    val lastSeenStr = member.lastSeenAt?.let {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        "Last seen: " + sdf.format(it.toDate())
    } ?: "Never active"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (member.isActive) Color(0xFF2E7D32) else Color(0xFF90A4AE))
                        .border(2.dp, Color.White, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    RoleBadge(role = member.role)
                }

                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Shift: ${member.shift} • $lastSeenStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Quick Status Actions
            if (member.role != Role.OWNER) {
                var showDeactivateConfirm by remember { mutableStateOf(false) }

                IconButton(
                    onClick = {
                        if (member.isActive) {
                            showDeactivateConfirm = true
                        } else {
                            onToggleStatus(true)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (member.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = if (member.isActive) "Deactivate" else "Activate",
                        tint = if (member.isActive) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }

                if (showDeactivateConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeactivateConfirm = false },
                        title = { Text("Confirm Deactivation") },
                        text = { Text("Are you sure you want to deactivate ${member.name}? They will be logged out, their current shift closed, and they won't be able to log in.") },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    showDeactivateConfirm = false
                                    onToggleStatus(false)
                                }
                            ) {
                                Text("Deactivate")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeactivateConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun RoleBadge(role: Role) {
    val bgColor = if (role == Role.OWNER) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
    val txtColor = if (role == Role.OWNER) Color(0xFF1976D2) else Color(0xFF2E7D32)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = role.name,
            color = txtColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyStaffState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simple canvas illustration of a team
        Canvas(modifier = Modifier.size(120.dp)) {
            // Draw three simplified abstract circular avatar nodes linked together
            drawCircle(color = Color(0xFFBB86FC), radius = 24.dp.toPx(), center = center.copy(x = center.x - 30.dp.toPx(), y = center.y + 10.dp.toPx()))
            drawCircle(color = Color(0xFF03DAC5), radius = 24.dp.toPx(), center = center.copy(x = center.x + 30.dp.toPx(), y = center.y + 10.dp.toPx()))
            drawCircle(color = Color(0xFF6200EE), radius = 30.dp.toPx(), center = center.copy(y = center.y - 20.dp.toPx()))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Team is Empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Recruit cashiers and manage shift operators by clicking add below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddClick) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your First Cashier")
        }
    }
}
