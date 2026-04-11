package com.invoiceflow.billing.util

/**
 * Application-wide constants
 */
object Constants {
    
    // Firestore Collections
    const val STORES_COLLECTION = "Stores"
    const val USERS_COLLECTION = "Users"
    const val PRODUCTS_COLLECTION = "Products"
    const val INVOICES_COLLECTION = "Invoices"
    const val STORE_SETTINGS_COLLECTION = "StoreSettings"
    
    // Shared Preferences / DataStore
    const val PREF_NAME = "invoiceflow_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_STORE_ID = "store_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // Default Values
    const val DEFAULT_GST_RATE = 18.0
    const val DEFAULT_CURRENCY = "INR"
    const val DEFAULT_STORE_NAME = "Kothari Provision Store"
    
    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PRODUCT_NAME_LENGTH = 100
    const val MAX_ADDRESS_LENGTH = 200
    
    // Firebase Settings
    const val FIRESTORE_CACHE_SIZE_BYTES = 104857600L // 100 MB
}
