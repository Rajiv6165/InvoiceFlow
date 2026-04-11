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
        }
    }
}
