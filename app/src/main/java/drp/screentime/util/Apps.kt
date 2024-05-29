package drp.screentime.util

import android.content.Intent
import android.content.pm.PackageManager

fun getAppName(packageManager: PackageManager, packageName: String): String {
    return try {
        packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager)
            .toString()
    } catch (e: Exception) {
        packageName
    }
}

fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
    } catch (e: Exception) {
        false
    }
}

fun getHomeScreenLauncher(packageManager: PackageManager): String {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName ?: ""
}

