package drp.screentime.util

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getAppName(packageName: String): String =
    this.getApplicationInfo(packageName, 0).loadLabel(this).toString()

fun PackageManager.isSystemApp(packageName: String): Boolean {
    val sysSig = getAppSignatureHash("android")
    val appSig = getAppSignatureHash(packageName)
    return sysSig == appSig
}

fun PackageManager.getHomeScreenLauncher(): String {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = this.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName ?: ""
}

fun PackageManager.getAppSignatureHash(packageName: String): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val sig = this.getPackageInfo(
            packageName, PackageManager.GET_SIGNING_CERTIFICATES
        ).signingInfo
        if (sig.hasMultipleSigners()) sig.apkContentsSigners.contentDeepHashCode()
        else sig.signingCertificateHistory.contentDeepHashCode()
    } else {
        @Suppress("DEPRECATION") this.getPackageInfo(
            packageName, PackageManager.GET_SIGNATURES
        ).signatures.contentDeepHashCode()
    }
}

