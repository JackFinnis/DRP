package drp.screentime.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
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
    fun getUsageStats(day: Date = Date()): List<UsageStats> {
        val startTime = getMidnight(day)
        val endTime = getMidnight(addDays(day, 1))

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ).filter { it.totalTimeInForeground > 0 }.filterNot { pm.isSystemApp(it.packageName) }
            .filterNot { hiddenPackages.contains(it.packageName) }
    }

    fun getUsageStatsSorted(day: Date = Date()): List<UsageStats> {
        return getUsageStats(day).sortedByDescending { it.totalTimeInForeground }
    }
}