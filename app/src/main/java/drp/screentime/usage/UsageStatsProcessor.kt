package drp.screentime.usage

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Process
import drp.screentime.util.addDays
import drp.screentime.util.getHomeScreenLaunchers
import drp.screentime.util.getMidnight
import drp.screentime.util.isSystemApp
import java.util.Date

class UsageStatsProcessor(context: Context) {

  private val pm: PackageManager = context.packageManager
  private val usageStatsManager: UsageStatsManager =
      context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

  private val hiddenPackages = setOf("drp.screentime") + pm.getHomeScreenLaunchers()

  /** Get the total usage for the given day in seconds. */
  fun getTotalUsage(day: Date = Date()): Long {
    return getUsageStats(day).sumOf { it.totalUsageMillis } / 1000
  }

  /** Get the currently open app, if there is one. */
  fun getCurrentlyOpenApp(): UsageStats? =
    getUsageStats().filter { it.isValid }.maxByOrNull { it.lastUsageTime }

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
      .filterNot { it.isMundane }
  }

  private val UsageStats.isMundane: Boolean
    get() = totalUsageMillis > 0 && !pm.isSystemApp(packageName) && packageName !in hiddenPackages
  private val UsageStats.isValid: Boolean
    get() {
      val currentTime = System.currentTimeMillis() / 1000
      return lastUsageTime in (currentTime - STALE_THRESHOLD)..currentTime
    }

  companion object {
    /** Threshold, in seconds, after which the usage statistic is considered stale. */
    private const val STALE_THRESHOLD = 3 * 60

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

/** Returns [UsageStats.getTotalTimeVisible] or [UsageStats.getTotalTimeInForeground] as appropriate, depending on the device's Android version. */
private val UsageStats.totalUsageMillis: Long
  get() = when {
    VERSION.SDK_INT >= VERSION_CODES.Q -> totalTimeVisible
    else -> totalTimeInForeground
  }

/** Returns [UsageStats.getLastTimeVisible] or [UsageStats.getLastTimeUsed] as appropriate, depending on the device's Android version. */
private val UsageStats.lastUsageTime: Long
  get() = when {
    VERSION.SDK_INT >= VERSION_CODES.Q -> lastTimeVisible
    else -> lastTimeUsed
  }
