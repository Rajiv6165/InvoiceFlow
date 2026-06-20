package com.invoiceflow.billing.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.Product
import com.invoiceflow.billing.util.LocalDataStore
import com.invoiceflow.billing.util.LocalDataStore.StoreSettings
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localDataStore: LocalDataStore,
    private val firestore: FirebaseFirestore
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    // Expose settings state flow
    val storeSettings: StateFlow<StoreSettings> = localDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettings()
        )
        
    fun updateSettings(newSettings: StoreSettings) {
        viewModelScope.launch {
            try {
                localDataStore.saveSettings(newSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings", e)
            }
        }
    }
    
    fun exportProductsToCsv(context: Context, storeId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = firestore.collection(Product.COLLECTION_NAME)
                    .whereEqualTo("storeId", storeId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                    
                val products = result.documents.mapNotNull { it.toObject(Product::class.java) }
                
                // Construct CSV String
                val csvContent = StringBuilder()
                csvContent.append("Product ID,Name,Price,SKU,Barcode,Stock Qty,Min Stock Level,Category\n")
                
                products.forEach { p ->
                    // Escape names and categories in quotes to preserve formatting
                    val nameEscaped = p.name.replace("\"", "\"\"")
                    val categoryEscaped = p.category.replace("\"", "\"\"")
                    csvContent.append("${p.productId},\"$nameEscaped\",${p.price},${p.sku},${p.barcode},${p.stockQty},${p.minStockLevel},\"$categoryEscaped\"\n")
                }
                
                // Write to cache directory
                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                val exportFile = File(exportDir, "InvoiceFlow_Products_${System.currentTimeMillis()}.csv")
                exportFile.writeText(csvContent.toString())
                
                // Share file
                val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.invoiceflow.billing.provider",
                    exportFile
                )
                
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "InvoiceFlow Products Export")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share Export CSV")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error during CSV generation/export", e)
                onComplete(false)
            }
        }
    }
    
    fun clearLocalPreferences(onComplete: () -> Unit) {
        viewModelScope.launch {
            localDataStore.clearUserSession()
            onComplete()
        }
    }
}
