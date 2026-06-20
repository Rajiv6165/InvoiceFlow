package com.invoiceflow.billing.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Audit log event representing security, data mutations, and cashier activity.
 */
data class ActivityLog(
    @DocumentId
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val actionType: String = "", // USER_LOGIN, USER_LOGOUT, INVOICE_CREATED, PRODUCT_ADDED, PRODUCT_UPDATED, PRODUCT_DELETED, BARCODE_SCANNED, SUBSCRIPTION_CHECKED
    val timestamp: Timestamp = Timestamp.now(),
    val details: Map<String, String> = emptyMap(),
    val deviceInfo: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "logId" to logId,
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "actionType" to actionType,
            "timestamp" to timestamp,
            "details" to details,
            "deviceInfo" to deviceInfo
        )
    }
}

/**
 * Session tracking for cashier work hours and real-time sales tallies.
 */
data class Shift(
    @DocumentId
    val shiftId: String = "",
    val userId: String = "",
    val userName: String = "",
    val storeId: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val totalRevenue: Double = 0.0,
    val totalBills: Int = 0,
    val status: String = "OPEN" // "OPEN", "CLOSED"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "shiftId" to shiftId,
            "userId" to userId,
            "userName" to userName,
            "storeId" to storeId,
            "startTime" to startTime,
            "endTime" to endTime,
            "totalRevenue" to totalRevenue,
            "totalBills" to totalBills,
            "status" to status
        )
    }
}

/**
 * Performance snapshot for cashiers.
 */
data class CashierPerformance(
    val totalBills: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageBillValue: Double = 0.0,
    val billsToday: Int = 0,
    val revenueToday: Double = 0.0,
    val bestPerformingDay: String = "N/A"
)
