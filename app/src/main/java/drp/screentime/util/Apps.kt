package drp.screentime.util

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getAppName(packageName: String): String =
    this.getApplicationInfo(packageName, 0).loadLabel(this).toString()

fun PackageManager.getActivityName(packageName: String, className: String): String {
  return try {
    val appRes = getResourcesForApplication(packageName)
    val activityInfo = getActivityInfo(ComponentName(packageName, className), 0)

    val name =
        if (activityInfo.labelRes != 0) {
          try {
            appRes.getString(activityInfo.labelRes)
          } catch (ignored: Exception) {
            className.substringAfterLast('.').replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(appRes.configuration.locales[0]) else it.toString()
            }
          }
        } else {
          className.substringAfterLast('.').replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(appRes.configuration.locales[0]) else it.toString()
          }
        }

    name
  } catch (e: PackageManager.NameNotFoundException) {
    className
  }
}

fun PackageManager.isSystemApp(packageName: String): Boolean {
  val sysSig = getAppSignatureHash("android")
  val appSig = getAppSignatureHash(packageName)
  return sysSig == appSig
}

fun PackageManager.getHomeScreenLaunchers(): List<String> {
  val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
  val homeApps = queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
  return homeApps.map { it.activityInfo.packageName }
}

fun PackageManager.getAppSignatureHash(packageName: String): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val sig = this.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
    if (sig.hasMultipleSigners()) sig.apkContentsSigners.contentDeepHashCode()
    else sig.signingCertificateHistory.contentDeepHashCode()
  } else {
    @Suppress("DEPRECATION")
    this.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures.contentDeepHashCode()
  }
}
