package drp.screentime.usage

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Process
import androidx.compose.ui.util.fastMaxBy
import drp.screentime.util.addDays
import drp.screentime.util.getActivityName
import drp.screentime.util.getAppName
import drp.screentime.util.getHomeScreenLaunchers
import drp.screentime.util.getMidnight
import drp.screentime.util.isSystemApp
import java.util.Date

class UsageStatsProcessor(context: Context) {

  private val pm: PackageManager = context.packageManager
  private val usageStatsManager: UsageStatsManager =
      context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

  private val hiddenPackages = pm.getHomeScreenLaunchers()

  /** Get the total usage for the given day in seconds. */
  fun getTotalUsage(day: Date = Date()): Long {
    return getUsageStats(day)
        .filterNot { it.totalUsageMillis <= 0 || isMundane(it.packageName) }
        .sumOf { it.totalUsageMillis } / 1000
  }

  /** Get the currently open app, and the time it was opened. */
  fun getLastAppOpen(): AppLiveUsageInfo? {
    val usageEvents = getUsageEvents().toList()

    val (openEvents, closeEvents) =
        usageEvents
            .filter {
              !isMundane(it.packageName) &&
                  it.eventType in (APP_OPEN_EVENT_TYPES + APP_CLOSE_EVENT_TYPES)
            }
            .partition { it.eventType in APP_OPEN_EVENT_TYPES }

    val currentActivities = openEvents
      .filter { openEvent ->
        // Ensure it has no later corresponding close event
        closeEvents.none { closeEvent ->
          closeEvent.packageName == openEvent.packageName &&
              closeEvent.timeStamp > openEvent.timeStamp
        }
      }

    val lastUsedActivity = currentActivities
      .fastMaxBy { it.timeStamp } ?: return null

    return AppLiveUsageInfo(
      lastUsedActivity.packageName,
      pm.getAppName(lastUsedActivity.packageName),
      lastUsedActivity.className,
      lastUsedActivity.className?.let { pm.getActivityName(lastUsedActivity.packageName, it) },
      lastUsedActivity.timeStamp,
    )
  }

  /**
   * Get the usage stats for the given day.
   *
   * @param day The day to get the usage stats for. Defaults to today.
   * @return A list of usage stats for the given day.
   */
  private fun getUsageStats(day: Date = Date()): List<UsageStats> {
    val startTime = getMidnight(day)
    val endTime = getMidnight(addDays(day, 1))

    return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
  }

  /** Get the usage events for the last [duration] minutes. */
  private fun getUsageEvents(duration: Int = 5): Sequence<UsageEvents.Event> {
    return usageStatsManager
        .queryEvents(System.currentTimeMillis() - duration * 60 * 1000, System.currentTimeMillis())
        .iterator()
        .asSequence()
  }

  private fun isMundane(packageName: String) =
      pm.isSystemApp(packageName) || packageName in hiddenPackages

  companion object {
    fun hasUsageStatsAccess(context: Context): Boolean {
      val appOps: AppOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
      @Suppress("DEPRECATION")
      val mode: Int =
          appOps.checkOpNoThrow(
              AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)

      return if (mode == AppOpsManager.MODE_DEFAULT) {
        context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
            PackageManager.PERMISSION_GRANTED
      } else mode == AppOpsManager.MODE_ALLOWED
    }
  }
}

/**
 * Returns [UsageStats.getTotalTimeVisible] or [UsageStats.getTotalTimeInForeground] as appropriate,
 * depending on the device's Android version.
 */
private val UsageStats.totalUsageMillis: Long
  get() =
      when {
        VERSION.SDK_INT >= VERSION_CODES.Q -> totalTimeVisible
        else -> totalTimeInForeground
      }

/** Extension method to convert [UsageEvents] to an [Iterator] of [UsageEvents.Event]. */
private operator fun UsageEvents.iterator(): Iterator<UsageEvents.Event> =
    object : Iterator<UsageEvents.Event> {
      override fun hasNext(): Boolean = this@iterator.hasNextEvent()

      override fun next(): UsageEvents.Event {
        if (!hasNext()) throw NoSuchElementException()
        return UsageEvents.Event().apply { this@iterator.getNextEvent(this) }
      }
    }

/** Event types that indicate the app was opened. */
private val APP_OPEN_EVENT_TYPES =
    setOf(
        1, // Moved to foreground
        4, // Rollover from previous tracking interval
        7, // User interaction
    )

/** Event types that indicate the app was closed. */
private val APP_CLOSE_EVENT_TYPES =
    setOf(
        2, // Moved to background
        3, // End of tracking period
    )
