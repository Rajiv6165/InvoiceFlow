package com.invoiceflow.billing.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardPage(
    val title: String,
    val description: String,
    val animationContent: @Composable (Modifier) -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Define slides
    val pages = remember {
        listOf(
            OnboardPage(
                title = "Bill in Seconds",
                description = "Scan any product barcode and instantly add to cart. Checkout 10x faster than manual entry.",
                animationContent = { modifier -> BarcodeScanAnimation(modifier) }
            ),
            OnboardPage(
                title = "Smart Inventory",
                description = "Track stock in real-time. Get alerts before you run out. InvoiceFlow updates inventory automatically on every sale.",
                animationContent = { modifier -> InventoryAnimation(modifier) }
            ),
            OnboardPage(
                title = "Powerful Analytics",
                description = "See exactly which products sell, when you earn the most, and how your business is growing. All in real-time.",
                animationContent = { modifier -> AnalyticsChartAnimation(modifier) }
            ),
            OnboardPage(
                title = "Cloud Powered",
                description = "Your data is safe in the cloud. Works offline. Access from any device. Never lose a single invoice.",
                animationContent = { modifier -> CloudSyncAnimation(modifier) }
            )
        )
    }
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pages.size - 1)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animation View
                    page.animationContent(
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Headline
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Body description
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Footer (Indicator + Navigation Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
                
                // Next / Get Started Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(90))
                        },
                        label = "button_text"
                    ) { lastPage ->
                        if (lastPage) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Next",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide 1 Canvas Animation: Barcode Laser Scan
 */
@Composable
fun BarcodeScanAnimation(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_loop")
    
    // Animate the laser sliding y-coordinate
    val laserYProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )
    
    // Animate subtle scanner phone scale
    val phoneScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phone_scale"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        // Draw background shadow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A237E).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = centerX * 0.8f
            )
        )
        
        // 1. Draw Barcode Lines in background
        val barcodeWidth = 160.dp.toPx()
        val barcodeHeight = 90.dp.toPx()
        val barcodeX = centerX - barcodeWidth / 2
        val barcodeY = centerY - barcodeHeight / 2
        
        val lineWeights = listOf(4f, 12f, 4f, 8f, 20f, 4f, 16f, 8f, 4f, 12f, 4f, 16f, 4f, 8f)
        var currentOffset = barcodeX + 6.dp.toPx()
        val lineSpacing = 6.dp.toPx()
        
        lineWeights.forEach { weight ->
            val strokeWidth = weight.dp.toPx()
            if (currentOffset + strokeWidth < barcodeX + barcodeWidth) {
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.4f),
                    start = Offset(currentOffset, barcodeY),
                    end = Offset(currentOffset, barcodeY + barcodeHeight),
                    strokeWidth = strokeWidth
                )
                currentOffset += strokeWidth + lineSpacing
            }
        }
        
        // Draw barcode border outline
        drawRoundRect(
            color = Color.LightGray.copy(alpha = 0.6f),
            topLeft = Offset(barcodeX, barcodeY),
            size = Size(barcodeWidth, barcodeHeight),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 2. Draw Scanner Phone Border Frame
        val phoneWidth = 110.dp.toPx() * phoneScale
        val phoneHeight = 190.dp.toPx() * phoneScale
        val phoneX = centerX - phoneWidth / 2
        val phoneY = centerY - phoneHeight / 2
        
        // Draw phone silhouette
        drawRoundRect(
            color = Color(0xFF1A237E),
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Phone screen notch
        drawRoundRect(
            color = Color(0xFF1A237E),
            topLeft = Offset(centerX - 20.dp.toPx(), phoneY + 4.dp.toPx()),
            size = Size(40.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Phone screen scanning box
        val scanBoxWidth = 80.dp.toPx() * phoneScale
        val scanBoxHeight = 80.dp.toPx() * phoneScale
        val scanBoxX = centerX - scanBoxWidth / 2
        val scanBoxY = centerY - scanBoxHeight / 2
        
        drawRoundRect(
            color = Color(0xFF00897B).copy(alpha = 0.3f),
            topLeft = Offset(scanBoxX, scanBoxY),
            size = Size(scanBoxWidth, scanBoxHeight),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 3. Draw Laser Scan Line
        val currentLaserY = scanBoxY + (scanBoxHeight * laserYProgress)
        drawLine(
            color = Color(0xFFC62828),
            start = Offset(scanBoxX - 4.dp.toPx(), currentLaserY),
            end = Offset(scanBoxX + scanBoxWidth + 4.dp.toPx(), currentLaserY),
            strokeWidth = 3.dp.toPx()
        )
        
        // Draw glowing gradient shadow behind laser
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFC62828).copy(alpha = 0.25f), Color.Transparent),
                startY = currentLaserY,
                endY = currentLaserY + 15.dp.toPx()
            ),
            topLeft = Offset(scanBoxX, currentLaserY),
            size = Size(scanBoxWidth, 15.dp.toPx())
        )
    }
}

/**
 * Slide 2 Canvas Animation: Inventory stock levels & alerts
 */
@Composable
fun InventoryAnimation(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "inventory_loop")
    
    // Animate box scales (pop in sequence)
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "box_scale_1"
    )
    
    // Animate warning badge alpha
    val badgeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_alpha"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00897B).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = centerX * 0.8f
            )
        )
        
        val boxSize = 50.dp.toPx()
        val shelfWidth = 180.dp.toPx()
        val shelfHeight = 8.dp.toPx()
        
        // Draw Shelves
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX - shelfWidth / 2, centerY + 30.dp.toPx()),
            end = Offset(centerX + shelfWidth / 2, centerY + 30.dp.toPx()),
            strokeWidth = shelfHeight
        )
        
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX - shelfWidth / 2, centerY - 45.dp.toPx()),
            end = Offset(centerX + shelfWidth / 2, centerY - 45.dp.toPx()),
            strokeWidth = shelfHeight
        )
        
        // 1. Draw top shelf boxes (Okay stock)
        // Green box
        drawRoundRect(
            color = Color(0xFF00897B).copy(alpha = 0.8f),
            topLeft = Offset(centerX - 60.dp.toPx(), centerY - 45.dp.toPx() - boxSize),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        
        // Blue box
        drawRoundRect(
            color = Color(0xFF1A237E).copy(alpha = 0.8f),
            topLeft = Offset(centerX + 10.dp.toPx(), centerY - 45.dp.toPx() - boxSize),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        
        // 2. Draw bottom shelf boxes (One low stock!)
        // Green Box
        drawRoundRect(
            color = Color(0xFF00897B).copy(alpha = 0.8f),
            topLeft = Offset(centerX - 70.dp.toPx(), centerY + 30.dp.toPx() - boxSize),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        
        // Empty/Low-Stock Red Box (animated scale)
        val alertBoxSize = boxSize * scale1
        val alertBoxX = centerX + 15.dp.toPx() + (boxSize - alertBoxSize) / 2
        val alertBoxY = centerY + 30.dp.toPx() - alertBoxSize
        
        drawRoundRect(
            color = Color(0xFFC62828).copy(alpha = 0.8f),
            topLeft = Offset(alertBoxX, alertBoxY),
            size = Size(alertBoxSize, alertBoxSize),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        
        // Draw warning exclamation badge
        val badgeRadius = 10.dp.toPx()
        val badgeX = alertBoxX + alertBoxSize
        val badgeY = alertBoxY
        
        drawCircle(
            color = Color(0xFFF57F17).copy(alpha = badgeAlpha),
            center = Offset(badgeX, badgeY),
            radius = badgeRadius
        )
        
        // Exclamation Mark inside badge
        drawLine(
            color = Color.White.copy(alpha = badgeAlpha),
            start = Offset(badgeX, badgeY - 5.dp.toPx()),
            end = Offset(badgeX, badgeY + 1.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(
            color = Color.White.copy(alpha = badgeAlpha),
            center = Offset(badgeX, badgeY + 4.dp.toPx()),
            radius = 1.dp.toPx()
        )
    }
}

/**
 * Slide 3 Canvas Animation: Growth charts drawing
 */
@Composable
fun AnalyticsChartAnimation(modifier: Modifier) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }
    
    val transition = rememberTransition(transitionState, label = "chart_transition")
    
    val lineProgress by transition.animateFloat(
        transitionSpec = { tween(2000, easing = FastOutSlowInEasing) },
        label = "line_path"
    ) { state ->
        if (state) 1f else 0f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        // Draw grid lines
        val padding = 40.dp.toPx()
        val graphWidth = width - padding * 2
        val graphHeight = height - padding * 2
        
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + (graphHeight / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw Axis
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2.dp.toPx()
        )
        
        // Draw Sales Bars
        val barPoints = listOf(0.2f, 0.45f, 0.35f, 0.65f, 0.55f, 0.85f)
        val barCount = barPoints.size
        val barSpacing = graphWidth / barCount
        
        barPoints.forEachIndexed { index, value ->
            val barHeight = graphHeight * value * lineProgress
            val barX = padding + barSpacing * index + (barSpacing - 16.dp.toPx()) / 2
            val barY = height - padding - barHeight
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00897B), Color(0xFF00897B).copy(alpha = 0.4f))
                ),
                topLeft = Offset(barX, barY),
                size = Size(16.dp.toPx(), barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
        
        // Draw Line Chart overlay (Spring path drawing)
        val linePoints = listOf(0.15f, 0.35f, 0.30f, 0.70f, 0.50f, 0.95f)
        val path = Path()
        
        linePoints.forEachIndexed { index, value ->
            val ptX = padding + barSpacing * index + barSpacing / 2
            val ptY = height - padding - (graphHeight * value)
            
            if (index == 0) {
                path.moveTo(ptX, ptY)
            } else {
                path.lineTo(ptX, ptY)
            }
        }
        
        // Draw path with progress animation
        drawPath(
            path = path,
            color = Color(0xFF1A237E),
            style = Stroke(
                width = 4.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(pathLength(path), pathLength(path)),
                    phase = pathLength(path) * (1f - lineProgress)
                )
            )
        )
        
        // Draw line points / dots
        linePoints.forEachIndexed { index, value ->
            val ptX = padding + barSpacing * index + barSpacing / 2
            val ptY = height - padding - (graphHeight * value)
            
            // Draw dot if progress has reached it
            val pointProgress = lineProgress * barCount
            if (index <= pointProgress) {
                drawCircle(
                    color = Color(0xFF1A237E),
                    center = Offset(ptX, ptY),
                    radius = 5.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(ptX, ptY),
                    radius = 2.dp.toPx()
                )
            }
        }
    }
}

// Approximate path length helper
private fun pathLength(path: Path): Float {
    return 1000f // Return high-enough default for dash phase bounds
}

/**
 * Slide 4 Canvas Animation: Cloud sync database & client sync
 */
@Composable
fun CloudSyncAnimation(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_loop")
    
    // Rotate sync arrows
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrow_rotation"
    )
    
    // Wave client connection signals
    val signalWave by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_wave"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        // Draw ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A237E).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = centerX * 0.8f
            )
        )
        
        // Draw cloud outline in center
        val cloudWidth = 100.dp.toPx()
        val cloudHeight = 60.dp.toPx()
        val cloudX = centerX - cloudWidth / 2
        val cloudY = centerY - cloudHeight / 2
        
        // Drawing beautiful cloud from overlapping circles
        drawCircle(color = Color(0xFF1A237E).copy(alpha = 0.15f), center = Offset(centerX, centerY + 10.dp.toPx()), radius = 30.dp.toPx())
        drawCircle(color = Color(0xFF1A237E).copy(alpha = 0.15f), center = Offset(centerX - 24.dp.toPx(), centerY + 12.dp.toPx()), radius = 22.dp.toPx())
        drawCircle(color = Color(0xFF1A237E).copy(alpha = 0.15f), center = Offset(centerX + 24.dp.toPx(), centerY + 12.dp.toPx()), radius = 22.dp.toPx())
        drawCircle(color = Color(0xFF1A237E).copy(alpha = 0.15f), center = Offset(centerX - 10.dp.toPx(), centerY - 10.dp.toPx()), radius = 24.dp.toPx())
        drawCircle(color = Color(0xFF1A237E).copy(alpha = 0.15f), center = Offset(centerX + 15.dp.toPx(), centerY - 5.dp.toPx()), radius = 20.dp.toPx())
        
        // Main solid Cloud base
        val cloudPath = Path().apply {
            moveTo(centerX - 40.dp.toPx(), centerY + 25.dp.toPx())
            lineTo(centerX + 40.dp.toPx(), centerY + 25.dp.toPx())
            
            // Right-side curve
            cubicTo(
                centerX + 55.dp.toPx(), centerY + 25.dp.toPx(),
                centerX + 55.dp.toPx(), centerY,
                centerX + 40.dp.toPx(), centerY
            )
            // Top curves
            cubicTo(
                centerX + 35.dp.toPx(), centerY - 25.dp.toPx(),
                centerX - 5.dp.toPx(), centerY - 25.dp.toPx(),
                centerX - 10.dp.toPx(), centerY - 10.dp.toPx()
            )
            cubicTo(
                centerX - 20.dp.toPx(), centerY - 20.dp.toPx(),
                centerX - 45.dp.toPx(), centerY - 15.dp.toPx(),
                centerX - 40.dp.toPx(), centerY + 5.dp.toPx()
            )
            // Left curve
            cubicTo(
                centerX - 55.dp.toPx(), centerY + 5.dp.toPx(),
                centerX - 55.dp.toPx(), centerY + 25.dp.toPx(),
                centerX - 40.dp.toPx(), centerY + 25.dp.toPx()
            )
        }
        
        drawPath(path = cloudPath, color = Color(0xFF1A237E))
        
        // Draw 3 Connected Client Devices
        val clientPositions = listOf(
            Offset(centerX - 90.dp.toPx(), centerY + 60.dp.toPx()),  // Mobile Phone
            Offset(centerX + 90.dp.toPx(), centerY + 60.dp.toPx()),  // Tablet
            Offset(centerX, centerY - 80.dp.toPx())                  // Server/Computer
        )
        
        clientPositions.forEach { pos ->
            // Draw connection lines with dotted animated effect
            drawLine(
                color = Color(0xFF00897B).copy(alpha = signalWave),
                start = Offset(centerX, centerY + 10.dp.toPx()),
                end = pos,
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                    phase = rotation
                )
            )
            
            // Draw device node shapes
            drawCircle(
                color = Color(0xFF00897B),
                center = pos,
                radius = 12.dp.toPx()
            )
            drawCircle(
                color = Color.White,
                center = pos,
                radius = 8.dp.toPx()
            )
            drawCircle(
                color = Color(0xFF00897B),
                center = pos,
                radius = 4.dp.toPx()
            )
        }
        
        // Draw rotating arrows directly inside the Cloud shape
        // For visual clarity, let's draw two rotating arrows in the center
        val arrowRadius = 14.dp.toPx()
        val arrowCenter = Offset(centerX, centerY + 5.dp.toPx())
        
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            center = arrowCenter,
            radius = arrowRadius + 4.dp.toPx()
        )
        
        // Draw simple revolving arcs/sync symbol
        withTransform({
            rotate(rotation, arrowCenter)
        }) {
            drawArc(
                color = Color(0xFF1A237E),
                startAngle = 45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(arrowCenter.x - arrowRadius, arrowCenter.y - arrowRadius),
                size = Size(arrowRadius * 2, arrowRadius * 2),
                style = Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = Color(0xFF1A237E),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(arrowCenter.x - arrowRadius, arrowCenter.y - arrowRadius),
                size = Size(arrowRadius * 2, arrowRadius * 2),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Draw small arrows heads
            drawCircle(
                color = Color(0xFF1A237E),
                center = Offset(arrowCenter.x + arrowRadius, arrowCenter.y),
                radius = 3.dp.toPx()
            )
            drawCircle(
                color = Color(0xFF1A237E),
                center = Offset(arrowCenter.x - arrowRadius, arrowCenter.y),
                radius = 3.dp.toPx()
            )
        }
    }
}
