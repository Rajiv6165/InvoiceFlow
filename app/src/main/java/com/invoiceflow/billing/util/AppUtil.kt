package com.invoiceflow.billing.util

import android.app.Application
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper object to access application context from anywhere
 */
object AppUtil {
    
    @Volatile
    private var application: Application? = null
    
    fun init(app: Application) {
        application = app
    }
    
    fun getApplication(): Application {
        return application ?: throw IllegalStateException("Application not initialized")
    }
}
