package drp.screentime

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import drp.screentime.usage.ScreenTimeUploadWorker
import java.util.concurrent.TimeUnit

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        enqueueStatsUploadWorker()
    }

    private fun enqueueStatsUploadWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresDeviceIdle(false) // Avoid redundancy, workers already run when unlocked
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ScreenTimeUploadWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            ScreenTimeUploadWorker.WORKER_NAME_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }
}
