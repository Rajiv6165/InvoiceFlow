package com.invoiceflow.billing.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Helper object for managing runtime permissions
 */
object PermissionHelper {
    
    const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if storage permission is granted (for saving PDFs)
     */
    fun hasStoragePermission(context: Context): Boolean {
        // For Android 13+, we don't need external storage permission for app-specific directories
        return true
    }
}
