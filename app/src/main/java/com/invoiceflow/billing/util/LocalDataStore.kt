package com.invoiceflow.billing.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data store for persisting user session and preferences
 */
@Singleton
class LocalDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_STORE_ID = stringPreferencesKey("store_id")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        
        // Advanced Settings keys
        val KEY_STORE_NAME = stringPreferencesKey("store_name")
        val KEY_STORE_ADDRESS = stringPreferencesKey("store_address")
        val KEY_STORE_GST_NUMBER = stringPreferencesKey("store_gst_number")
        val KEY_STORE_GST_RATE = androidx.datastore.preferences.core.doublePreferencesKey("store_gst_rate")
        val KEY_STORE_CURRENCY = stringPreferencesKey("store_currency")
        val KEY_STORE_LOGO_PATH = stringPreferencesKey("store_logo_path")
        val KEY_RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val KEY_SHOW_GST_BREAKDOWN = booleanPreferencesKey("show_gst_breakdown")
        val KEY_SHOW_BARCODE = booleanPreferencesKey("show_barcode")
        val KEY_RECEIPT_PAPER_SIZE = stringPreferencesKey("receipt_paper_size")
        val KEY_DAILY_SUMMARY_NOTIF = booleanPreferencesKey("daily_summary_notif")
        val KEY_DAILY_SUMMARY_TIME = stringPreferencesKey("daily_summary_time")
        val KEY_LOW_STOCK_ALERT = booleanPreferencesKey("low_stock_alert")
        val KEY_LOW_STOCK_THRESHOLD = androidx.datastore.preferences.core.intPreferencesKey("low_stock_threshold")
        val KEY_SUB_EXPIRY_ALERT = booleanPreferencesKey("sub_expiry_alert")
    }
    
    /**
     * Save user session
     */
    suspend fun saveUserSession(userId: String, storeId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_STORE_ID] = storeId
            preferences[KEY_IS_LOGGED_IN] = true
        }
    }
    
    /**
     * Save onboarding completed status
     */
    suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
    
    /**
     * Get onboarding completed flow
     */
    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }
    
    /**
     * Get user ID flow
     */
    val userIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }
    
    /**
     * Get store ID flow
     */
    val storeIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_STORE_ID]
    }
    
    /**
     * Check if user is logged in
     */
    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }
    
    /**
     * Clear user session (logout)
     */
    suspend fun clearUserSession() {
        dataStore.edit { preferences ->
            preferences.clear()
            // Keep onboarding preference so they don't see it again on logout
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    data class StoreSettings(
        val storeName: String = "",
        val storeAddress: String = "",
        val gstNumber: String = "",
        val gstRate: Double = 18.0,
        val currency: String = "INR",
        val logoPath: String = "",
        val receiptFooter: String = "Thank you for your business!",
        val showGstBreakdown: Boolean = true,
        val showBarcode: Boolean = true,
        val paperSize: String = "80mm",
        val dailySummaryNotif: Boolean = true,
        val dailySummaryTime: String = "21:00",
        val lowStockAlert: Boolean = true,
        val lowStockThreshold: Int = 5,
        val subExpiryAlert: Boolean = true
    )

    val settingsFlow: Flow<StoreSettings> = dataStore.data.map { preferences ->
        StoreSettings(
            storeName = preferences[KEY_STORE_NAME] ?: "",
            storeAddress = preferences[KEY_STORE_ADDRESS] ?: "",
            gstNumber = preferences[KEY_STORE_GST_NUMBER] ?: "",
            gstRate = preferences[KEY_STORE_GST_RATE] ?: 18.0,
            currency = preferences[KEY_STORE_CURRENCY] ?: "INR",
            logoPath = preferences[KEY_STORE_LOGO_PATH] ?: "",
            receiptFooter = preferences[KEY_RECEIPT_FOOTER] ?: "Thank you for your business!",
            showGstBreakdown = preferences[KEY_SHOW_GST_BREAKDOWN] ?: true,
            showBarcode = preferences[KEY_SHOW_BARCODE] ?: true,
            paperSize = preferences[KEY_RECEIPT_PAPER_SIZE] ?: "80mm",
            dailySummaryNotif = preferences[KEY_DAILY_SUMMARY_NOTIF] ?: true,
            dailySummaryTime = preferences[KEY_DAILY_SUMMARY_TIME] ?: "21:00",
            lowStockAlert = preferences[KEY_LOW_STOCK_ALERT] ?: true,
            lowStockThreshold = preferences[KEY_LOW_STOCK_THRESHOLD] ?: 5,
            subExpiryAlert = preferences[KEY_SUB_EXPIRY_ALERT] ?: true
        )
    }

    suspend fun saveSettings(settings: StoreSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_STORE_NAME] = settings.storeName
            preferences[KEY_STORE_ADDRESS] = settings.storeAddress
            preferences[KEY_STORE_GST_NUMBER] = settings.gstNumber
            preferences[KEY_STORE_GST_RATE] = settings.gstRate
            preferences[KEY_STORE_CURRENCY] = settings.currency
            preferences[KEY_STORE_LOGO_PATH] = settings.logoPath
            preferences[KEY_RECEIPT_FOOTER] = settings.receiptFooter
            preferences[KEY_SHOW_GST_BREAKDOWN] = settings.showGstBreakdown
            preferences[KEY_SHOW_BARCODE] = settings.showBarcode
            preferences[KEY_RECEIPT_PAPER_SIZE] = settings.paperSize
            preferences[KEY_DAILY_SUMMARY_NOTIF] = settings.dailySummaryNotif
            preferences[KEY_DAILY_SUMMARY_TIME] = settings.dailySummaryTime
            preferences[KEY_LOW_STOCK_ALERT] = settings.lowStockAlert
            preferences[KEY_LOW_STOCK_THRESHOLD] = settings.lowStockThreshold
            preferences[KEY_SUB_EXPIRY_ALERT] = settings.subExpiryAlert
        }
    }
}
