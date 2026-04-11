package com.invoiceflow.billing.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * User document representing a system user (Owner or Cashier)
 * Each user is associated with exactly one store
 */
data class User(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: Role = Role.CASHIER,
    val storeId: String = "",
    val phone: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp? = null,
    val isActive: Boolean = true
) {
    /**
     * Convert to map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "role" to role.name,
            "storeId" to storeId,
            "phone" to phone,
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt,
            "isActive" to isActive
        )
    }
    
    /**
     * Check if user is owner
     */
    fun isOwner(): Boolean = role == Role.OWNER
    
    companion object {
        const val COLLECTION_NAME = "Users"
    }
}
