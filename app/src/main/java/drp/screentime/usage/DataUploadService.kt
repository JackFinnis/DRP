package drp.screentime.usage

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import java.util.Date

/**
 * Short-lived monitoring service that uploads real-time screen time data to Firestore for a short
 * amount of time.
 *
 * TODO: (Compliance) must allow the user to opt out of this service and/or stop it at any time.
 */
class DataUploadService : Service() {

  companion object {
    /** Tag for logging. */
    private const val TAG = "DataUploadService"

    /** Notification ID. */
    private const val NOTIFICATION_ID = 464

    /** Notification channel ID. */
    private const val CHANNEL_ID = "DataUploadService"

    /** Starts the service. */
    fun startService(context: android.content.Context) {
      val startIntent = Intent(context, DataUploadService::class.java)
      if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
        context.startForegroundService(startIntent)
      } else {
        context.startService(startIntent)
      }
    }
  }

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
  private val scope = CoroutineScope(job + Dispatchers.IO)

  /** Main logic of this service. */
  private suspend fun monitorAppUsage() {
    val dataStoreManager = DataStoreManager(applicationContext)
    val usageStatsProcessor = UsageStatsProcessor(applicationContext)

    val userId = dataStoreManager.get(DataStoreManager.Key.USER_ID).firstOrNull()

    if (userId == null) {
      Log.e(TAG, "User ID not found. Stopping service.")
      stopSelf()
      return
    }

    // Every second until the time limit is reached, upload device usage in real time
    while (true) {
      Log.d(TAG, "Uploading data...")
      val totalUsage = usageStatsProcessor.getTotalUsage()
      val (currentApp, currentAppSince) = usageStatsProcessor.getLastAppOpen() ?: Pair(null, null)

      FirestoreManager.setUserScore(userId, totalUsage) { success ->
        if (!success) Log.e(TAG, "Failed to upload user score.")
      }

      FirestoreManager.setUserCurrentApp(
          userId,
          currentApp,
          currentAppSince?.let { Date(it) },
      ) { success ->
        if (!success) Log.e(TAG, "Failed to upload current app.")
      }

      // Wait for a second before checking again.
      delay(5000)
    }
  }

  private val notification: Notification
    get() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring app usage...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

  private val notificationChannel: NotificationChannelCompat
    get() =
        NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_LOW)
            .setName("Data Upload Service")
            .setDescription("Uploads real-time screen time data.")
            .build()

  /** Service is not intended to be bound. */
  override fun onBind(intent: Intent?): IBinder? = null

  /** Called when the service is started. */
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i(TAG, "Starting service.")

    // Create the notification channel (if it doesn't already exist).
    NotificationManagerCompat.from(this).createNotificationChannel(notificationChannel)

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
    scope.launch { monitorAppUsage() }

    return super.onStartCommand(intent, flags, startId)
  }

  /** Lifecycle cleanup. */
  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
