package com.invoiceflow.billing.viewmodel

import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.Product
import com.invoiceflow.billing.repository.ProductRepository
import com.invoiceflow.billing.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Inventory Screen
 */
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingProduct: Product? = null,
    val lowStockCount: Int = 0
)

/**
 * ViewModel for managing inventory operations
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "InventoryViewModel"
    }
    
    // Current user's store ID (must be set from outside)
    private var currentStoreId: String = ""
    
    // UI State
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()
    
    /**
     * Set the current store ID and start observing products
     */
    fun setStoreId(storeId: String) {
        if (currentStoreId != storeId) {
            currentStoreId = storeId
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
                            val products = result.data
                            val lowStockCount = products.count { it.stockQty <= it.minStockLevel }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                products = products,
                                lowStockCount = lowStockCount
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
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = query.isNotBlank()
        )
        
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
     * Show add product dialog
     */
    fun showAddProductDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingProduct = null
        )
    }
    
    /**
     * Show edit product dialog
     */
    fun showEditProductDialog(product: Product) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingProduct = product
        )
    }
    
    /**
     * Hide add/edit dialog
     */
    fun hideAddEditDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = false,
            editingProduct = null
        )
    }
    
    /**
     * Save product (add or update)
     */
    fun saveProduct(
        name: String,
        barcode: String,
        sku: String,
        price: Double,
        stockQty: Int,
        description: String = "",
        category: String = "",
        gstRate: Double = 18.0
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val editingProduct = currentState.editingProduct
            
            if (editingProduct != null) {
                // Update existing product
                val updatedProduct = editingProduct.copy(
                    name = name,
                    barcode = barcode,
                    sku = sku,
                    price = price,
                    stockQty = stockQty,
                    description = description,
                    category = category,
                    gstRate = gstRate,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                productRepository.updateProduct(updatedProduct)
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                _uiState.value = currentState.copy(
                                    showAddEditDialog = false,
                                    editingProduct = null,
                                    successMessage = "Product updated successfully!"
                                )
                            }
                            is Result.Error -> {
                                _uiState.value = currentState.copy(
                                    errorMessage = result.message ?: "Error updating product"
                                )
                            }
                            else -> {}
                        }
                    }
            } else {
                // Add new product
                val newProduct = Product(
                    storeId = currentStoreId,
                    name = name,
                    barcode = barcode,
                    sku = sku,
                    price = price,
                    stockQty = stockQty,
                    description = description,
                    category = category,
                    gstRate = gstRate
                )
                
                productRepository.addProduct(newProduct)
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                _uiState.value = currentState.copy(
                                    showAddEditDialog = false,
                                    successMessage = "Product added successfully!"
                                )
                            }
                            is Result.Error -> {
                                _uiState.value = currentState.copy(
                                    errorMessage = result.message ?: "Error adding product"
                                )
                            }
                            else -> {}
                        }
                    }
            }
        }
    }
    
    /**
     * Delete product
     */
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            productRepository.deleteProduct(productId)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.value = currentState.copy(
                                successMessage = "Product deleted successfully!"
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = currentState.copy(
                                errorMessage = result.message ?: "Error deleting product"
                            )
                        }
                        else -> {}
                    }
                }
        }
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
