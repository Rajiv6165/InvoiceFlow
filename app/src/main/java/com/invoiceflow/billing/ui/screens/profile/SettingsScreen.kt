@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.invoiceflow.billing.ui.screens.profile

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.ui.theme.Dimensions
import com.invoiceflow.billing.util.LocalDataStore.StoreSettings
import com.invoiceflow.billing.viewmodel.SettingsViewModel
import java.util.*

@Composable
fun SettingsScreen(
    storeId: String,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val scrollState = rememberScrollState()
    
    var showClearCacheDialog by remember { mutableStateOf(false) }
    
    // Logo Picker Launcher
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.updateSettings(settings.copy(logoPath = uri.toString()))
            Toast.makeText(context, "Logo updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Invoice Header Preview Box
            LiveInvoiceHeaderPreview(settings = settings)
            
            // 1. Store Profile Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Store Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Divider()
                    
                    // Logo Upload Button / View
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable { logoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.logoPath.isNotBlank()) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text("Store Logo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Upload logo for receipt headers", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    
                    // Store Name
                    OutlinedTextField(
                        value = settings.storeName,
                        onValueChange = { viewModel.updateSettings(settings.copy(storeName = it)) },
                        label = { Text("Store Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Store Address
                    OutlinedTextField(
                        value = settings.storeAddress,
                        onValueChange = { viewModel.updateSettings(settings.copy(storeAddress = it)) },
                        label = { Text("Store Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // GST Number
                    OutlinedTextField(
                        value = settings.gstNumber,
                        onValueChange = { viewModel.updateSettings(settings.copy(gstNumber = it.uppercase())) },
                        label = { Text("GSTIN (GST Number)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // GST Rate Slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Default GST Rate", style = MaterialTheme.typography.bodyMedium)
                            Text("${settings.gstRate.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.gstRate.toFloat(),
                            onValueChange = { viewModel.updateSettings(settings.copy(gstRate = it.toDouble())) },
                            valueRange = 0f..28f,
                            steps = 27
                        )
                        
                        // Preset GST Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { preset ->
                                SuggestionChip(
                                    onClick = { viewModel.updateSettings(settings.copy(gstRate = preset)) },
                                    label = { Text("${preset.toInt()}%") },
                                    colors = if (settings.gstRate == preset) {
                                        SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        SuggestionChipDefaults.suggestionChipColors()
                                    }
                                )
                            }
                        }
                    }
                    
                    // Currency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Business Currency", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("INR" to "₹", "USD" to "$").forEach { (code, symbol) ->
                                FilterChip(
                                    selected = settings.currency == code,
                                    onClick = { viewModel.updateSettings(settings.copy(currency = code)) },
                                    label = { Text("$code ($symbol)") }
                                )
                            }
                        }
                    }
                }
            }
            
            // 2. Receipt Customization Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Receipt Customization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Divider()
                    
                    // Paper Size toggle Segment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paper Size", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            listOf("80mm" to "Thermal (80mm)", "A4" to "Standard Page (A4)").forEach { (value, label) ->
                                val selected = settings.paperSize == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { viewModel.updateSettings(settings.copy(paperSize = value)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Custom Footer Message
                    OutlinedTextField(
                        value = settings.receiptFooter,
                        onValueChange = { viewModel.updateSettings(settings.copy(receiptFooter = it)) },
                        label = { Text("Receipt Footer Message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Show GST Breakdown toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GST Breakdown", fontWeight = FontWeight.Bold)
                            Text("Show tax details per-item on receipts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = settings.showGstBreakdown,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(showGstBreakdown = it)) }
                        )
                    }
                    
                    // Show Barcode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Print Barcodes", fontWeight = FontWeight.Bold)
                            Text("Show product barcode details", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = settings.showBarcode,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(showBarcode = it)) }
                        )
                    }
                }
            }
            
            // 3. Notification Settings Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Alerts & Notifications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Divider()
                    
                    // Daily Summary Notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Business Summary", fontWeight = FontWeight.Bold)
                            Text("Receive sales summaries daily", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = settings.dailySummaryNotif,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(dailySummaryNotif = it)) }
                        )
                    }
                    
                    // Time Picker (if summary notification is on)
                    if (settings.dailySummaryNotif) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parts = settings.dailySummaryTime.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    
                                    TimePickerDialog(
                                        context,
                                        { _, selectedHour, selectedMinute ->
                                            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            viewModel.updateSettings(settings.copy(dailySummaryTime = formattedTime))
                                        },
                                        hour,
                                        min,
                                        true
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Summary Alerts Time", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = settings.dailySummaryTime,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    
                    // Low Stock Alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Low Stock Alerts", fontWeight = FontWeight.Bold)
                            Text("Notify when stock counts drop", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = settings.lowStockAlert,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(lowStockAlert = it)) }
                        )
                    }
                    
                    // Low Stock threshold Slider
                    if (settings.lowStockAlert) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Low Stock Threshold Level", style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.lowStockThreshold} units", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = settings.lowStockThreshold.toFloat(),
                                onValueChange = { viewModel.updateSettings(settings.copy(lowStockThreshold = it.toInt())) },
                                valueRange = 1f..50f,
                                steps = 49
                            )
                        }
                    }
                    
                    // Subscription reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Plan Expiry Reminders", fontWeight = FontWeight.Bold)
                            Text("Alerts on upcoming subscription checks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = settings.subExpiryAlert,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(subExpiryAlert = it)) }
                        )
                    }
                }
            }
            
            // 4. Data Management & Sync Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Data & Cloud Management", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Divider()
                    
                    // Last sync info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Database Backup", style = MaterialTheme.typography.bodyMedium)
                        Text("Just now", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    
                    // Export CSV Button
                    Button(
                        onClick = {
                            viewModel.exportProductsToCsv(context, storeId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Products CSV exported successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to export data CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Products as CSV")
                    }
                    
                    // Clear Cache / Reset preferences Button
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Local Cache / Reset Session")
                    }
                }
            }
            
            // 5. About Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("About InvoiceFlow", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Version", style = MaterialTheme.typography.bodyMedium)
                        Text("v3.0.0 (Build 3020)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    // Privacy Policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://invoiceflow.com/privacy"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    
                    // Terms
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://invoiceflow.com/terms"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Terms of Service", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    
                    // WhatsApp Support
                    Button(
                        onClick = {
                            try {
                                val msg = "Hi, I need support with my InvoiceFlow app (Store ID: $storeId)."
                                val url = "https://api.whatsapp.com/send?phone=919876543210&text=${Uri.encode(msg)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContactSupport, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact Support (WhatsApp)", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Session") },
            text = { Text("Are you sure you want to clear your local session? This will sign you out and reset onboarding preferences.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearLocalPreferences {
                            Toast.makeText(context, "Preferences cleared", Toast.LENGTH_SHORT).show()
                            // Force app restart or exit
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear & Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Beautiful Live Invoice Header Preview Composable
 */
@Composable
fun LiveInvoiceHeaderPreview(settings: StoreSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live Invoice Header Preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Divider()
            
            // Mock logo circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Mock store details based on current settings values
            Text(
                text = settings.storeName.ifBlank { "My Retail Store" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = settings.storeAddress.ifBlank { "123 Business Street, Shopping District" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (settings.gstNumber.isNotBlank()) {
                Text(
                    text = "GSTIN: ${settings.gstNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Mock receipt body
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sample Item x 1", style = MaterialTheme.typography.bodySmall)
                Text(if (settings.currency == "INR") "₹100.00" else "$100.00", style = MaterialTheme.typography.bodySmall)
            }
            if (settings.showGstBreakdown) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GST (${settings.gstRate.toInt()}%) included", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(if (settings.currency == "INR") "₹${String.format("%.2f", 100 * settings.gstRate / 100)}" else "$${String.format("%.2f", 100 * settings.gstRate / 100)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text(
                text = settings.receiptFooter.ifBlank { "Thank you for shopping!" },
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
