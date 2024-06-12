package drp.screentime.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import drp.screentime.R
import drp.screentime.usage.DataUploadWorker

class MessagingService : FirebaseMessagingService() {

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Handle FCM messages here.
    Log.d(TAG, "From: ${remoteMessage.from}")

    // Check if message contains a data payload.
    remoteMessage.data.isNotEmpty().let {
      Log.d(TAG, "Message data payload: " + remoteMessage.data)

      when (val action = remoteMessage.data["action"]) {
        Actions.REQUEST_DATA_UPDATE -> DataUploadWorker.uploadAsap(applicationContext)
        else -> Log.d(TAG, "Unknown action: $action")
      }
    }

    remoteMessage.notification?.let {
      showNotification(it)
    }
  }

  override fun onNewToken(token: String) {
    Log.d(TAG, "Refreshed token: $token")
    fcmToken = token
  }

  private fun showNotification(notification: RemoteMessage.Notification) {
    val notificationBuilder =
      NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(notification.title).setContentText(notification.body)
        .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
  }

  companion object {
    private const val TAG = "ScreenTimeMessagingService"
    private const val CHANNEL_ID = "poke_channel"
    var fcmToken: String? = null

    fun createNotificationChannel(context: Context) {
      // Check if the device is running Android 8.0 or higher
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = "Poke"
        val channelDescription = "Displayed when users poke you"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
          description = channelDescription
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  object Actions {
    /** Remote request for latest screen time data. */
    const val REQUEST_DATA_UPDATE = "request_data_update"
  }
}
