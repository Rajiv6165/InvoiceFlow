package com.invoiceflow.billing.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Store document representing a retail shop/tenant
 * Every store is isolated with its own data in Firestore
 */
data class Store(
    @DocumentId
    val storeId: String = "",
    val name: String = "",
    val address: String = "",
    val gstRate: Double = 18.0,  // Default GST rate of 18%
    val logoUrl: String = "",
    val currency: String = "INR",
    val phone: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    
    // Subscription system fields
    val subscriptionStatus: String = "trial", // "trial" | "active" | "expired" | "suspended"
    val planType: String = "starter", // "starter" | "professional" | "enterprise"
    val trialStartDate: Timestamp = Timestamp.now(),
    val trialEndDate: Timestamp = Timestamp(java.util.Date(createdAt.toDate().time + 14L * 24 * 60 * 60 * 1000)),
    val subscriptionStartDate: Timestamp? = null,
    val subscriptionExpiryDate: Timestamp? = null,
    val maxStaffAccounts: Int = 2,
    val maxProductsAllowed: Int = 100,
    val featuresEnabled: List<String> = listOf("analytics", "pdf"),
    val lastPaymentDate: Timestamp? = null,
    val paymentMethod: String = "",
    val autoRenew: Boolean = false,
    val suspensionReason: String? = null
) {
    /**
     * Convert to map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "storeId" to storeId,
            "name" to name,
            "address" to address,
            "gstRate" to gstRate,
            "logoUrl" to logoUrl,
            "currency" to currency,
            "phone" to phone,
            "email" to email,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isActive" to isActive,
            "subscriptionStatus" to subscriptionStatus,
            "planType" to planType,
            "trialStartDate" to trialStartDate,
            "trialEndDate" to trialEndDate,
            "subscriptionStartDate" to subscriptionStartDate,
            "subscriptionExpiryDate" to subscriptionExpiryDate,
            "maxStaffAccounts" to maxStaffAccounts,
            "maxProductsAllowed" to maxProductsAllowed,
            "featuresEnabled" to featuresEnabled,
            "lastPaymentDate" to lastPaymentDate,
            "paymentMethod" to paymentMethod,
            "autoRenew" to autoRenew,
            "suspensionReason" to suspensionReason
        )
    }
    
    companion object {
        const val COLLECTION_NAME = "Stores"
        
        /**
         * Default store for initial seeding
         */
        fun defaultKothariProvisionStore(storeId: String): Store {
            return Store(
                storeId = storeId,
                name = "Kothari Provision Store",
                address = "",
                gstRate = 18.0,
                currency = "INR"
            )
        }
    }
}
