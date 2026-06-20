package com.invoiceflow.billing.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.CartItem
import com.invoiceflow.billing.model.Invoice
import com.invoiceflow.billing.model.InvoiceItem
import com.invoiceflow.billing.model.Product
import com.invoiceflow.billing.repository.AuthRepository
import com.invoiceflow.billing.repository.ProductRepository
import com.invoiceflow.billing.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI State for POS Screen
 */
data class PosUiState(
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isCheckingOut: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCheckoutSuccess: Boolean = false,
    val lastInvoice: Invoice? = null,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val gstRate: Double = 18.0
)

/**
 * ViewModel for managing POS and cart operations
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val activityLogRepository: com.invoiceflow.billing.repository.ActivityLogRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "PosViewModel"
    }
    
    // Current user's store ID and user info
    private var currentStoreId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    
    // UI State
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()
    
    /**
     * Set the current store ID and user info, then start observing products
     */
    fun setStoreAndUserInfo(storeId: String, userId: String, userName: String) {
        currentStoreId = storeId
        currentUserId = userId
        currentUserName = userName
        
        if (storeId.isNotEmpty()) {
            observeProducts()
        }
    }
    
    /**
     * Observe products for the current store
     */
    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getProductsByStoreId(currentStoreId)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                products = result.data
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Error loading products"
                            )
                        }
                    }
                }
        }
    }
    
    /**
     * Search products by query
     */
    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isBlank()) {
            observeProducts()
            return
        }
        
        viewModelScope.launch {
            productRepository.searchProducts(currentStoreId, query)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                products = result.data,
                                isLoading = false
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = result.message ?: "Error searching products"
                            )
                        }
                        else -> {}
                    }
                }
        }
    }
    
    /**
     * Add product to cart or increment quantity
     */
    fun addToCart(product: Product) {
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find { it.product.productId == product.productId }
        
        val newCartItems = if (existingItem != null) {
            // Increment quantity
            currentState.cartItems.map {
                if (it.product.productId == product.productId) {
                    it.copy(quantity = it.quantity + 1)
                } else {
                    it
                }
            }
        } else {
            // Add new item
            currentState.cartItems + CartItem(product = product, quantity = 1)
        }
        
        updateCart(newCartItems)
    }
    
    /**
     * Remove product from cart
     */
    fun removeFromCart(productId: String) {
        val currentState = _uiState.value
        val newCartItems = currentState.cartItems.filter { it.product.productId != productId }
        updateCart(newCartItems)
    }
    
    /**
     * Increment item quantity
     */
    fun incrementQuantity(productId: String) {
        val currentState = _uiState.value
        val newCartItems = currentState.cartItems.map { cartItem ->
            if (cartItem.product.productId == productId) {
                cartItem.copy(quantity = cartItem.quantity + 1)
            } else {
                cartItem
            }
        }
        updateCart(newCartItems)
    }
    
    /**
     * Decrement item quantity (remove if quantity becomes 0)
     */
    fun decrementQuantity(productId: String) {
        val currentState = _uiState.value
        val newCartItems = currentState.cartItems.mapNotNull { cartItem ->
            if (cartItem.product.productId == productId) {
                if (cartItem.quantity > 1) {
                    cartItem.copy(quantity = cartItem.quantity - 1)
                } else {
                    null // Remove from cart
                }
            } else {
                cartItem
            }
        }
        updateCart(newCartItems)
    }
    
    /**
     * Update cart and recalculate totals
     */
    private fun updateCart(newCartItems: List<CartItem>) {
        val currentState = _uiState.value
        
        // Calculate totals
        val subtotal = newCartItems.sumOf { it.getSubtotal() }
        val taxAmount = newCartItems.sumOf { it.getTaxAmount() }
        val grandTotal = subtotal + taxAmount
        
        _uiState.value = currentState.copy(
            cartItems = newCartItems,
            subtotal = subtotal,
            taxAmount = taxAmount,
            grandTotal = grandTotal
        )
    }
    
    /**
     * Clear cart
     */
    fun clearCart() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            cartItems = emptyList(),
            subtotal = 0.0,
            taxAmount = 0.0,
            grandTotal = 0.0
        )
    }
    
    /**
     * Process checkout - Create invoice and update stock
     */
    fun checkout(paymentMethod: String = "CASH") {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (currentState.cartItems.isEmpty()) {
                _uiState.value = currentState.copy(
                    errorMessage = "Cart is empty"
                )
                return@launch
            }
            
            _uiState.value = currentState.copy(
                isCheckingOut = true,
                errorMessage = null
            )
            
            try {
                // Generate invoice number
                val invoiceNumber = generateInvoiceNumber()
                
                // Create invoice items from cart items
                val invoiceItems = currentState.cartItems.map { cartItem ->
                    InvoiceItem.fromCartItem(cartItem)
                }
                
                // Create invoice document
                val invoice = Invoice(
                    storeId = currentStoreId,
                    cashierId = currentUserId,
                    cashierName = currentUserName,
                    timestamp = com.google.firebase.Timestamp.now(),
                    items = invoiceItems,
                    subtotal = currentState.subtotal,
                    taxAmount = currentState.taxAmount,
                    discountAmount = 0.0,
                    grandTotal = currentState.grandTotal,
                    paymentMethod = paymentMethod,
                    paymentStatus = "PAID",
                    invoiceNumber = invoiceNumber
                )
                
                // Use Firestore transaction to ensure atomicity
                val invoiceRef = firestore.collection(Invoice.COLLECTION_NAME).document()
                val finalInvoice = invoice.copy(invoiceId = invoiceRef.id)
                
                // Prepare stock updates
                val stockUpdates = currentState.cartItems.associate { 
                    it.product.productId to it.quantity 
                }
                
                // Execute transaction
                firestore.runTransaction { transaction ->
                    // Add invoice
                    transaction.set(invoiceRef, finalInvoice.toMap())
                    
                    // Update stock for each product
                    stockUpdates.forEach { (productId, quantity) ->
                        val productRef = firestore.collection(Product.COLLECTION_NAME).document(productId)
                        transaction.update(
                            productRef,
                            mapOf(
                                "stockQty" to com.google.firebase.firestore.FieldValue.increment(-quantity.toLong()),
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                        )
                    }
                }.await()
                
                // Log event
                try {
                    activityLogRepository.logEvent(
                        storeId = currentStoreId,
                        userId = currentUserId,
                        userName = currentUserName,
                        userEmail = "",
                        actionType = "INVOICE_CREATED",
                        details = mapOf(
                            "invoiceId" to finalInvoice.invoiceId,
                            "invoiceNumber" to finalInvoice.invoiceNumber,
                            "grandTotal" to finalInvoice.grandTotal.toString(),
                            "itemsCount" to finalInvoice.items.sumOf { it.quantity }.toString()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log invoice creation event", e)
                }
                
                // Success!
                _uiState.value = currentState.copy(
                    isCheckingOut = false,
                    showCheckoutSuccess = true,
                    lastInvoice = finalInvoice,
                    successMessage = "Checkout successful! Invoice: $invoiceNumber",
                    cartItems = emptyList(),
                    subtotal = 0.0,
                    taxAmount = 0.0,
                    grandTotal = 0.0
                )
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isCheckingOut = false,
                    errorMessage = e.message ?: "Checkout failed"
                )
            }
        }
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            showCheckoutSuccess = false,
            lastInvoice = null,
            successMessage = null
        )
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(errorMessage = null)
    }
    
    /**
     * Generate unique invoice number
     * Format: INV-YYYYMMDD-XXXXX
     */
    private fun generateInvoiceNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val randomNum = (10000..99999).random()
        return "INV-$dateStr-$randomNum"
    }
}
