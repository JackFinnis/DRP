package drp.screentime.usage

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import drp.screentime.util.addDays
import drp.screentime.util.getHomeScreenLaunchers
import drp.screentime.util.getMidnight
import drp.screentime.util.isSystemApp
import java.util.Date

class UsageStatsProcessor(
    private val pm: PackageManager, private val usageStatsManager: UsageStatsManager
) {

    private val hiddenPackages = setOf("drp.screentime") + pm.getHomeScreenLaunchers()

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

    companion object {
        fun hasUsageStatsAccess(context: Context): Boolean {
            val appOps: AppOpsManager =
                context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION") val mode: Int = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )

            return if (mode == AppOpsManager.MODE_DEFAULT) {
                context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            } else mode == AppOpsManager.MODE_ALLOWED
        }
    }
}