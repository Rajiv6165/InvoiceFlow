package com.invoiceflow.billing.ui.screens.staff

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.SubscriptionStatus
import com.invoiceflow.billing.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionGate(
    storeId: String,
    onSignOut: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()

    LaunchedEffect(storeId) {
        viewModel.checkSubscriptionStatus(storeId)
    }

    when (val status = subscriptionStatus) {
        null -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "InvoiceFlow",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Checking license status...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        is SubscriptionStatus.Active, is SubscriptionStatus.Trial -> {
            // Grant access to the app
            content()
        }
        is SubscriptionStatus.Expired -> {
            // Block and show paywall
            PaywallScreen(
                storeId = storeId,
                daysSinceExpiry = status.daysSinceExpiry,
                onSignOut = onSignOut
            )
        }
        is SubscriptionStatus.Suspended -> {
            // Block and show suspension screen
            SuspensionScreen(
                storeId = storeId,
                reason = status.reason,
                onSignOut = onSignOut
            )
        }
        is SubscriptionStatus.Error -> {
            // Fallback screen for network or database errors
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Licensing Error",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We could not verify your store license. Please check your internet connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.checkSubscriptionStatus(storeId, forceRefresh = true) }
                    ) {
                        Text("Retry Connection")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
