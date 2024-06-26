package drp.screentime.usage

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
    /** Tag for logging. */
    private const val TAG = "DataUploadService"

    /** Notification ID. */
    private const val NOTIFICATION_ID = 464

    /** Notification channel ID. */
    private const val CHANNEL_ID = "DataUploadService"

    /** Starts the service. */
    fun startService(context: Context) {
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

  private fun isDeviceAwake(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isInteractive
  }

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

    var firstTime = true
    var lastApp: AppLiveUsageInfo? = null
    val lastTimePerApp = mutableMapOf<String, Pair<Long, Long>>()

    // Every second until the time limit is reached, upload device usage in real time
    while (true) {
      Log.d(TAG, "Uploading data...")

      val isAwake = isDeviceAwake(applicationContext)

      val currentAppData = if (isAwake) usageStatsProcessor.getLastAppOpen() else null

      if (lastApp != currentAppData || firstTime) {
        lastApp =
            if (lastApp != null && lastApp.packageName == currentAppData?.packageName) {
              // if app is same, but activity is different
              currentAppData.copy(usedSince = lastApp.usedSince)
            } else {
              // if app changed, or app closed, or app opened

              if (lastApp != null) {
                // if app changed or closed, update its close time
                lastTimePerApp[lastApp.packageName] =
                    Pair(lastApp.usedSince, System.currentTimeMillis())

                // TODO: remove once logging finished
                val lA = lastApp
                FirebaseFirestore.getInstance()
                    .collection("config")
                    .document("logging")
                    .get()
                    .addOnSuccessListener { data ->
                      if (data.getBoolean("leaves")!!) {
                        val ref = FirebaseFirestore.getInstance().collection("appLeaves").document()
                        ref.set(
                            mapOf(
                                "packageName" to lA.packageName,
                                "userID" to userId,
                                "timestamp" to Timestamp.now()))
                      }
                    }
              }

              var currentData = currentAppData

              if (currentAppData != null) {
                // if user is currently in an app

                val lastTimes = lastTimePerApp[currentAppData.packageName]
                if (lastTimes != null) {
                  val (lastStartTime, lastCloseTime) = lastTimes
                  if (currentAppData.usedSince - lastCloseTime < 15 * 1000) {
                    // if user was in same app within last few seconds
                    currentData = currentAppData.copy(usedSince = lastStartTime)
                  }
                }
              }

              currentData
            }

        firstTime = false

        FirestoreManager.setUserCurrentApp(userId, lastApp) { success ->
          if (!success) Log.e(TAG, "Failed to upload current app.")
        }

        val totalUsage = usageStatsProcessor.getTotalUsage()

        FirestoreManager.setUserScore(userId, totalUsage) { success ->
          if (!success) Log.e(TAG, "Failed to upload user score.")
        }
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

    return START_STICKY
  }

  /** Lifecycle cleanup. */
  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
