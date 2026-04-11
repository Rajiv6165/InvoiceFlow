package com.invoiceflow.billing.model

/**
 * Cart item representing a product in the shopping cart
 * This is a local data class (not stored in Firestore)
 */
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    /**
     * Calculate subtotal for this cart item (before tax)
     */
    fun getSubtotal(): Double {
        return product.price * quantity
    }
    
    /**
     * Calculate tax amount for this cart item
     */
    fun getTaxAmount(): Double {
        return getSubtotal() * (product.gstRate / 100.0)
    }
    
    /**
     * Get total amount for this cart item (including tax)
     */
    fun getTotalAmount(): Double {
        return getSubtotal() + getTaxAmount()
    }
}
