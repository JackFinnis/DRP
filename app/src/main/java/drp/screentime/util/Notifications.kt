package drp.screentime.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

fun Context.areAppNotificationsEnabled() =
    NotificationManagerCompat.from(this).areNotificationsEnabled()