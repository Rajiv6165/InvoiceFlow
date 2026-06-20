package com.invoiceflow.billing.ui.screens.analytics

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.CategoryRevenue
import com.invoiceflow.billing.model.TopProductModel
import com.invoiceflow.billing.util.Result
import com.invoiceflow.billing.viewmodel.ReportsViewModel
import java.util.*
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopProductsScreen(
    storeId: String,
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    // Configure store ID
    LaunchedEffect(storeId) {
        viewModel.setStoreId(storeId)
    }

    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val topProductsResult by viewModel.topProductsState.collectAsState()
    val categoryResult by viewModel.categoryBreakdownState.collectAsState()
    val activeSort by viewModel.activeProductsSort.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Sync tab changes with reports view model sort orders
    LaunchedEffect(selectedTab) {
        val sortBy = when (selectedTab) {
            0 -> "quantity"
            1 -> "revenue"
            2 -> "slow_moving"
            3 -> "never_sold"
            else -> "quantity"
        }
        viewModel.setProductsSort(sortBy)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Intelligence", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            // 1. Period Indicator
            item {
                Text(
                    text = "Reporting Period: ${selectedPeriod.getLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 2. Category Performance Donut Section
            item {
                when (categoryResult) {
                    is Result.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Result.Success -> {
                        val categories = (categoryResult as Result.Success<List<CategoryRevenue>>).data
                        if (categories.isNotEmpty()) {
                            CategoryDonutCard(
                                categories = categories,
                                selectedCategory = selectedCategoryFilter,
                                onCategoryTapped = { cat ->
                                    selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                }
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Active Category Filter Indicator
            if (selectedCategoryFilter != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InputChip(
                            selected = true,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Category: $selectedCategoryFilter") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // 3. Tab Layout Selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Top Qty", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Top Revenue", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Slow Moving", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Never Sold", fontSize = 12.sp) }
                    )
                }
            }

            // 4. Products List
            when (topProductsResult) {
                is Result.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is Result.Error -> {
                    item {
                        Text(
                            text = "Error: ${(topProductsResult as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is Result.Success -> {
                    val products = (topProductsResult as Result.Success<List<TopProductModel>>).data
                    
                    // Filter in memory by tapped category
                    val filteredProducts = if (selectedCategoryFilter != null) {
                        products.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                    } else {
                        products
                    }

                    if (filteredProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No products found for this query",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    } else {
                        val maxQtyVal = products.maxOfOrNull { it.quantitySold }?.coerceAtLeast(1) ?: 1
                        val maxRevVal = products.maxOfOrNull { it.revenue }?.coerceAtLeast(1.0) ?: 1.0
                        
                        itemsIndexed(
                            items = filteredProducts,
                            key = { _, prod -> prod.productId }
                        ) { index, product ->
                            ProductPerformanceCard(
                                product = product,
                                rank = index + 1,
                                maxQuantityValue = maxQtyVal,
                                maxRevenueValue = maxRevVal,
                                showRevenueBar = selectedTab == 1
                            )
                        }
                    }
                }
            }

            // Extra space
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CategoryDonutCard(
    categories: List<CategoryRevenue>,
    selectedCategory: String?,
    onCategoryTapped: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Revenue Contribution by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Donut Chart using Canvas
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(110.dp)
                            .pointerInput(categories) {
                                detectTapGestures { offset ->
                                    val center = size.width / 2f
                                    val dx = offset.x - center
                                    val dy = offset.y - center
                                    // Calculate angle in degrees [0, 360)
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    // Rotate angle to align with chart startAngle (-90 degrees / top center)
                                    angle = (angle + 90f) % 360f
                                    
                                    var startAngle = 0f
                                    for (cat in categories) {
                                        val sweepAngle = (cat.percentage / 100.0 * 360f).toFloat()
                                        if (angle >= startAngle && angle <= startAngle + sweepAngle) {
                                            onCategoryTapped(cat.category)
                                            break
                                        }
                                        startAngle += sweepAngle
                                    }
                                }
                            }
                    ) {
                        var startAngle = -90f
                        categories.forEachIndexed { index, cat ->
                            val sweepAngle = (cat.percentage / 100f * 360f).toFloat()
                            val isSelected = selectedCategory == cat.category
                            
                            drawArc(
                                color = getCategoryColor(index),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                alpha = if (selectedCategory == null || isSelected) 1f else 0.3f
                            )
                            startAngle += sweepAngle
                        }
                        
                        // Inner circle hole to make it a donut
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension / 3.2f
                        )
                    }
                    
                    if (selectedCategory != null) {
                        Text(
                            text = "Filtered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    categories.take(5).forEachIndexed { index, cat ->
                        val isSelected = selectedCategory == cat.category
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryTapped(cat.category) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = getCategoryColor(index),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.category.ifBlank { "General" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${String.format("%.1f", cat.percentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPerformanceCard(
    product: TopProductModel,
    rank: Int,
    maxQuantityValue: Int,
    maxRevenueValue: Double,
    showRevenueBar: Boolean
) {
    val stockStatusColor = when {
        product.stockQty <= 0 -> Color(0xFFB00020) // Red
        product.stockQty <= product.minStockLevel -> Color(0xFFE65100) // Orange
        else -> Color(0xFF2E7D32) // Green
    }
    
    val stockText = when {
        product.stockQty <= 0 -> "Out of Stock"
        product.stockQty <= product.minStockLevel -> "Low Stock: ${product.stockQty}"
        else -> "Stock: ${product.stockQty}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Rank Medal/Badge
                    when (rank) {
                        1 -> Text("🥇", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        2 -> Text("🥈", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        3 -> Text("🥉", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stockStatusColor.copy(alpha = 0.1f),
                ) {
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.labelSmall,
                        color = stockStatusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "Quantity Sold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${product.quantitySold} pcs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "Revenue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "₹${String.format("%.2f", product.revenue)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Store Share",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%.1f", product.percentageOfTotalRevenue)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Relative performance bar
            val progress = if (showRevenueBar) {
                (product.revenue / maxRevenueValue).toFloat().coerceIn(0f, 1f)
            } else {
                (product.quantitySold.toFloat() / maxQuantityValue).coerceIn(0f, 1f)
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (showRevenueBar) Color(0xFF1976D2) else Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

fun getCategoryColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF1976D2), // Blue
        Color(0xFF2E7D32), // Green
        Color(0xFFBB86FC), // Purple
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFF9C27B0), // Violet
        Color(0xFF4CAF50), // Light Green
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF9E9E9E)  // Gray
    )
    return colors[index % colors.size]
}
