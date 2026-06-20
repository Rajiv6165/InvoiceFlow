package com.invoiceflow.billing.ui.screens.staff

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.viewmodel.SubscriptionViewModel
import java.net.URLEncoder

data class PlanOption(
    val id: String,
    val name: String,
    val price: String,
    val staffLimit: String,
    val productLimit: String,
    val features: List<String>,
    val color: Color,
    val isPopular: Boolean = false
)

@Composable
fun PaywallScreen(
    storeId: String,
    daysSinceExpiry: Int,
    onSignOut: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val storeName by viewModel.storeName.collectAsState()
    
    var selectedPlanId by remember { mutableStateOf("professional") }

    val plans = listOf(
        PlanOption(
            id = "starter",
            name = "Starter",
            price = "₹999",
            staffLimit = "Up to 2 staff accounts",
            productLimit = "Up to 100 products",
            features = listOf("Standard POS billing", "PDF Invoice generation", "Basic Sales tracking"),
            color = MaterialTheme.colorScheme.secondary
        ),
        PlanOption(
            id = "professional",
            name = "Professional",
            price = "₹1,999",
            staffLimit = "Up to 5 staff accounts",
            productLimit = "Up to 500 products",
            features = listOf(
                "Everything in Starter",
                "Advanced Sales Analytics",
                "Business Intelligence dashboard",
                "Staff Shift & activity monitoring",
                "Priority email support"
            ),
            color = MaterialTheme.colorScheme.primary,
            isPopular = true
        ),
        PlanOption(
            id = "enterprise",
            name = "Enterprise",
            price = "₹3,999",
            staffLimit = "Unlimited staff accounts",
            productLimit = "Unlimited products",
            features = listOf(
                "Everything in Professional",
                "Multi-device cloud syncing",
                "Unlimited stores linkage",
                "Dedicated customer manager",
                "24/7 Phone & Chat support"
            ),
            color = Color(0xFFE5A93B) // Gold
        )
    )

    val currentPlan = plans.first { it.id == selectedPlanId }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Header
        Text(
            text = "License Expired",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your store's license expired $daysSinceExpiry ${if (daysSinceExpiry == 1) "day" else "days"} ago. Please activate a subscription plan to resume operations.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Plan Selection Header
        Text(
            text = "Choose Your Subscription Plan",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Row of Plan Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            plans.forEach { plan ->
                val isSelected = selectedPlanId == plan.id
                val borderStroke = if (isSelected) {
                    BorderStroke(3.dp, plan.color)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                }
                
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .clickable { selectedPlanId = plan.id },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    border = borderStroke,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (plan.isPopular) {
                            Row(
                                modifier = Modifier
                                    .background(plan.color, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MOST POPULAR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = plan.color
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = plan.price,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "/month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = plan.productLimit,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.staffLimit,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        plan.features.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = plan.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Order Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Request License Activation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You've selected the ${currentPlan.name} plan (${currentPlan.price}/mo). Click below to request license activation via WhatsApp support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // WhatsApp Button
                Button(
                    onClick = {
                        try {
                            val msg = "Hi, I want to activate InvoiceFlow for $storeName. Plan: ${currentPlan.name}. Store ID: $storeId"
                            val encodedMsg = URLEncoder.encode(msg, "UTF-8")
                            val whatsappUrl = "https://api.whatsapp.com/send?phone=919876543210&text=$encodedMsg"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open WhatsApp. Please call support.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366), // WhatsApp Green
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ORDER VIA WHATSAPP",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Call Button
                    OutlinedButton(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                            context.startActivity(callIntent)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Support")
                    }

                    // Email Button
                    OutlinedButton(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@invoiceflow.com")
                                putExtra(Intent.EXTRA_SUBJECT, "InvoiceFlow License Activation: $storeName")
                                putExtra(Intent.EXTRA_TEXT, "Store ID: $storeId\nRequested Plan: ${currentPlan.name}")
                            }
                            context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Support")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Out Button
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "SIGN OUT FROM THIS SESSION",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
