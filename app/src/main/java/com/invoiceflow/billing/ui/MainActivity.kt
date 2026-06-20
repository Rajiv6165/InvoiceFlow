package com.invoiceflow.billing.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.ui.screens.login.LoginScreen
import com.invoiceflow.billing.ui.screens.staff.SubscriptionGate
import com.invoiceflow.billing.ui.screens.main.MainScreen
import com.invoiceflow.billing.ui.screens.register.RegistrationScreen
import com.invoiceflow.billing.ui.theme.GhostgridTheme
import com.invoiceflow.billing.util.AppUtil
import com.invoiceflow.billing.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.invoiceflow.billing.ui.screens.login.OnboardingScreen
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AnticipateInterpolator

/**
 * Navigation routes
 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Android 12+ SplashScreen API
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Configure smooth exit animation for system splash screen
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            val iconView = splashScreenViewProvider.iconView
            
            // Icon scales and fades out
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.3f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.3f)
            val iconAlpha = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)
            
            // Splash background fades out
            val bgAlpha = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)
            
            val animatorSet = AnimatorSet().apply {
                playTogether(scaleX, scaleY, iconAlpha, bgAlpha)
                duration = 450
                interpolator = AnticipateInterpolator()
            }
            
            animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    splashScreenViewProvider.remove()
                }
            })
            animatorSet.start()
        }
        
        // Initialize AppUtil
        AppUtil.init(application)
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            GhostgridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(authViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    // Observe auth state with security guard
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Check initial auth state
                isLoggedIn = authViewModel.isLoggedIn()
                currentUser = authViewModel.getCurrentUser()
                
                // Listen for auth state changes
                authViewModel.observeAuthState().collect { firebaseUser ->
                    isLoggedIn = firebaseUser != null
                    
                    // Update current user from Firestore
                    currentUser = if (firebaseUser != null) {
                        authViewModel.getCurrentUser()
                    } else {
                        null
                    }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Splash Screen (animated Compose entry)
        composable(Routes.SPLASH) {
            val isOnboardingCompletedVal by authViewModel.isOnboardingCompleted.collectAsState()
            SplashScreen(
                onComplete = {
                    if (!isOnboardingCompletedVal) {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else if (isLoggedIn && currentUser != null) {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Onboarding Screen
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    authViewModel.completeOnboarding()
                    if (isLoggedIn && currentUser != null) {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Login Screen
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        
        // Registration Screen
        composable(Routes.REGISTER) {
            RegistrationScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main Screen (POS, Inventory, Profile)
        composable(Routes.MAIN) {
            currentUser?.let { user ->
                SubscriptionGate(
                    storeId = user.storeId,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                ) {
                    MainScreen(
                        storeId = user.storeId,
                        userId = user.userId,
                        userName = user.name,
                        userRole = user.role.name,
                        onSignOut = {
                            authViewModel.signOut()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.MAIN) { inclusive = true }
                            }
                        }
                    )
                }
            } ?: run {
                // Security guard: If user becomes null, navigate to login
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    onComplete: () -> Unit
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.5f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val offsetY = remember { androidx.compose.animation.core.Animatable(50f) }
    val taglineAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // App icon scale + fade
        kotlinx.coroutines.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
        kotlinx.coroutines.launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 600)
            )
        }
        
        // Wait briefly for icon, then slide up "InvoiceFlow"
        kotlinx.coroutines.delay(400)
        kotlinx.coroutines.launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
        }
        
        // Fade in tagline
        kotlinx.coroutines.delay(500)
        kotlinx.coroutines.launch {
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
            )
        }
        
        // Complete after 2.5 seconds total
        kotlinx.coroutines.delay(1600)
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF1A237E),
                        androidx.compose.ui.graphics.Color(0xFF283593)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated App Icon Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .background(
                        color = androidx.compose.ui.graphics.Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = androidx.compose.ui.graphics.Color(0xFF1A237E)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // "InvoiceFlow" Text with spring slide-up
            Text(
                text = "InvoiceFlow",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier
                    .offset(y = offsetY.value.dp)
                    .alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline fades in
            Text(
                text = "Smart Billing for Smart Business",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
