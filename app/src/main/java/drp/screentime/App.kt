package drp.screentime

import android.app.Application
import android.content.Context
import drp.screentime.usage.DataUploadWorker
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.areAppNotificationsEnabled

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    DataUploadWorker.startPeriodicUploadWorker(applicationContext)
  }

  companion object {
    fun areAllPermissionsGranted(context: Context): Boolean {
      return UsageStatsProcessor.hasUsageStatsAccess(context) &&
          context.areAppNotificationsEnabled()
    }
  }
}
