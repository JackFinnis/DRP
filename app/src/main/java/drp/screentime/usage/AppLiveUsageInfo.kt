package drp.screentime.usage

data class AppLiveUsageInfo(
    val packageName: String,
    val appName: String,
    val className: String,
    val activityName: String,
    val usedSince: Long,
)
