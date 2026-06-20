package com.invoiceflow.billing.repository

import android.util.Log
import com.invoiceflow.billing.model.ActivityLog
import com.invoiceflow.billing.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BaseRepository() {

    companion object {
        private const val TAG = "ActivityLogRepository"
    }

    /**
     * Log a new activity event to Firestore
     */
    suspend fun logEvent(
        storeId: String,
        userId: String,
        userName: String,
        userEmail: String,
        actionType: String,
        details: Map<String, String> = emptyMap(),
        deviceInfo: String = ""
    ): Result<Unit> {
        return try {
            val logRef = firestore.collection("ActivityLogs")
                .document(storeId)
                .collection("logs")
                .document()

            val log = ActivityLog(
                logId = logRef.id,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                actionType = actionType,
                timestamp = Timestamp.now(),
                details = details,
                deviceInfo = deviceInfo
            )

            logRef.set(log.toMap()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing activity log", e)
            Result.Error(e, e.message ?: "Failed to write activity log")
        }
    }

    /**
     * Stream activity logs with in-memory filtering to bypass composite index requirements
     */
    fun getActivityLogs(
        storeId: String,
        userIdFilter: String? = null,
        actionFilter: String? = null,
        startDate: Date? = null,
        endDate: Date? = null,
        limit: Int = 500
    ): Flow<Result<List<ActivityLog>>> = callbackFlow {
        try {
            trySend(Result.Loading)
            
            val query = firestore.collection("ActivityLogs")
                .document(storeId)
                .collection("logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error, error.message ?: "Failed to stream activity logs"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ActivityLog::class.java)
                    }

                    // Apply filters in-memory
                    val filtered = logs.filter { log ->
                        val matchesUser = userIdFilter == null || log.userId == userIdFilter
                        val matchesAction = actionFilter == null || log.actionType == actionFilter
                        
                        val logDate = log.timestamp.toDate()
                        val matchesStart = startDate == null || logDate.after(startDate) || logDate.equals(startDate)
                        val matchesEnd = endDate == null || logDate.before(endDate) || logDate.equals(endDate)
                        
                        matchesUser && matchesAction && matchesStart && matchesEnd
                    }

                    trySend(Result.Success(filtered))
                }
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity logs flow", e)
            close(e)
        }
    }
}
