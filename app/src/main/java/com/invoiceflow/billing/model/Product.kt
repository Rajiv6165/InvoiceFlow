package com.invoiceflow.billing.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Product document representing an inventory item
 * Every product is associated with exactly one store
 */
data class Product(
    @DocumentId
    val productId: String = "",
    val storeId: String = "",
    val name: String = "",
    val description: String = "",
    val barcode: String = "",
    val sku: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val stockQty: Int = 0,
    val minStockLevel: Int = 0,
    val category: String = "",
    val unit: String = "pcs",
    val gstRate: Double = 18.0,
    val isActive: Boolean = true,
    val imageUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Convert to map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "storeId" to storeId,
            "name" to name,
            "description" to description,
            "barcode" to barcode,
            "sku" to sku,
            "price" to price,
            "costPrice" to costPrice,
            "stockQty" to stockQty,
            "minStockLevel" to minStockLevel,
            "category" to category,
            "unit" to unit,
            "gstRate" to gstRate,
            "isActive" to isActive,
            "imageUrl" to imageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    companion object {
        const val COLLECTION_NAME = "Products"
    }
}
