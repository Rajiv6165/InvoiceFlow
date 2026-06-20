package com.invoiceflow.billing.ui.screens.pos

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoiceflow.billing.model.CartItem
import com.invoiceflow.billing.model.Invoice
import com.invoiceflow.billing.model.Product
import com.invoiceflow.billing.model.Store
import com.invoiceflow.billing.util.PdfGeneratorUtil
import com.invoiceflow.billing.viewmodel.PosViewModel
import com.invoiceflow.billing.viewmodel.PosUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@Composable
fun PosScreen(
    storeId: String,
    userId: String,
    userName: String,
    viewModel: PosViewModel = hiltViewModel()
) {
    LaunchedEffect(storeId, userId, userName) {
        viewModel.setStoreAndUserInfo(storeId, userId, userName)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    if (uiState.showCheckoutSuccess && uiState.lastInvoice != null) {
        CheckoutSuccessScreen(
            invoice = uiState.lastInvoice!!,
            onNewBill = viewModel::clearSuccessMessage,
            onPrint = {
                // Perform receipt printing
                printInvoiceReceipt(context, uiState.lastInvoice!!)
            },
            onShare = {
                // Share receipt via WhatsApp
                shareReceiptOnWhatsApp(context, uiState.lastInvoice!!)
            }
        )
    } else {
        PosContent(
            uiState = uiState,
            onSearchQueryChange = viewModel::searchProducts,
            onProductClick = viewModel::addToCart,
            onIncrementQuantity = viewModel::incrementQuantity,
            onDecrementQuantity = viewModel::decrementQuantity,
            onRemoveFromCart = viewModel::removeFromCart,
            onClearCart = viewModel::clearCart,
            onCheckout = viewModel::checkout,
            onClearSuccessMessage = viewModel::clearSuccessMessage,
            onClearErrorMessage = viewModel::clearErrorMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosContent(
    uiState: PosUiState,
    onSearchQueryChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onIncrementQuantity: (String) -> Unit,
    onDecrementQuantity: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (String) -> Unit,
    onClearSuccessMessage: () -> Unit,
    onClearErrorMessage: () -> Unit
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val priceFormat = NumberFormat.getCurrencyInstance().apply {
        currency = java.util.Currency.getInstance("INR")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    AnimatedVisibility(
                        visible = !searchExpanded,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Text("Point of Sale", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Animated Search bar expansion
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            placeholder = { Text("Search products...", color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    onSearchQueryChange("")
                                    searchExpanded = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close search",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                    
                    if (uiState.cartItems.isNotEmpty() && !searchExpanded) {
                        IconButton(onClick = onClearCart) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear cart",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Static Search Bar fallback (if not expanded in top bar)
                if (!searchExpanded) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Quick filter products...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                // Error Banner with shake feedback
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearErrorMessage) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Products Grid
                if (uiState.isLoading) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(6) {
                            ProductCardSkeleton()
                        }
                    }
                } else if (uiState.products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No products found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.products, key = { it.productId }) { product ->
                            PremiumProductCard(
                                product = product,
                                onClick = { onProductClick(product) }
                            )
                        }
                    }
                }
            }
            
            // Cart Bottom Sheet panel
            if (uiState.cartItems.isNotEmpty()) {
                CartBottomSheet(
                    cartItems = uiState.cartItems,
                    subtotal = uiState.subtotal,
                    taxAmount = uiState.taxAmount,
                    grandTotal = uiState.grandTotal,
                    isCheckingOut = uiState.isCheckingOut,
                    onIncrementQuantity = onIncrementQuantity,
                    onDecrementQuantity = onDecrementQuantity,
                    onRemoveFromCart = onRemoveFromCart,
                    onCheckoutClick = { showCheckoutDialog = true }
                )
            }
        }
        
        // Checkout Confirmation Dialog
        if (showCheckoutDialog) {
            AlertDialog(
                onDismissRequest = { showCheckoutDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Confirm Checkout", style = MaterialTheme.typography.headlineMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = priceFormat.format(uiState.grandTotal),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payment Mode: CASH", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onCheckout("CASH")
                            showCheckoutDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Receipt")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Premium Product Card with Category code borders and category icons
 */
@Composable
private fun PremiumProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val priceFormat = NumberFormat.getCurrencyInstance().apply {
        currency = java.util.Currency.getInstance("INR")
    }
    
    // Category mapping configurations
    val categoryClean = product.category.lowercase().trim()
    val categoryColor = when {
        categoryClean.contains("grocery") || categoryClean.contains("food") -> Color(0xFF2E7D32)
        categoryClean.contains("bev") || categoryClean.contains("drink") -> Color(0xFF1565C0)
        categoryClean.contains("snack") || categoryClean.contains("chips") -> Color(0xFFE65100)
        categoryClean.contains("dairy") || categoryClean.contains("milk") -> Color(0xFFFD7E14)
        else -> Color(0xFF00897B)
    }
    
    val categoryIcon = when {
        categoryClean.contains("grocery") -> Icons.Default.LocalGroceries
        categoryClean.contains("bev") -> Icons.Default.LocalCafe
        categoryClean.contains("snack") -> Icons.Default.Fastfood
        categoryClean.contains("dairy") -> Icons.Default.Egg
        else -> Icons.Default.Inventory
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Category accent header band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(categoryColor)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top row with Category Icon & Stock levels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // Stock badge
                    val isLowStock = product.stockQty <= product.minStockLevel
                    val badgeBg = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    val badgeFg = if (isLowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (product.stockQty <= 0) "Out of Stock" else "${product.stockQty} left",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = badgeFg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Product Name
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 40.dp)
                )
                
                // Price and Add button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = priceFormat.format(product.price),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to cart",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen Checkout Success Overlay Screen
 */
@Composable
fun CheckoutSuccessScreen(
    invoice: Invoice,
    onNewBill: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
    val priceFormat = NumberFormat.getCurrencyInstance().apply {
        currency = java.util.Currency.getInstance("INR")
    }
    
    val showConfetti = invoice.grandTotal >= 1000.0
    
    // Checkmark draw animation path
    val checkmarkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Confetti Canvas Overlay (only for >= ₹1000)
        if (showConfetti) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Draw Checkmark Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(50.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.5f)
                        lineTo(size.width * 0.42f, size.height * 0.75f)
                        lineTo(size.width * 0.85f, size.height * 0.25f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF2E7D32),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                intervals = floatArrayOf(200f, 200f),
                                phase = 200f * (1f - checkmarkProgress.value)
                            )
                        )
                    )
                }
            }
            
            Text(
                text = "Bill Created successfully!",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32)
            )
            
            // Invoice Summary details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Invoice No:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(invoice.invoiceNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Items:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text("${invoice.items.sumOf { it.quantity }} items", style = MaterialTheme.typography.bodyMedium)
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = priceFormat.format(invoice.grandTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print Receipt", style = MaterialTheme.typography.titleMedium)
                }
                
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share on WhatsApp", style = MaterialTheme.typography.titleMedium)
                }
                
                TextButton(
                    onClick = onNewBill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create New Bill", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Animated Confetti Canvas drawing floating particles
 */
@Composable
fun ConfettiCanvas(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti_loop")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_y"
    )
    
    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = (0..100).random() / 100f,
                speed = (5..15).random() / 10f,
                radius = (4..8).random().toFloat(),
                color = Color(
                    red = (0..255).random() / 255f,
                    green = (0..255).random() / 255f,
                    blue = (0..255).random() / 255f
                ),
                sway = (1..5).random()
            )
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val py = (size.height * progress * p.speed) % size.height
            val px = (size.width * p.x) + (Math.sin(progress.toDouble() * p.sway) * 20f).toFloat()
            drawCircle(
                color = p.color,
                radius = p.radius,
                center = Offset(px, py)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val sway: Int
)

/**
 * Helpers for Printing & Sharing Invoices
 */
private fun printInvoiceReceipt(context: Context, invoice: Invoice) {
    val file = PdfGeneratorUtil.generateInvoicePdf(context, invoice, null)
    if (file != null) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "InvoiceFlow_${invoice.invoiceNumber}"
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(jobName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = java.io.FileInputStream(file)
                    val output = java.io.FileOutputStream(destination?.fileDescriptor)
                    input.copyTo(output)
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, printAdapter, null)
    }
}

private fun shareReceiptOnWhatsApp(context: Context, invoice: Invoice) {
    try {
        val text = "🧾 *Invoice from InvoiceFlow*\n" +
                "Invoice No: ${invoice.invoiceNumber}\n" +
                "Total: ₹${String.format("%.2f", invoice.grandTotal)}\n" +
                "Payment Method: ${invoice.paymentMethod}\n" +
                "Thank you for shopping with us!"
        val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=${invoice.customerPhone.ifBlank { "" }}&text=$encodedText"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Safe fallback
    }
}
