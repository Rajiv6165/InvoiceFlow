package com.invoiceflow.billing.ui.screens.staff

import android.util.Patterns
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.viewmodel.StaffViewModel
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashierBottomSheet(
    onDismiss: () -> Unit,
    onCashierCreated: (User, String) -> Unit,
    viewModel: StaffViewModel
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedShift by remember { mutableStateOf("Any") }
    
    var autoGeneratePassword by remember { mutableStateOf(true) }
    var manualPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var submissionError by remember { mutableStateOf<String?>(null) }

    // Validation checks
    val isEmailValid = email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isNameTooLong = name.length > 50

    val passwordStrength = getPasswordStrength(manualPassword)
    
    val canSubmit = name.isNotBlank() && !isNameTooLong &&
            email.isNotBlank() && isEmailValid &&
            (autoGeneratePassword || manualPassword.length >= 6) &&
            !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Staff Member",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (submissionError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = submissionError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Required", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${name.length}/50",
                            color = if (isNameTooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                isError = isNameTooLong,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email Address *") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = !isEmailValid,
                supportingText = {
                    if (!isEmailValid) {
                        Text(text = "Enter a valid email address", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(text = "Used for cashier login")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15) phone = it },
                label = { Text("Phone Number (Optional)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // Shift Assignment
            Column {
                Text(
                    text = "Shift Assignment",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shifts = listOf("Morning", "Afternoon", "Night", "Any")
                    shifts.forEach { shift ->
                        val selected = selectedShift == shift
                        FilterChip(
                            selected = selected,
                            onClick = { selectedShift = shift },
                            label = { Text(shift) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Password Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoGeneratePassword,
                        onCheckedChange = { autoGeneratePassword = it }
                    )
                    Text(
                        text = "Auto-generate secure password",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!autoGeneratePassword) {
                    OutlinedTextField(
                        value = manualPassword,
                        onValueChange = { manualPassword = it },
                        label = { Text("Choose Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Min 6 characters")
                                if (manualPassword.isNotEmpty()) {
                                    PasswordStrengthMeter(strength = passwordStrength)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    submissionError = null
                    
                    val finalPassword = if (autoGeneratePassword) {
                        generateRandomPassword()
                    } else {
                        manualPassword
                    }

                    viewModel.createCashier(
                        name = name,
                        email = email,
                        password = finalPassword,
                        phone = phone,
                        shift = selectedShift,
                        onSuccess = { newUser ->
                            isSubmitting = false
                            onCashierCreated(newUser, finalPassword)
                        },
                        onError = { error ->
                            isSubmitting = false
                            submissionError = error
                        }
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Registering Cashier...")
                } else {
                    Text("Create Cashier Account", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class Strength(val label: String, val color: Color, val progress: Float) {
    WEAK("Weak", Color(0xFFC62828), 0.25f),
    FAIR("Fair", Color(0xFFEF6C00), 0.5f),
    STRONG("Strong", Color(0xFFFBC02D), 0.75f),
    VERY_STRONG("Very Strong", Color(0xFF2E7D32), 1f)
}

private fun getPasswordStrength(password: String): Strength {
    if (password.length < 6) return Strength.WEAK
    
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        0, 1 -> Strength.FAIR
        2, 3 -> Strength.STRONG
        else -> Strength.VERY_STRONG
    }
}

@Composable
fun PasswordStrengthMeter(strength: Strength) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Password Strength: ${strength.label}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = strength.progress,
            color = strength.color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )
    }
}

private fun generateRandomPassword(): String {
    val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%"
    val random = SecureRandom()
    val bytes = ByteArray(10)
    random.nextBytes(bytes)
    return (1..10)
        .map { random.nextInt(charPool.length) }
        .map (charPool::get)
        .joinToString("")
}
