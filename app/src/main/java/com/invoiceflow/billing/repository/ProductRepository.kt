package com.invoiceflow.billing.repository

import android.util.Log
import com.invoiceflow.billing.model.Product
import com.invoiceflow.billing.util.Constants
import com.invoiceflow.billing.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling Product/Inventory operations
 */
@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BaseRepository() {
    
    companion object {
        private const val TAG = "ProductRepository"
    }
    
    /**
     * Get all products for a store as Flow (real-time updates)
     */
    fun getProductsByStoreId(storeId: String): Flow<Result<List<Product>>> = callbackFlow {
        try {
            trySend(Result.Loading)
            
            val listenerRegistration = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error, error.message ?: "Error fetching products"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val products = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Product::class.java)
                        }
                        trySend(Result.Success(products))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products flow", e)
            close(e)
        }
    }
    
    /**
     * Search products by name or barcode
     */
    fun searchProducts(storeId: String, query: String): Flow<Result<List<Product>>> = callbackFlow {
        try {
            trySend(Result.Loading)
            
            // Case-insensitive search by name
            val lowerCaseQuery = query.lowercase()
            
            val listenerRegistration = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error, error.message ?: "Error searching products"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val products = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Product::class.java)
                        }.filter { product ->
                            // Filter by name or barcode
                            product.name.lowercase().contains(lowerCaseQuery) ||
                            product.barcode.lowercase().contains(lowerCaseQuery) ||
                            product.sku.lowercase().contains(lowerCaseQuery)
                        }
                        trySend(Result.Success(products))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products", e)
            close(e)
        }
    }
    
    /**
     * Get product by ID
     */
    suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val doc = firestore.collection(Product.COLLECTION_NAME)
                .document(productId)
                .get()
                .await()
            
            if (doc.exists()) {
                val product = doc.toObject(Product::class.java) 
                    ?: return Result.Error(Exception("Product data is null"), "Product not found")
                Result.Success(product)
            } else {
                Result.Error(Exception("Product not found"), "Product does not exist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product by ID", e)
            Result.Error(e, e.message ?: "Error fetching product")
        }
    }
    
    /**
     * Add new product
     */
    suspend fun addProduct(product: Product): Result<String> {
        return try {
            // Validate storeId
            if (product.storeId.isBlank()) {
                return Result.Error(
                    IllegalArgumentException("Store ID is required"),
                    "Store ID is required"
                )
            }
            
            val docRef = firestore.collection(Product.COLLECTION_NAME).document()
            val newProduct = product.copy(productId = docRef.id)
            
            docRef.set(newProduct).await()
            
            Log.d(TAG, "Product added: ${newProduct.productId}")
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product", e)
            Result.Error(e, e.message ?: "Error adding product")
        }
    }
    
    /**
     * Update existing product
     */
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            if (product.productId.isBlank()) {
                return Result.Error(
                    IllegalArgumentException("Product ID is required"),
                    "Product ID is required"
                )
            }
            
            firestore.collection(Product.COLLECTION_NAME)
                .document(product.productId)
                .update(product.toMap())
                .await()
            
            Log.d(TAG, "Product updated: ${product.productId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product", e)
            Result.Error(e, e.message ?: "Error updating product")
        }
    }
    
    /**
     * Delete product (soft delete - set isActive to false)
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection(Product.COLLECTION_NAME)
                .document(productId)
                .update("isActive", false, "updatedAt", com.google.firebase.Timestamp.now())
                .await()
            
            Log.d(TAG, "Product deleted (soft): $productId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product", e)
            Result.Error(e, e.message ?: "Error deleting product")
        }
    }
    
    /**
     * Permanently delete product (hard delete)
     */
    suspend fun permanentlyDeleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection(Product.COLLECTION_NAME)
                .document(productId)
                .delete()
                .await()
            
            Log.d(TAG, "Product permanently deleted: $productId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error permanently deleting product", e)
            Result.Error(e, e.message ?: "Error deleting product")
        }
    }
    
    /**
     * Update product stock (for checkout)
     */
    suspend fun updateProductStock(productId: String, quantitySold: Int): Result<Unit> {
        return try {
            firestore.collection(Product.COLLECTION_NAME)
                .document(productId)
                .update(
                    mapOf(
                        "stockQty" to com.google.firebase.firestore.FieldValue.increment(-quantitySold.toLong()),
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
            
            Log.d(TAG, "Product stock updated: $productId, quantity: -$quantitySold")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product stock", e)
            Result.Error(e, e.message ?: "Error updating stock")
        }
    }
    
    /**
     * Batch update stock for multiple products (used in checkout transaction)
     */
    suspend fun batchUpdateStock(stockUpdates: Map<String, Int>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            stockUpdates.forEach { (productId, quantity) ->
                val docRef = firestore.collection(Product.COLLECTION_NAME).document(productId)
                batch.update(
                    docRef,
                    mapOf(
                        "stockQty" to com.google.firebase.firestore.FieldValue.increment(-quantity.toLong()),
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
            }
            
            batch.commit().await()
            
            Log.d(TAG, "Batch stock update completed for ${stockUpdates.size} products")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch stock update", e)
            Result.Error(e, e.message ?: "Error updating stock")
        }
    }
    
    /**
     * Check if product exists
     */
    suspend fun productExists(productId: String): Boolean {
        return try {
            val doc = firestore.collection(Product.COLLECTION_NAME)
                .document(productId)
                .get()
                .await()
            
            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking product existence", e)
            false
        }
    }
    
    /**
     * Get low stock products
     */
    fun getLowStockProducts(storeId: String): Flow<Result<List<Product>>> = callbackFlow {
        try {
            trySend(Result.Loading)
            
            val listenerRegistration = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error, error.message ?: "Error fetching low stock products"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val products = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Product::class.java)
                        }.filter { product ->
                            product.stockQty <= product.minStockLevel
                        }
                        trySend(Result.Success(products))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting low stock products", e)
            close(e)
        }
    }
}
