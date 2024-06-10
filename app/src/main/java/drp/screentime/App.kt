package drp.screentime

import android.app.Application
import android.content.Context
import drp.screentime.usage.DataUploader
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.areAppNotificationsEnabled

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    DataUploader.startPeriodicUploadWorker(applicationContext)
  }

  companion object {
    fun areAllPermissionsGranted(context: Context): Boolean {
      return UsageStatsProcessor.hasUsageStatsAccess(context) &&
          context.areAppNotificationsEnabled()
    }
  }
}
