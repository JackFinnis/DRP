package drp.screentime.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.os.Build
import drp.screentime.util.addDays
import drp.screentime.util.getHomeScreenLauncher
import drp.screentime.util.getMidnight
import drp.screentime.util.isSystemApp
import java.util.Date

class UsageStatsProcessor(
    private val pm: PackageManager, private val usageStatsManager: UsageStatsManager
) {

    private val hiddenPackages = setOf("drp.screentime", pm.getHomeScreenLauncher())

    /** Get the usage stats for the given day.
     * @param day The day to get the usage stats for. Defaults to today.
     * @return A list of usage stats for the given day.
     */
    private fun getUsageStats(day: Date = Date()): List<UsageStats> {
        val startTime = getMidnight(day)
        val endTime = getMidnight(addDays(day, 1))

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ).filter { it.totalTimeInForeground > 0 }.filterNot { pm.isSystemApp(it.packageName) }
            .filterNot { hiddenPackages.contains(it.packageName) }
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
}