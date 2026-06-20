package com.invoiceflow.billing.ui.screens.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.ui.screens.main.MainRoutes
import com.invoiceflow.billing.util.PdfGeneratorUtil
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    storeId: String,
    userName: String,
    onNavigateToTab: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configure storeId in ReportsViewModel
    LaunchedEffect(storeId) {
        viewModel.setStoreId(storeId)
    }

    // State bindings
    val currentStore by viewModel.currentStore.collectAsState()
    val todaysSalesResult by viewModel.todaysSalesState.collectAsState()
    val last7DaysTrendResult by viewModel.last7DaysTrendState.collectAsState()
    val recentTransactionsResult by viewModel.recentTransactionsState.collectAsState()
    val activeProductsCount by viewModel.activeProductsCount.collectAsState()
    val lowStockProductsCount by viewModel.lowStockProductsCount.collectAsState()

    var isSharingSummary by remember { mutableStateOf(false) }

    // Live clock state
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // Pull to Refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val refreshThreshold = 200f

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && dragOffset < refreshThreshold) {
                    dragOffset += available.y * 0.4f // Friction
                    return Offset(0f, available.y)
                }
                if (available.y < 0 && dragOffset > 0) {
                    val consumed = Math.min(-available.y, dragOffset)
                    dragOffset -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.refreshData(force = true)
            isRefreshing = false
            dragOffset = 0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "InvoiceFlow Dashboard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data"
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Drag triggers refresh
            if (dragOffset > 50f) {
                LinearProgressIndicator(
                    progress = (dragOffset / refreshThreshold).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (dragOffset >= refreshThreshold && !isRefreshing) {
                    LaunchedEffect(Unit) {
                        isRefreshing = true
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Greeting Card
                item {
                    GreetingCard(
                        ownerName = userName,
                        storeName = currentStore?.name ?: "Loading Store...",
                        clockTime = currentTime
                    )
                }

                // 2. Metrics Title
                item {
                    Text(
                        text = "Today's Performance Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 3. Metric Cards Row
                item {
                    val todaysSales = todaysSalesResult.getOrNull()
                    val last7DaysTrend = last7DaysTrendResult.getOrNull() ?: emptyList()
                    
                    val revenueData = last7DaysTrend.map { it.revenue }
                    val billsData = last7DaysTrend.map { it.billCount.toDouble() }

                    // Mock data fallbacks for sparklines if empty
                    val revenueSparkline = if (revenueData.size >= 2) revenueData else listOf(0.0, 1.0, 0.5, 2.0, 1.5, 3.0, 2.5)
                    val billsSparkline = if (billsData.size >= 2) billsData else listOf(0.0, 2.0, 1.0, 4.0, 3.0, 5.0, 4.0)
                    val productsSparkline = listOf(10.0, 11.0, 12.0, 12.0, 14.0, 15.0, 15.0)
                    val stockAlertSparkline = listOf(5.0, 4.0, 6.0, 3.0, 4.0, 2.0, 3.0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Revenue card
                        MetricCard(
                            title = "Today's Revenue",
                            value = "₹${String.format("%.2f", todaysSales?.totalRevenue ?: 0.0)}",
                            subText = "${String.format("%.1f", todaysSales?.revenueTrendPercent ?: 0.0)}% vs yesterday",
                            trendUp = todaysSales?.revenueTrendUp ?: true,
                            sparklineData = revenueSparkline,
                            sparklineColor = Color(0xFF2E7D32)
                        )

                        // Bills card
                        MetricCard(
                            title = "Today's Bills",
                            value = "${todaysSales?.billCount ?: 0}",
                            subText = "${String.format("%.1f", todaysSales?.billsTrendPercent ?: 0.0)}% vs yesterday",
                            trendUp = todaysSales?.billsTrendUp ?: true,
                            sparklineData = billsSparkline,
                            sparklineColor = Color(0xFF1976D2)
                        )

                        // Active products card
                        MetricCard(
                            title = "Active Products",
                            value = "$activeProductsCount",
                            subText = "Products cataloged",
                            trendUp = null,
                            sparklineData = productsSparkline,
                            sparklineColor = Color(0xFFBB86FC)
                        )

                        // Low stock card
                        MetricCard(
                            title = "Low Stock Alerts",
                            value = "$lowStockProductsCount",
                            subText = "Items need restocking",
                            trendUp = null,
                            sparklineData = stockAlertSparkline,
                            badgeCount = lowStockProductsCount,
                            sparklineColor = Color(0xFFB00020)
                        )
                    }
                }

                // 4. Quick Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions Command Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuickActionButton(
                                    icon = Icons.Default.ShoppingCart,
                                    label = "New Bill",
                                    onClick = { onNavigateToTab(MainRoutes.POS) }
                                )
                                QuickActionButton(
                                    icon = Icons.Default.AddBox,
                                    label = "Add Product",
                                    onClick = { onNavigateToTab(MainRoutes.INVENTORY) }
                                )
                                QuickActionButton(
                                    icon = Icons.Default.BarChart,
                                    label = "View Reports",
                                    onClick = { onNavigateToTab(MainRoutes.ANALYTICS) }
                                )
                                QuickActionButton(
                                    icon = Icons.Default.Share,
                                    label = "Share Summary",
                                    onClick = {
                                        if (!isSharingSummary) {
                                            isSharingSummary = true
                                            shareDailySummary(
                                                context = context,
                                                storeId = storeId,
                                                store = currentStore,
                                                todaysSales = todaysSalesResult.getOrNull(),
                                                viewModel = viewModel,
                                                onComplete = { isSharingSummary = false }
                                            )
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 5. Recent Activity Feed Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Activity Feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 6. Recent Activity Feed Items
                when (recentTransactionsResult) {
                    is Result.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is Result.Error -> {
                        item {
                            Text(
                                text = "Error loading activity feed: ${(recentTransactionsResult as Result.Error).message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is Result.Success -> {
                        val transactions = (recentTransactionsResult as Result.Success<List<Invoice>>).data
                        
                        if (transactions.isEmpty()) {
                            item {
                                Text(
                                    text = "No transactions today yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = transactions,
                                key = { _, invoice -> invoice.invoiceId }
                            ) { _, invoice ->
                                TransactionRow(invoice = invoice)
                            }
                        }
                    }
                }
                
                // Extra padding bottom
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isSharingSummary) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Compiling Business Summary...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingCard(
    ownerName: String,
    storeName: String,
    clockTime: String
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    
    val firstName = ownerName.split(" ").firstOrNull() ?: ownerName
    val todayFormatted = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1976D2), // primary brand blue
                            Color(0xFF1565C0)  // variant dark blue
                        )
                    )
                )
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = storeName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$greeting, $firstName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider(color = Color.White.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = clockTime,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    trendUp: Boolean? = null,
    sparklineData: List<Double>,
    badgeCount: Int = 0,
    sparklineColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (trendUp != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (trendUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (trendUp) Color(0xFF2E7D32) else Color(0xFFB00020),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trendUp == null) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else if (trendUp) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFB00020)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Sparkline(
                    dataPoints = sparklineData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    lineColor = sparklineColor
                )
            }
            
            if (badgeCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFB00020),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        if (dataPoints.size < 2) return@Canvas
        val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val minVal = dataPoints.minOrNull() ?: 0.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal
        
        val width = size.width
        val height = size.height
        
        val path = Path()
        val fillPath = Path()
        
        dataPoints.forEachIndexed { index, value ->
            val x = index * (width / (dataPoints.size - 1))
            val y = height - ((value - minVal) / range * height).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == dataPoints.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }
        
        // Draw fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    lineColor.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
            .width(76.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun TransactionRow(invoice: Invoice) {
    val itemsCount = invoice.items.sumOf { it.quantity }
    val formatSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val invoiceTime = formatSdf.format(invoice.timestamp.toDate())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$invoiceTime • Cashier: ${invoice.cashierName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${String.format("%.2f", invoice.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$itemsCount ${if (itemsCount == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun shareDailySummary(
    context: Context,
    storeId: String,
    store: Store?,
    todaysSales: TodaysSalesModel?,
    viewModel: ReportsViewModel,
    onComplete: () -> Unit
) {
    if (todaysSales == null) {
        Toast.makeText(context, "Today's sales are not loaded yet", Toast.LENGTH_SHORT).show()
        onComplete()
        return
    }

    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    coroutineScope.launch {
        try {
            // Load Today's top products and category breakdown dynamically in background
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val end = calendar.time

            // Direct repository queries to guarantee today's values
            val topProductsResult = viewModel.reportsRepository.getTopProducts(storeId, start, end, limit = 10, sortBy = "quantity")
            val categoriesResult = viewModel.reportsRepository.getCategoryBreakdown(storeId, start, end)

            val topProducts = (topProductsResult as? Result.Success)?.data ?: emptyList()
            val categories = (categoriesResult as? Result.Success)?.data ?: emptyList()

            // Generate Daily Summary PDF
            val file = PdfGeneratorUtil.generateDailySummaryPdf(
                context = context,
                store = store,
                revenue = todaysSales.totalRevenue,
                billCount = todaysSales.billCount,
                avgBillValue = todaysSales.averageValue,
                topProducts = topProducts,
                categoryRevenue = categories
            )

            // Direct back to Main thread for sharing
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (file != null) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.invoiceflow.billing.provider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(Intent.createChooser(shareIntent, "Share Daily summary"))
                } else {
                    Toast.makeText(context, "Error generating report PDF", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Error sharing summary: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }
}
