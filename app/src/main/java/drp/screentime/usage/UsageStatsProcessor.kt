package drp.screentime.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.firebase.Timestamp
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.UsageEvent
import drp.screentime.util.addDays
import drp.screentime.util.getActivityName
import drp.screentime.util.getAllInstalledApps
import drp.screentime.util.getAppName
import drp.screentime.util.getHomeScreenLaunchers
import drp.screentime.util.getMidnight
import drp.screentime.util.isSystemApp
import drp.screentime.util.iterator
import java.util.Date

class UsageStatsProcessor(
    private val pm: PackageManager, private val usageStatsManager: UsageStatsManager
) {

    companion object {
        private const val TAG: String = "UsageStatsProcessor"
        val TRACKED_EVENT_TYPES = setOf(
//            0, // No event type??

            1, // Moved to foreground
            2, // Moved to background
//            7, // User interaction
//            15, // Screen interactive
//            16, // Screen non-interactive
//            17, // Keyguard shown
//            18, // Keyguard hidden
            23, // ACTIVITY_STOPPED
            24, // ACTIVITY_DESTROYED
//            26, // Device shut down
//            27, // Device started up
//            28, // USER_UNLOCKED
//            29, // USER_STOPPED

//            3, // End of day
//            4, // Continue previous day
//
//            10, // Notification viewed
//            12, // Notification posted
//
//            6, // System interaction
//            31, // App component used
        )

        val EVENT_TYPES = mapOf(
            1 to 1, 2 to 0
        )
    }

    private val ignoredPackages: Set<String> =
        setOf("drp.screentime") + pm.getHomeScreenLaunchers().toSet() + pm.getAllInstalledApps()
            .filter { pm.isSystemApp(it) }.toSet()

    /** Get the usage stats for the given day.
     * @param day The day to get the usage stats for. Defaults to today.
     * @return A list of usage stats for the given day.
     */
    private fun getUsageStats(day: Date = Date()): List<UsageStats> {
        val startTime = getMidnight(day)
        val endTime = getMidnight(addDays(day, 1))

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        return stats.filter { it.totalTimeInForeground > 0 }
            .filterNot { it.packageName in ignoredPackages }
    }

    private fun UsageStats.totalUsageMillis(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) totalTimeVisible else totalTimeInForeground
    }

    fun getApplicationUsageStats(day: Date = Date()): Map<String, Long> {
        return getUsageStats(day).associate {
            it.packageName to it.totalUsageMillis() / 1000
        }
    }

    fun getTotalUsage(day: Date = Date()): Long {
        return getUsageStats(day).sumOf { it.totalUsageMillis() } / 1000
    }

    private val appNames: MutableMap<String, String> = mutableMapOf()
    private val classNames: MutableMap<Pair<String, String>, String> = mutableMapOf()

    fun uploadDeviceActivity(userId: String, day: Date = Date(), onComplete: (Boolean) -> Unit) {
        val db = FirestoreManager()
        val usageEvents = usageStatsManager.queryEvents(
            getMidnight(day), getMidnight(addDays(day, 1))
        )

        usageEvents.iterator().asSequence().filter { it.eventType in EVENT_TYPES.keys }
            .filterNot { it.packageName in ignoredPackages }
            .map { it: UsageEvents.Event ->
                UsageEvent(type = EVENT_TYPES[it.eventType]!!,
                    timestamp = Timestamp(Date(it.timeStamp)),
                    appId = it.packageName,
                    app = appNames.getOrPut(it.packageName) { pm.getAppName(it.packageName) },
                    action = classNames.getOrPut(it.packageName to it.className) {
                        pm.getActivityName(
                            it.packageName, it.className
                        )
                    })
            }.chunked(FirestoreManager.MAX_BATCH_SIZE).forEach {
                db.uploadUsageEvents(userId, it) { success ->
                    if (!success) {
                        Log.w(TAG, "Failed to upload usage events")
                        onComplete(false)
                        return@uploadUsageEvents
                    } else {
                        Log.d(TAG, "Uploaded ${it.size} usage events")
                    }
                }
            }

        onComplete(true)
    }
}