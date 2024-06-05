package drp.screentime.usage

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import drp.screentime.util.getAppName
import drp.screentime.util.isTrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class AppUsageTrackingService : AccessibilityService() {

  /**
   * The package name of the last known used app. Nullified when a system, hidden or otherwise
   * mundane app is used.
   */
  private var currentApp: String? = null

  /** The time when the current app was opened. */
  private var currentAppOpened: Date? = null

  /** The user ID for which to track app usage. This is set in [onServiceConnected]. */
  private var userId: String? = null

  /** The time at which the last app usage was posted. */
  private var lastPosted = System.currentTimeMillis()

  private val db = FirestoreManager()
  private lateinit var dataStoreManager: DataStoreManager

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    if (event.packageName == null) return

    val packageName: String = event.packageName.toString()
    val userId: String = userId ?: return

    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

      val app: String? = if (packageManager.isTrackedApp(packageName)) packageName else null

      /* TODO reliability: current time may not be the actual time the
      app was opened. Use event time instead. */
      val openTime: Date = Date()

      // Avoids unnecessary updates
      if (app == currentApp) return

      currentApp = app?.let { packageManager.getAppName(it) }
      currentAppOpened = openTime

      db.updateCurrentApp(userId, currentApp, currentAppOpened) { success ->
        if (success) {
          lastPosted = System.currentTimeMillis()
        }
      }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    dataStoreManager = DataStoreManager(applicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      dataStoreManager.userIdFlow.collect { id -> userId = id }
    }
  }

  override fun onInterrupt() {
    Log.e(TAG, "Service interrupted")
  }

  companion object {
    const val TAG = "AppUsageTrackingService"

    fun isEnabled(context: Context): Boolean {
      var enabled = 0
      try {
        enabled =
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
      } catch (e: Settings.SettingNotFoundException) {
        Log.e("Accessibility", "Error finding setting, default accessibility to not found: $e")
      }
      if (enabled == 1) {
        val name = ComponentName(context, AppUsageTrackingService::class.java)
        val services =
            Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return services?.contains(name.flattenToString()) ?: false
      }
      return false
    }
  }
}
