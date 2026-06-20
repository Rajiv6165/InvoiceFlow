package com.invoiceflow.billing

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for InvoiceFlow billing system
 * Initializes Hilt and other app-level components
 */
@HiltAndroidApp
class BillingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging (only in debug builds)
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Enqueue periodic background database sync
        com.invoiceflow.billing.util.BackgroundSyncWorker.enqueuePeriodicSync(this)
        
        Timber.d("InvoiceFlow Application started")
    }
}
