@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.invoiceflow.billing.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.AnalyticsPeriod
import com.invoiceflow.billing.viewmodel.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesAnalyticsScreen(
    storeId: String,
    onNavigateToTopProducts: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    // Configure store ID
    LaunchedEffect(storeId) {
        viewModel.setStoreId(storeId)
    }

    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val salesPeriodResult by viewModel.salesPeriodState.collectAsState()
    val trendResult by viewModel.dailyRevenueTrendState.collectAsState()
    val categoryResult by viewModel.categoryBreakdownState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Intelligence", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshData(force = true) }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Chips
            item {
                PeriodSelectorRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { period ->
                        if (period is AnalyticsPeriod.Custom) {
                            showDatePicker = true
                        } else {
                            viewModel.setPeriod(period)
                        }
                    }
                )
            }

            // 2. Revenue Overview Section
            item {
                when (salesPeriodResult) {
                    is Result.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Result.Error -> {
                        Text(
                            text = "Error: ${(salesPeriodResult as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    is Result.Success -> {
                        val metrics = (salesPeriodResult as Result.Success<SalesPeriodModel>).data
                        RevenueOverviewCard(
                            metrics = metrics,
                            periodLabel = selectedPeriod.getLabel()
                        )
                    }
                }
            }

            // 3. Interactive Line Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Revenue Trend (Line)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        when (trendResult) {
                            is Result.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is Result.Error -> {
                                Text("Failed to load trend data", color = MaterialTheme.colorScheme.error)
                            }
                            is Result.Success -> {
                                val trends = (trendResult as Result.Success<List<DailyRevenue>>).data
                                val avgRevenue = if (trends.isNotEmpty()) trends.map { it.revenue }.filter { it > 0 }.average() else 0.0
                                val finalAvg = if (avgRevenue.isNaN()) 0.0 else avgRevenue

                                InteractiveLineChart(
                                    data = trends,
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    averageRevenue = finalAvg
                                )
                            }
                        }
                    }
                }
            }

            // 4. Products Intelligence Card Link
            item {
                Card(
                    onClick = onNavigateToTopProducts,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Product Intelligence Reports",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Rankings, category share & bottom sales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Detailed Transaction Analysis Section
            item {
                Text(
                    text = "Transaction Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                when (salesPeriodResult) {
                    is Result.Success -> {
                        val metrics = (salesPeriodResult as Result.Success<SalesPeriodModel>).data
                        TransactionAnalysisGrid(metrics = metrics)
                    }
                    else -> {}
                }
            }

            // 6. Payment Insights Pie Chart & Success Rate
            item {
                when (salesPeriodResult) {
                    is Result.Success -> {
                        val metrics = (salesPeriodResult as Result.Success<SalesPeriodModel>).data
                        PaymentInsightsCard(metrics = metrics)
                    }
                    else -> {}
                }
            }

            // Extra padding
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Date Range Picker Dialog
        if (showDatePicker) {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                viewModel.setPeriod(
                                    AnalyticsPeriod.Custom(Date(start), Date(end))
                                )
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PeriodSelectorRow(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    val periods = listOf(
        AnalyticsPeriod.Today,
        AnalyticsPeriod.Yesterday,
        AnalyticsPeriod.ThisWeek,
        AnalyticsPeriod.ThisMonth,
        AnalyticsPeriod.Last3Months,
        AnalyticsPeriod.ThisYear,
        AnalyticsPeriod.Custom(Date(), Date())
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            val isSelected = when (period) {
                is AnalyticsPeriod.Custom -> selectedPeriod is AnalyticsPeriod.Custom
                else -> selectedPeriod == period
            }

            FilterChip(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.getLabel()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun RevenueOverviewCard(
    metrics: SalesPeriodModel,
    periodLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Total Revenue ($periodLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "₹${String.format("%.2f", metrics.totalRevenue)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (metrics.comparisonRevenueUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (metrics.comparisonRevenueUp) Color(0xFF81C784) else Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.1f", metrics.comparisonRevenuePercentage)}% vs previous period",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (metrics.comparisonRevenueUp) Color(0xFF81C784) else Color(0xFFE57373)
                )
            }
        }
    }
}

@Composable
fun InteractiveLineChart(
    data: List<DailyRevenue>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    averageRevenue: Double
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No sales data available for chart", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    // Reset selection on data change
    LaunchedEffect(data) {
        selectedIndex = null
    }

    // Animate line loading
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    val maxVal = data.maxOf { it.revenue }.coerceAtLeast(1.0)
    val minVal = 0.0
    val range = maxVal - minVal

    Column(modifier = Modifier.fillMaxWidth()) {
        if (selectedIndex != null && selectedIndex!! < data.size) {
            val pt = data[selectedIndex!!]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "${pt.dateStr}: ₹${String.format("%.2f", pt.revenue)} (${pt.billCount} bills)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val pointsCount = data.size
                        val stepX = if (pointsCount > 1) width / (pointsCount - 1) else width
                        val index = (offset.x / stepX).roundToInt().coerceIn(0, pointsCount - 1)
                        selectedIndex = index
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Grid Lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * (i.toFloat() / gridLines)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Average Revenue Line (Dashed)
            if (averageRevenue > 0) {
                val avgY = (height - ((averageRevenue - minVal) / range * height)).toFloat()
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(0f, avgY),
                    end = Offset(width, avgY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            }

            // 3. Draw Path & Area
            if (data.size >= 2) {
                val path = Path()
                val fillPath = Path()

                data.forEachIndexed { index, point ->
                    val x = index * (width / (data.size - 1))
                    val y = (height - ((point.revenue - minVal) / range * height)).toFloat()
                    val animY = height - (height - y) * animProgress.value

                    if (index == 0) {
                        path.moveTo(x, animY)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, animY)
                    } else {
                        path.lineTo(x, animY)
                        fillPath.lineTo(x, animY)
                    }

                    if (index == data.size - 1) {
                        fillPath.lineTo(x, height)
                        fillPath.close()
                    }
                }

                // Draw Gradient Area Fill
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

                // Draw Trend Line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Dots at Points
                data.forEachIndexed { index, point ->
                    val x = index * (width / (data.size - 1))
                    val y = (height - ((point.revenue - minVal) / range * height)).toFloat()
                    val animY = height - (height - y) * animProgress.value

                    val isSelected = selectedIndex == index
                    drawCircle(
                        color = if (isSelected) secondaryColor else lineColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(x, animY)
                    )

                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, animY)
                        )
                    }
                }
            } else if (data.size == 1) {
                // Single point fallback
                val y = (height - ((data[0].revenue - minVal) / range * height)).toFloat()
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = Offset(width / 2, y)
                )
            }
        }
    }
}

@Composable
fun TransactionAnalysisGrid(metrics: SalesPeriodModel) {
    val items = listOf(
        AnalysisItemData("Total Transactions", "${metrics.transactionCount}"),
        AnalysisItemData("Avg. Bill Value", "₹${String.format("%.2f", metrics.averageTransactionValue)}"),
        AnalysisItemData("Highest Transaction", "₹${String.format("%.2f", metrics.highestTransactionValue)}"),
        AnalysisItemData("Lowest Transaction", "₹${String.format("%.2f", metrics.lowestTransactionValue)}"),
        AnalysisItemData("Peak Day of Week", formatDayOfWeek(metrics.peakDayOfWeek))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Analysis Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    AnalysisItem(items[0])
                    Spacer(modifier = Modifier.height(12.dp))
                    AnalysisItem(items[2])
                }
                Column(modifier = Modifier.weight(1f)) {
                    AnalysisItem(items[1])
                    Spacer(modifier = Modifier.height(12.dp))
                    AnalysisItem(items[3])
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Peak Hour & Day section
            Text(
                text = "Peak Transactions Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Peak hour: ${formatHour(metrics.peakHour)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Peak day: ${items[4].value}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                HourlyBarChart(
                    hourlyDistribution = metrics.hourlyDistribution,
                    modifier = Modifier.size(width = 120.dp, height = 40.dp)
                )
            }
        }
    }
}

data class AnalysisItemData(val label: String, val value: String)

@Composable
fun AnalysisItem(data: AnalysisItemData) {
    Column {
        Text(
            text = data.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = data.value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HourlyBarChart(
    hourlyDistribution: Map<Int, Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxCount = hourlyDistribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val barCount = 24
        val spacing = 1.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        for (hour in 0 until barCount) {
            val count = hourlyDistribution[hour] ?: 0
            val barHeight = (count.toFloat() / maxCount) * height
            val x = hour * (barWidth + spacing)
            val y = height - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
fun PaymentInsightsCard(metrics: SalesPeriodModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Payment Splits & Success",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PaymentInsightsPieChart(
                cashRevenue = metrics.cashRevenue,
                digitalRevenue = metrics.digitalRevenue,
                modifier = Modifier.height(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transaction Success Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reflects completed vs pending checkouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (metrics.transactionSuccessRate >= 95.0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "${String.format("%.1f", metrics.transactionSuccessRate)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.transactionSuccessRate >= 95.0) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentInsightsPieChart(
    cashRevenue: Double,
    digitalRevenue: Double,
    modifier: Modifier = Modifier
) {
    val total = cashRevenue + digitalRevenue
    if (total == 0.0) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No transaction history", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val cashAngle = (cashRevenue / total * 360f).toFloat()
    val digitalAngle = (digitalRevenue / total * 360f).toFloat()

    val cashPercent = (cashRevenue / total * 100).toFloat()
    val digitalPercent = (digitalRevenue / total * 100).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            // Draw Cash slice
            drawArc(
                color = Color(0xFF2E7D32),
                startAngle = -90f,
                sweepAngle = cashAngle,
                useCenter = true
            )
            // Draw Digital slice
            drawArc(
                color = Color(0xFF1976D2),
                startAngle = -90f + cashAngle,
                sweepAngle = digitalAngle,
                useCenter = true
            )
            
            // Draw donut overlay
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 4f
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF2E7D32), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cash: ₹${String.format("%.2f", cashRevenue)} (${String.format("%.1f", cashPercent)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF1976D2), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Digital: ₹${String.format("%.2f", digitalRevenue)} (${String.format("%.1f", digitalPercent)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour == 12 -> "12 PM"
        hour > 12 -> "${hour - 12} PM"
        else -> "$hour AM"
    }
}

private fun formatDayOfWeek(day: Int): String {
    return when (day) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Unknown"
    }
}
