package com.invoiceflow.billing.repository

import com.invoiceflow.billing.model.Store
import com.invoiceflow.billing.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseActivationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Activate subscription license for a store
     */
    suspend fun activateLicense(storeId: String, planType: String, durationMonths: Int): Result<Unit> {
        return try {
            val maxStaff = when (planType.lowercase()) {
                "starter" -> 2
                "professional" -> 5
                else -> 99999
            }
            val maxProducts = when (planType.lowercase()) {
                "starter" -> 100
                "professional" -> 500
                else -> 99999
            }
            val features = when (planType.lowercase()) {
                "starter" -> listOf("analytics", "pdf")
                "professional" -> listOf("analytics", "staff", "pdf", "export")
                else -> listOf("analytics", "staff", "pdf", "export", "custom_branding")
            }

            val cal = Calendar.getInstance()
            val start = cal.time
            cal.add(Calendar.MONTH, durationMonths)
            val expiry = cal.time

            val updates = mapOf(
                "subscriptionStatus" to "active",
                "planType" to planType.lowercase(),
                "subscriptionStartDate" to Timestamp(start),
                "subscriptionExpiryDate" to Timestamp(expiry),
                "maxStaffAccounts" to maxStaff,
                "maxProductsAllowed" to maxProducts,
                "featuresEnabled" to features,
                "lastPaymentDate" to Timestamp(start),
                "suspensionReason" to null,
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(updates)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to activate license")
        }
    }

    /**
     * Extend a store's free trial session by X days
     */
    suspend fun extendTrial(storeId: String, additionalDays: Int): Result<Unit> {
        return try {
            val doc = firestore.collection(Store.COLLECTION_NAME).document(storeId).get().await()
            val store = doc.toObject(Store::class.java)
                ?: return Result.Error(Exception("Store not found"), "Store not found")

            val currentTrialEnd = store.trialEndDate.toDate()
            val cal = Calendar.getInstance()
            cal.time = currentTrialEnd
            cal.add(Calendar.DAY_OF_YEAR, additionalDays)

            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(
                    mapOf(
                        "subscriptionStatus" to "trial",
                        "trialEndDate" to Timestamp(cal.time),
                        "suspensionReason" to null,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to extend trial")
        }
    }

    /**
     * Suspend a store with a private reason note
     */
    suspend fun suspendAccount(storeId: String, reason: String): Result<Unit> {
        return try {
            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(
                    mapOf(
                        "subscriptionStatus" to "suspended",
                        "suspensionReason" to reason,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to suspend account")
        }
    }

    /**
     * Reactivate a suspended store account
     */
    suspend fun reactivateAccount(storeId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(Store.COLLECTION_NAME).document(storeId).get().await()
            val store = doc.toObject(Store::class.java)
                ?: return Result.Error(Exception("Store not found"), "Store not found")

            // Determine if trial or subscription is already expired
            val current = System.currentTimeMillis()
            val resolvedStatus = when {
                store.subscriptionExpiryDate != null && store.subscriptionExpiryDate.toDate().time > current -> "active"
                store.subscriptionExpiryDate == null && store.trialEndDate.toDate().time > current -> "trial"
                else -> "expired"
            }

            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(
                    mapOf(
                        "subscriptionStatus" to resolvedStatus,
                        "suspensionReason" to null,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to reactivate account")
        }
    }

    /**
     * Change plan type tier (starter, professional, enterprise)
     */
    suspend fun upgradeDowngradePlan(storeId: String, newPlanType: String): Result<Unit> {
        return try {
            val maxStaff = when (newPlanType.lowercase()) {
                "starter" -> 2
                "professional" -> 5
                else -> 99999
            }
            val maxProducts = when (newPlanType.lowercase()) {
                "starter" -> 100
                "professional" -> 500
                else -> 99999
            }
            val features = when (newPlanType.lowercase()) {
                "starter" -> listOf("analytics", "pdf")
                "professional" -> listOf("analytics", "staff", "pdf", "export")
                else -> listOf("analytics", "staff", "pdf", "export", "custom_branding")
            }

            firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .update(
                    mapOf(
                        "planType" to newPlanType.lowercase(),
                        "maxStaffAccounts" to maxStaff,
                        "maxProductsAllowed" to maxProducts,
                        "featuresEnabled" to features,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to upgrade/downgrade plan")
        }
    }
}
