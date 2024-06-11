package drp.screentime

import android.app.Application
import android.content.Context
import drp.screentime.notification.MessagingService
import drp.screentime.usage.DataUploadService
import drp.screentime.usage.DataUploadWorker
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.areAppNotificationsEnabled
import drp.screentime.util.areBatteryOptimisationsDisabled

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    DataUploadWorker.startPeriodicUploadWorker(applicationContext)
    MessagingService.createNotificationChannel(this)
    DataUploadService.startService(applicationContext)
  }

  companion object {
    fun areAllPermissionsGranted(context: Context): Boolean {
      return UsageStatsProcessor.hasUsageStatsAccess(context) &&
          context.areBatteryOptimisationsDisabled() &&
          context.areAppNotificationsEnabled()
    }
  }
}
