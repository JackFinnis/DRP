package drp.screentime.usage

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import drp.screentime.R
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Short-lived monitoring service that uploads real-time screen time data to Firestore for a short
 * amount of time.
 *
 * TODO: (Compliance) must allow the user to opt out of this service and/or stop it at any time.
 */
class DataUploadService : Service() {

  companion object {
    /** Duration, in milliseconds, for which the service should run. */
    const val UPLOAD_DURATION = 3 * 60 * 1000L

    /** Tag for logging. */
    private const val TAG = "DataUploadService"

    /** Notification ID. */
    private const val NOTIFICATION_ID = 464

    /** Notification channel ID. */
    private const val CHANNEL_ID = "DataUploadService"
  }

  /** Time at which the service started. */
  private val startTime: Long = System.currentTimeMillis()

  /**
   * Define the job for this service.
   *
   * @see scope
   */
  private val job = SupervisorJob()

  /**
   * Define the execution scope for the service. Uses the IO dispatcher as the service is
   * network-bound.
   *
   * @see job
   */
  private val scope = CoroutineScope(Dispatchers.IO + job)

  /** Main logic of this service. */
  private fun monitorAppUsage() {
    scope.launch {
      val dataStoreManager = DataStoreManager(applicationContext)
      val usageStatsProcessor = UsageStatsProcessor(applicationContext)

      val userId = dataStoreManager.get(DataStoreManager.Key.USER_ID).firstOrNull()

      if (userId == null) {
        Log.e(TAG, "User ID not found. Stopping service.")
        stopSelf()
        return@launch
      }

      // Every second until the time limit is reached, upload device usage in real time
      while (System.currentTimeMillis() - startTime < UPLOAD_DURATION) {
        val totalUsage = usageStatsProcessor.getTotalUsage()
        val currentAppStat = usageStatsProcessor.getCurrentlyOpenApp()
        val currentApp = currentAppStat?.packageName

        FirestoreManager.setUserScore(userId, totalUsage) { success ->
          if (!success) Log.e(TAG, "Failed to upload user score.")
        }

        FirestoreManager.setUserCurrentApp(userId, currentApp, null) { success ->
          if (!success) Log.e(TAG, "Failed to upload current app.")
        }

        // Wait for a second before checking again.
        delay(1000)
      }
    }
  }

  private val notification: Notification
    get() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring app usage...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

  /** Service is not intended to be bound. */
  override fun onBind(intent: Intent?): IBinder? = null

  /** Called when the service is started. */
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    // Start the service in the foreground to prevent it from being killed.
    try {
      ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification,
          when {
            Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            Build.VERSION.SDK_INT >= VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
          })
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start service in the foreground.", e)
      return START_NOT_STICKY
    }

    // Start monitoring app usage.
    monitorAppUsage()

    return super.onStartCommand(intent, flags, startId)
  }

  /** Lifecycle cleanup. */
  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}