package drp.screentime.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Handle FCM messages here.
    Log.d(TAG, "From: ${remoteMessage.from}")

    // Check if message contains a data payload.
    remoteMessage.data.isNotEmpty().let {
      Log.d(TAG, "Message data payload: " + remoteMessage.data)
      // Handle data payload here.
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
}
