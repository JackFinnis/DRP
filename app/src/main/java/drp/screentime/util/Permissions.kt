package drp.screentime.util

import android.content.Context
import android.os.PowerManager

fun Context.areBatteryOptimisationsDisabled(): Boolean {
  val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
  return powerManager.isIgnoringBatteryOptimizations(packageName)
}
