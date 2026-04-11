package com.invoiceflow.billing.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.ui.screens.inventory.InventoryScreen
import com.invoiceflow.billing.ui.screens.pos.PosScreen
import com.invoiceflow.billing.ui.screens.profile.ProfileScreen

/**
 * Navigation routes for main app
 */
object MainRoutes {
    const val POS = "pos"
    const val INVENTORY = "inventory"
    const val PROFILE = "profile"
}

@Composable
fun MainScreen(
    storeId: String,
    userId: String,
    userName: String,
    userRole: String,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    var currentRoute by remember { mutableStateOf(MainRoutes.POS) }
    
    Scaffold(
        modifier = Modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == MainRoutes.POS) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "POS"
                        )
                    },
                    label = { Text("POS") },
                    selected = currentRoute == MainRoutes.POS,
                    onClick = {
                        currentRoute = MainRoutes.POS
                        navController.navigate(MainRoutes.POS) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == MainRoutes.INVENTORY) Icons.Filled.Inventory else Icons.Outlined.Inventory,
                            contentDescription = "Inventory"
                        )
                    },
                    label = { Text("Inventory") },
                    selected = currentRoute == MainRoutes.INVENTORY,
                    onClick = {
                        currentRoute = MainRoutes.INVENTORY
                        navController.navigate(MainRoutes.INVENTORY) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    enabled = userRole == "OWNER" // Only owners can manage inventory
                )
                
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == MainRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    selected = currentRoute == MainRoutes.PROFILE,
                    onClick = {
                        currentRoute = MainRoutes.PROFILE
                        navController.navigate(MainRoutes.PROFILE) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = MainRoutes.POS,
            modifier = Modifier.padding(paddingValues)
        ) {
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
            
            composable(MainRoutes.PROFILE) {
                ProfileScreen(
                    user = User(
                        userId = userId,
                        name = userName,
                        role = enumValueOf(userRole),
                        storeId = storeId
                    ),
                    onSignOut = onSignOut
                )
            }
        }
    }
}
