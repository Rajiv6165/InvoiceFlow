package com.invoiceflow.billing.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.invoiceflow.billing.model.Store
import com.invoiceflow.billing.model.SubscriptionStatus
import com.invoiceflow.billing.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "SubscriptionRepository"
        
        // Cache preference keys
        private val KEY_SUB_STATUS = stringPreferencesKey("sub_status")
        private val KEY_SUB_PLAN = stringPreferencesKey("sub_plan")
        private val KEY_SUB_EXPIRY = longPreferencesKey("sub_expiry")
        private val KEY_SUB_TRIAL_END = longPreferencesKey("sub_trial_end")
        private val KEY_SUB_SUSPENSION_REASON = stringPreferencesKey("sub_suspension_reason")
        private val KEY_SUB_LAST_CHECK = longPreferencesKey("sub_last_check")
    }

    /**
     * Check current subscription status from cache or Firestore (24 hour cache expiration)
     */
    suspend fun checkSubscriptionStatus(storeId: String, forceRefresh: Boolean = false): SubscriptionStatus {
        if (storeId.isBlank()) return SubscriptionStatus.Error
        
        val currentTime = System.currentTimeMillis()
        
        // 1. Read cache from DataStore
        val prefs = dataStore.data.first()
        val cachedTime = prefs[KEY_SUB_LAST_CHECK]
        val cachedStatus = prefs[KEY_SUB_STATUS]
        
        if (!forceRefresh && cachedTime != null && cachedStatus != null && (currentTime - cachedTime < 24 * 60 * 60 * 1000L)) {
            Log.d(TAG, "Using cached subscription status: $cachedStatus")
            return when (cachedStatus) {
                "active" -> SubscriptionStatus.Active
                "trial" -> {
                    val trialEnd = prefs[KEY_SUB_TRIAL_END]
                    if (trialEnd != null && trialEnd < currentTime) {
                        val days = ((currentTime - trialEnd) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                        SubscriptionStatus.Expired(days)
                    } else {
                        SubscriptionStatus.Trial
                    }
                }
                "expired" -> {
                    val expiry = prefs[KEY_SUB_EXPIRY] ?: prefs[KEY_SUB_TRIAL_END] ?: currentTime
                    val days = ((currentTime - expiry) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                    SubscriptionStatus.Expired(days)
                }
                "suspended" -> {
                    val reason = prefs[KEY_SUB_SUSPENSION_REASON] ?: "Account suspended"
                    SubscriptionStatus.Suspended(reason)
                }
                else -> SubscriptionStatus.Trial
            }
        }
        
        // 2. Fetch fresh from Firestore
        Log.d(TAG, "Fetching subscription status from Firestore for store: $storeId")
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            if (storeDoc.exists()) {
                val store = storeDoc.toObject(Store::class.java)
                if (store != null) {
                    val status = store.subscriptionStatus
                    val plan = store.planType
                    val expiryMs = store.subscriptionExpiryDate?.toDate()?.time
                    val trialEndMs = store.trialEndDate.toDate().time
                    val suspensionReason = store.suspensionReason ?: ""
                    
                    // Write to DataStore cache
                    dataStore.edit { preferences ->
                        preferences[KEY_SUB_STATUS] = status
                        preferences[KEY_SUB_PLAN] = plan
                        if (expiryMs != null) {
                            preferences[KEY_SUB_EXPIRY] = expiryMs
                        } else {
                            preferences.remove(KEY_SUB_EXPIRY)
                        }
                        preferences[KEY_SUB_TRIAL_END] = trialEndMs
                        preferences[KEY_SUB_SUSPENSION_REASON] = suspensionReason
                        preferences[KEY_SUB_LAST_CHECK] = currentTime
                    }
                    
                    // Evaluate current status
                    when (status) {
                        "active" -> {
                            if (expiryMs != null && expiryMs < currentTime) {
                                // Subscription expired
                                val days = ((currentTime - expiryMs) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                                updateStoreStatus(storeId, "expired")
                                SubscriptionStatus.Expired(days)
                            } else {
                                SubscriptionStatus.Active
                            }
                        }
                        "trial" -> {
                            if (trialEndMs < currentTime) {
                                // Trial expired
                                val days = ((currentTime - trialEndMs) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                                updateStoreStatus(storeId, "expired")
                                SubscriptionStatus.Expired(days)
                            } else {
                                SubscriptionStatus.Trial
                            }
                        }
                        "expired" -> {
                            val baseMs = expiryMs ?: trialEndMs
                            val days = ((currentTime - baseMs) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                            SubscriptionStatus.Expired(days)
                        }
                        "suspended" -> {
                            SubscriptionStatus.Suspended(suspensionReason)
                        }
                        else -> SubscriptionStatus.Error
                    }
                } else {
                    SubscriptionStatus.Error
                }
            } else {
                SubscriptionStatus.Error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription status from Firestore", e)
            SubscriptionStatus.Error
        }
    }

    /**
     * Get remaining trial days (returns 0 if expired or not on trial)
     */
    suspend fun getRemainingTrialDays(storeId: String): Int {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            val store = storeDoc.toObject(Store::class.java)
            if (store != null && store.subscriptionStatus == "trial") {
                val current = System.currentTimeMillis()
                val trialEnd = store.trialEndDate.toDate().time
                if (trialEnd > current) {
                    ((trialEnd - current) / (24 * 60 * 60 * 1000L)).toInt()
                } else {
                    0
                }
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining trial days", e)
            0
        }
    }

    /**
     * Check if a store has access to a specific feature name
     */
    suspend fun checkFeatureAccess(storeId: String, featureName: String): Boolean {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            val store = storeDoc.toObject(Store::class.java)
            if (store != null) {
                if (store.subscriptionStatus == "suspended" || store.subscriptionStatus == "expired") {
                    return false
                }
                store.featuresEnabled.contains(featureName)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking feature access", e)
            false
        }
    }

    /**
     * Check if the product count is under the plan limit
     */
    suspend fun isWithinProductLimit(storeId: String): Boolean {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            val store = storeDoc.toObject(Store::class.java)
            if (store != null) {
                if (store.maxProductsAllowed >= 99999) return true // Unlimited
                
                val countSnapshot = firestore.collection("Products")
                    .whereEqualTo("storeId", storeId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                countSnapshot.size() < store.maxProductsAllowed
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking product limit", e)
            false
        }
    }

    /**
     * Check if the staff count is under the plan limit
     */
    suspend fun isWithinStaffLimit(storeId: String): Boolean {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            val store = storeDoc.toObject(Store::class.java)
            if (store != null) {
                if (store.maxStaffAccounts >= 99999) return true // Unlimited
                
                val countSnapshot = firestore.collection("Users")
                    .whereEqualTo("storeId", storeId)
                    .get()
                    .await()
                
                countSnapshot.size() < store.maxStaffAccounts
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking staff limit", e)
            false
        }
    }

    /**
     * Get store name from Firestore
     */
    suspend fun getStoreName(storeId: String): String {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            val store = storeDoc.toObject(Store::class.java)
            store?.name ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching store name", e)
            ""
        }
    }

    /**
     * Helper method to update subscriptionStatus directly in Firestore when client detects expiration
     */
    private suspend fun updateStoreStatus(storeId: String, status: String) {
        try {
            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(
                    mapOf(
                        "subscriptionStatus" to status,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-update store status to $status", e)
        }
    }
}

