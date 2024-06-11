package drp.screentime.usage

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Handles both instantaneous and periodic upload of snapshot screen time data to Firestore.
 *
 * @see WorkManager for more information on how the worker is scheduled.
 */
class DataUploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    var failed = false

    // Fetch user id and usage stats
    val dataStoreManager = DataStoreManager(applicationContext)
    val usageStatsProcessor = UsageStatsProcessor(applicationContext)

    val totalUsage = usageStatsProcessor.getTotalUsage()
    dataStoreManager.get(DataStoreManager.Key.USER_ID).firstOrNull()?.let { userId ->
      FirestoreManager.setUserScore(userId, totalUsage) { success -> if (!success) failed = true }
    }

    return if (failed) Result.retry() else Result.success()
  }

  companion object {
    /** Schedule a one-off expedited task to upload screen time data to Firestore. */
    fun uploadAsap(context: Context) {
      val workRequest =
          OneTimeWorkRequestBuilder<DataUploadWorker>()
              .setConstraints(getConstraints())
              .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
              .build()

      WorkManager.getInstance(context)
          .enqueueUniqueWork(
              WorkType.ONE_OFF_FAST.workName, ExistingWorkPolicy.REPLACE, workRequest)
    }

    /**
     * Frequency of background usage stats upload jobs in minutes. Executes with ±5 minutes leeway.
     */
    private const val PERIODIC_JOB_FREQUENCY = 15L

    /** Schedule a periodic task to upload screen time data to Firestore. */
    fun startPeriodicUploadWorker(context: Context) {
      val builder =
          PeriodicWorkRequestBuilder<DataUploadWorker>(PERIODIC_JOB_FREQUENCY, TimeUnit.MINUTES)
      builder.setConstraints(getConstraints())
      val request = builder.build()

      WorkManager.getInstance(context)
          .enqueueUniquePeriodicWork(
              WorkType.PERIODIC.workName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Constraints for the worker to execute successfully. */
    private fun getConstraints() =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Require network connection
            .setRequiresDeviceIdle(false) // Only runs when the screen is on
            .build()
  }

  /**
   * Enum class to define the type of work to be done by the worker.
   *
   * @param workName The unique work name to identify the task, as provided to
   *   [WorkManager.enqueueUniqueWork].
   */
  enum class WorkType(val workName: String) {
    /**
     * Periodic work to upload screen time data to Firestore. Uploads screen time data every 15±5
     * minutes.
     */
    PERIODIC("screenTimeUploadWork"),

    /**
     * One-off expedited work to upload screen time data to Firestore. Uploads screen time data as
     * soon as possible.
     */
    ONE_OFF_FAST("oneOffScreenTimeUploadWorkFast"),
  }
}
