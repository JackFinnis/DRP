package drp.screentime.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull

class ScreenTimeUploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

  companion object {
    const val WORKER_NAME_TAG = "screenTimeUploadWork"
  }

  override suspend fun doWork(): Result {
    var failed = false

    // Fetch user id and usage stats
    val dataStoreManager = DataStoreManager(applicationContext)
    val usageStatsProcessor =
        UsageStatsProcessor(
            applicationContext.packageManager,
            applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
    val firestoreManager = FirestoreManager()

    val usageData = usageStatsProcessor.getApplicationUsageStats()
    val totalUsage = usageStatsProcessor.getTotalUsage()

    dataStoreManager.userIdFlow.firstOrNull()?.let { userId ->
      firestoreManager.uploadUsageData(userId, usageData) { success -> if (!success) failed = true }
      firestoreManager.setUserScore(userId, totalUsage) { success -> if (!success) failed = true }
    }

    return if (failed) Result.retry() else Result.success()
  }
}
