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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("InvoiceFlow Application started")
    }
}
