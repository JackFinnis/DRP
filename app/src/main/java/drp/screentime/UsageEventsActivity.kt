package drp.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import drp.screentime.ui.components.UsageStatsScreen
import drp.screentime.ui.theme.ScreenTimeTheme
import drp.screentime.util.getAppName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageEventsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UsageStatsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun UsageEvent(usageEvent: UsageEvents.Event) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "${formatter.format(Date(usageEvent.timeStamp))}: ${
                LocalContext.current.packageManager.getAppName(usageEvent.packageName)
            } ${getEventType(usageEvent.eventType)}"
        )
    }
}

val hiddenEventTypes = listOf(11, 19, 20)

fun getEventType(eventType: Int): String {
    return when (eventType) {
        UsageEvents.Event.ACTIVITY_PAUSED -> "moved to background"
        UsageEvents.Event.ACTIVITY_RESUMED -> "moved to foreground"
        UsageEvents.Event.ACTIVITY_STOPPED -> "stopped (no longer visible in UI)"
        UsageEvents.Event.CONFIGURATION_CHANGE -> "device configuration changed"
        UsageEvents.Event.DEVICE_SHUTDOWN -> "device shut down"
        UsageEvents.Event.DEVICE_STARTUP -> "device started up"
        UsageEvents.Event.FOREGROUND_SERVICE_START -> "foreground service start"
        UsageEvents.Event.FOREGROUND_SERVICE_STOP -> "foreground service stop"
        UsageEvents.Event.KEYGUARD_HIDDEN -> "keyguard hidden"
        UsageEvents.Event.KEYGUARD_SHOWN -> "keyguard shown"
        UsageEvents.Event.SHORTCUT_INVOCATION -> "shortcut action performed"
        UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "standby bucket"
        UsageEvents.Event.SCREEN_INTERACTIVE -> "screen turned on"
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "screen turned off"
        UsageEvents.Event.USER_INTERACTION -> "interacted with"
        UsageEvents.Event.NONE -> ""
        10 -> "notification viewed"
        12 -> "posted an interruptive notification"
        13 -> "slice pinned by launcher or assistant"
        14 -> "slice pinned by app"
        6 -> "system interaction"
        else -> "unknown $eventType"
    }
}

fun getUsageEvents(usageStatsManager: UsageStatsManager): List<UsageEvents.Event> {
    val currentTime = System.currentTimeMillis()
    val hours = 1
    val startTime = currentTime - 1000 * 60 * 60 * hours
    val usageEvents = usageStatsManager.queryEvents(startTime, currentTime)
    val usageEventList = mutableListOf<UsageEvents.Event>()
    while (usageEvents.hasNextEvent()) {
        val event = UsageEvents.Event()
        usageEvents.getNextEvent(event)
        if (event.eventType !in hiddenEventTypes) usageEventList.add(event)
    }
    return usageEventList
}

