package com.invoiceflow.billing.model

import com.google.firebase.Timestamp

/**
 * Invoice document representing a completed sale/transaction
 * Stored permanently in Firestore for reporting and analytics
 */
data class Invoice(
    val invoiceId: String = "",
    val storeId: String = "",
    val cashierId: String = "",
    val cashierName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, CARD, UPI, etc.
    val paymentStatus: String = "PAID", // PAID, PENDING
    val customerName: String = "",
    val customerPhone: String = "",
    val notes: String = "",
    val invoiceNumber: String = "" // Human-readable invoice number
) {
    /**
     * Convert to map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "invoiceId" to invoiceId,
            "storeId" to storeId,
            "cashierId" to cashierId,
            "cashierName" to cashierName,
            "timestamp" to timestamp,
            "items" to items.map { it.toMap() },
            "subtotal" to subtotal,
            "taxAmount" to taxAmount,
            "discountAmount" to discountAmount,
            "grandTotal" to grandTotal,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "notes" to notes,
            "invoiceNumber" to invoiceNumber
        )
    }
    
    companion object {
        const val COLLECTION_NAME = "Invoices"
    }
}

/**
 * Individual line item in an invoice
 */
data class InvoiceItem(
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val gstRate: Double = 18.0,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0
) {
    /**
     * Convert to map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "barcode" to barcode,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
            "gstRate" to gstRate,
            "subtotal" to subtotal,
            "taxAmount" to taxAmount,
            "totalAmount" to totalAmount
        )
    }
    
    companion object {
        /**
         * Create from CartItem
         */
        fun fromCartItem(cartItem: CartItem): InvoiceItem {
            return InvoiceItem(
                productId = cartItem.product.productId,
                productName = cartItem.product.name,
                barcode = cartItem.product.barcode,
                quantity = cartItem.quantity,
                unitPrice = cartItem.product.price,
                gstRate = cartItem.product.gstRate,
                subtotal = cartItem.getSubtotal(),
                taxAmount = cartItem.getTaxAmount(),
                totalAmount = cartItem.getTotalAmount()
            )
        }
    }
}
