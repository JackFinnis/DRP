package drp.screentime.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import drp.screentime.usage.DataUploader

class MessagingService : FirebaseMessagingService() {

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Handle FCM messages here.
    Log.d(TAG, "From: ${remoteMessage.from}")

    // Check if message contains a data payload.
    remoteMessage.data.isNotEmpty().let {
      Log.d(TAG, "Message data payload: " + remoteMessage.data)

      when (val action = remoteMessage.data["action"]) {
        Actions.REQUEST_DATA_UPDATE -> DataUploader.uploadAsap(applicationContext)
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
  }

  object Actions {
    /** Remote request for latest screen time data. */
    const val REQUEST_DATA_UPDATE = "request_data_update"
  }
}
