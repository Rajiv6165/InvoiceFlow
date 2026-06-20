package com.invoiceflow.billing.ui.screens.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.invoiceflow.billing.model.Role
import com.invoiceflow.billing.model.SubscriptionStatus
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.ui.screens.home.HomeScreen
import com.invoiceflow.billing.ui.screens.inventory.InventoryScreen
import com.invoiceflow.billing.ui.screens.pos.PosScreen
import com.invoiceflow.billing.ui.screens.profile.ProfileScreen
import com.invoiceflow.billing.ui.screens.analytics.SalesAnalyticsScreen
import com.invoiceflow.billing.ui.screens.analytics.TopProductsScreen
import com.invoiceflow.billing.ui.screens.staff.StaffManagementScreen
import com.invoiceflow.billing.ui.screens.staff.CashierProfileScreen
import com.invoiceflow.billing.ui.screens.staff.ActivityLogScreen
import com.invoiceflow.billing.ui.screens.staff.ShiftSummaryScreen
import com.invoiceflow.billing.ui.screens.staff.TrialBannerComposable
import com.invoiceflow.billing.viewmodel.StaffViewModel
import com.invoiceflow.billing.viewmodel.SubscriptionViewModel

/**
 * Navigation routes for main app
 */
object MainRoutes {
    const val HOME = "home"
    const val POS = "pos"
    const val INVENTORY = "inventory"
    const val STAFF = "staff"
    const val CASHIER_PROFILE = "cashier_profile"
    const val ACTIVITY_LOGS = "activity_logs"
    const val SHIFTS = "shifts"
    const val ANALYTICS = "analytics"
    const val TOP_PRODUCTS = "top_products"
    const val PROFILE = "profile"
}

@Composable
fun MainScreen(
    storeId: String,
    userId: String,
    userName: String,
    userRole: String,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
    staffViewModel: StaffViewModel = hiltViewModel(),
    subViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val isOwner = userRole == "OWNER"
    val startRoute = if (isOwner) MainRoutes.HOME else MainRoutes.POS
    var currentRoute by remember { mutableStateOf(startRoute) }

    val subStatus by subViewModel.subscriptionStatus.collectAsState()
    val remainingDays by subViewModel.remainingTrialDays.collectAsState()
    val isAnalyticsEnabled by subViewModel.isAnalyticsEnabled.collectAsState()
    val storeName by subViewModel.storeName.collectAsState()

    LaunchedEffect(storeId) {
        subViewModel.checkSubscriptionStatus(storeId)
    }

    // Sync StoreId with staff views
    LaunchedEffect(storeId) {
        staffViewModel.setStoreId(storeId)
    }

    // Cashiers automatically start/verify shift on login
    LaunchedEffect(userId, userName, userRole) {
        if (userRole == "CASHIER") {
            staffViewModel.startShift(userId, userName)
        }
    }

    // Intercept sign-out to close cashier shift
    val handleSignOut: () -> Unit = {
        if (userRole == "CASHIER") {
            staffViewModel.closeShift(userId, userName) {
                onSignOut()
            }
        } else {
            // Owner logouts
            staffViewModel.logEvent(
                userId = userId,
                userName = userName,
                userEmail = "",
                actionType = "USER_LOGOUT",
                details = mapOf("role" to userRole)
            )
            onSignOut()
        }
    }

    Scaffold(
        modifier = Modifier,
        bottomBar = {
            NavigationBar {
                // 1. Home (Owner Only)
                if (isOwner) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == MainRoutes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        selected = currentRoute == MainRoutes.HOME,
                        alwaysShowLabel = false,
                        onClick = {
                            currentRoute = MainRoutes.HOME
                            navController.navigate(MainRoutes.HOME) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 2. POS (All Users)
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == MainRoutes.POS) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = "POS"
                        )
                    },
                    label = { Text("POS") },
                    selected = currentRoute == MainRoutes.POS,
                    alwaysShowLabel = false,
                    onClick = {
                        currentRoute = MainRoutes.POS
                        navController.navigate(MainRoutes.POS) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 3. Inventory (Owner Only)
                if (isOwner) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == MainRoutes.INVENTORY) Icons.Filled.Inventory else Icons.Outlined.Inventory,
                                contentDescription = "Inventory"
                            )
                        },
                        label = { Text("Inventory") },
                        selected = currentRoute == MainRoutes.INVENTORY,
                        alwaysShowLabel = false,
                        onClick = {
                            currentRoute = MainRoutes.INVENTORY
                            navController.navigate(MainRoutes.INVENTORY) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 4. Staff (Owner Only)
                if (isOwner) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == MainRoutes.STAFF) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Staff"
                            )
                        },
                        label = { Text("Staff") },
                        selected = currentRoute == MainRoutes.STAFF,
                        alwaysShowLabel = false,
                        onClick = {
                            currentRoute = MainRoutes.STAFF
                            navController.navigate(MainRoutes.STAFF) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 5. Shift Summary (Cashier Only)
                if (!isOwner) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == MainRoutes.SHIFTS) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "Shift"
                            )
                        },
                        label = { Text("Shift") },
                        selected = currentRoute == MainRoutes.SHIFTS,
                        alwaysShowLabel = false,
                        onClick = {
                            currentRoute = MainRoutes.SHIFTS
                            navController.navigate(MainRoutes.SHIFTS) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 6. Analytics (Owner Only)
                if (isOwner) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == MainRoutes.ANALYTICS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Analytics") },
                        selected = currentRoute == MainRoutes.ANALYTICS || currentRoute == MainRoutes.TOP_PRODUCTS,
                        alwaysShowLabel = false,
                        onClick = {
                            currentRoute = MainRoutes.ANALYTICS
                            navController.navigate(MainRoutes.ANALYTICS) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 7. Profile (All Users)
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == MainRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    selected = currentRoute == MainRoutes.PROFILE,
                    alwaysShowLabel = false,
                    onClick = {
                        currentRoute = MainRoutes.PROFILE
                        navController.navigate(MainRoutes.PROFILE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (subStatus is SubscriptionStatus.Trial) {
                TrialBannerComposable(
                    remainingDays = remainingDays,
                    storeId = storeId,
                    storeName = storeName
                )
            }
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.weight(1f)
            ) {
            composable(MainRoutes.HOME) {
                HomeScreen(
                    storeId = storeId,
                    userName = userName,
                    onNavigateToTab = { route ->
                        currentRoute = route
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSignOut = handleSignOut
                )
            }

            composable(MainRoutes.POS) {
                PosScreen(
                    storeId = storeId,
                    userId = userId,
                    userName = userName
                )
            }

            composable(MainRoutes.INVENTORY) {
                InventoryScreen(
                    storeId = storeId
                )
            }

            composable(MainRoutes.STAFF) {
                StaffManagementScreen(
                    storeId = storeId,
                    onNavigateToProfile = { cashierId ->
                        navController.navigate("${MainRoutes.CASHIER_PROFILE}/$cashierId")
                    },
                    onNavigateToLogs = {
                        navController.navigate(MainRoutes.ACTIVITY_LOGS)
                    },
                    onNavigateToShifts = {
                        // For owner: views shifts list
                        navController.navigate(MainRoutes.SHIFTS)
                    }
                )
            }

            composable(
                route = "${MainRoutes.CASHIER_PROFILE}/{cashierId}",
                arguments = listOf(navArgument("cashierId") { type = NavType.StringType })
            ) { backStackEntry ->
                val cashierId = backStackEntry.arguments?.getString("cashierId") ?: ""
                CashierProfileScreen(
                    cashierId = cashierId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MainRoutes.ACTIVITY_LOGS) {
                ActivityLogScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MainRoutes.SHIFTS) {
                if (isOwner) {
                    // Owners view shift logs history
                    // We can reuse or show Shifts log summary inside ActivityLogScreen or customized shifts history list.
                    // For simplicity, we can render the ActivityLogScreen or a list of closed/open shifts.
                    // Let's redirect owners to ActivityLogScreen filtering by logins/logouts
                    ActivityLogScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    // Cashiers view their own active shift stats
                    ShiftSummaryScreen(
                        storeId = storeId,
                        userId = userId,
                        userName = userName,
                        onClockOut = handleSignOut
                    )
                }
            }

            composable(MainRoutes.ANALYTICS) {
                if (isAnalyticsEnabled) {
                    SalesAnalyticsScreen(
                        storeId = storeId,
                        onNavigateToTopProducts = {
                            currentRoute = MainRoutes.TOP_PRODUCTS
                            navController.navigate(MainRoutes.TOP_PRODUCTS)
                        }
                    )
                } else {
                    AnalyticsLockedOverlay(
                        storeId = storeId,
                        storeName = storeName
                    )
                }
            }

            composable(MainRoutes.TOP_PRODUCTS) {
                if (isAnalyticsEnabled) {
                    TopProductsScreen(
                        storeId = storeId,
                        onBack = {
                            currentRoute = MainRoutes.ANALYTICS
                            navController.popBackStack()
                        }
                    )
                } else {
                    AnalyticsLockedOverlay(
                        storeId = storeId,
                        storeName = storeName
                    )
                }
            }

            composable(MainRoutes.PROFILE) {
                ProfileScreen(
                    user = User(
                        userId = userId,
                        name = userName,
                        role = enumValueOf(userRole),
                        storeId = storeId
                    ),
                    onSignOut = handleSignOut,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                com.invoiceflow.billing.ui.screens.profile.SettingsScreen(
                    storeId = storeId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
}


@Composable
fun AnalyticsLockedOverlay(
    storeId: String,
    storeName: String
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background gradients
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Professional Feature Locked",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Upgrade to the Professional or Enterprise plan to unlock advanced reports, sales insights, and business intelligence dashboards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        try {
                            val msg = "Hi, I want to upgrade my store $storeName (Store ID: $storeId) to the Professional plan to unlock Analytics features."
                            val encodedMsg = java.net.URLEncoder.encode(msg, "UTF-8")
                            val whatsappUrl = "https://api.whatsapp.com/send?phone=919876543210&text=$encodedMsg"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Upgrade Plan via WhatsApp")
                }
            }
        }
    }
}
