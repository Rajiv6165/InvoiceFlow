package com.invoiceflow.billing.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker to sync offline Firestore data with cloud database
 */
class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "invoiceflow_sync_channel"
        private const val NOTIFICATION_ID = 1002
        
        /**
         * Enqueue periodic background sync work every 15 minutes when connected
         */
        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "InvoiceFlowBackgroundSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Setup Notification Channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cloud Data Synchronization",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        // Build progress notification
        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("InvoiceFlow Sync")
            .setContentText("Uploading pending bills to cloud...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setProgress(100, 0, true) // Indeterminate progress
            .setOngoing(true)
            .setAutoCancel(false)
            
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Wait for firestore writes to finish
            firestore.waitForPendingWrites().await()
            
            // Update notification with success
            val successNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("InvoiceFlow Sync")
                .setContentText("All transactions synced and backed up successfully")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
                
            notificationManager.notify(NOTIFICATION_ID, successNotification)
            Result.success()
        } catch (e: Exception) {
            // Update notification with retry warning
            val errorNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Sync Paused")
                .setContentText("Sync failed. Retrying when connection is stable.")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
                
            notificationManager.notify(NOTIFICATION_ID, errorNotification)
            Result.retry()
        }
    }
}
