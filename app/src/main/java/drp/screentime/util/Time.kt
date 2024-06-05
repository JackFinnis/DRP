package drp.screentime.util

import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun getMidnight(date: Date): Long {
  val calendar = Calendar.getInstance()
  calendar.time = date
  calendar.set(Calendar.HOUR_OF_DAY, 0)
  calendar.set(Calendar.MINUTE, 0)
  calendar.set(Calendar.SECOND, 0)
  calendar.set(Calendar.MILLISECOND, 0)
  return calendar.timeInMillis
}

fun addDays(date: Date, days: Int): Date {
  val cal = Calendar.getInstance()
  cal.time = date
  cal.add(Calendar.DATE, days)
  return cal.time
}

fun formatDuration(durationInSeconds: Long): String {
  val duration = abs(durationInSeconds)

  val hours = duration / 3600
  val minutes = (duration % 3600) / 60
  val seconds = duration % 60

  val locale = Locale.getDefault()

  return when {
    hours > 0 ->
        if (minutes > 0) {
          String.format(locale, "%dh %dm", hours, minutes)
        } else {
          String.format(locale, "%dh", hours)
        }
    minutes >= 10 -> String.format(locale, "%dm", minutes)
    minutes > 0 -> String.format(locale, "%dm %ds", minutes, seconds)
    else -> String.format(locale, "%ds", seconds)
  }
}
