package drp.screentime.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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

    // Check if message contains a notification payload.
    remoteMessage.notification?.let {
      Log.d(TAG, "Message Notification Body: ${it.body}")
      // Handle notification payload here.
    }
  }

  override fun onNewToken(token: String) {
    Log.d(TAG, "Refreshed token: $token")
    fcmToken = token
  }

  companion object {
    private const val TAG = "SCREENTIME"
    var fcmToken: String? = null

    fun createNotificationChannel(context: Context) {
      // Check if the device is running Android 8.0 or higher
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "poke_channel"
        val channelName = "Poke"
        val channelDescription = "Displayed when users poke you"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance).apply {
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
