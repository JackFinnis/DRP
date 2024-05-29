package drp.screentime

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import drp.screentime.ui.theme.ScreenTimeTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
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
fun UsageStatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageStatsList by remember { mutableStateOf(getUsageStats(usageStatsManager)) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (usageStatsList.isEmpty()) {
            Text(text = "No usage stats available. Please enable usage access in system settings.")
            Button(onClick = { openUsageAccessSettings(context) }) {
                Text(text = "Open Usage Access Settings")
            }
        } else {
            usageStatsList.forEach { usageStat ->
                UsageStatItem(usageStat)
            }
        }
    }
}

@Composable
fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon = context.packageManager.getApplicationIcon(packageName)
    val appName = getAppName(context, packageName)
    Image(
        painter = rememberAsyncImagePainter(icon.toBitmap()),
        contentDescription = "Icon for $appName",
        modifier = Modifier
            .size(48.dp)
            .padding(8.dp)
    )
}

@Composable
fun UsageStatItem(usageStat: UsageStats) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(usageStat.packageName)
            Text(
                text = getAppName(LocalContext.current, usageStat.packageName),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(text = "Last time used: ${formatter.format(Date(usageStat.lastTimeUsed))}")
        Text(text = "Total time in foreground: ${usageStat.totalTimeInForeground / 1000} seconds")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Text(text = "Last time visible: ${formatter.format(Date(usageStat.lastTimeVisible))}")
            Text(text = "Total time visible: ${usageStat.totalTimeVisible / 1000} seconds")
            if (usageStat.lastTimeForegroundServiceUsed > 0) {
                Text(text = "Last time service used: ${formatter.format(Date(usageStat.lastTimeForegroundServiceUsed))}")
                Text(text = "Total time service used: ${usageStat.totalTimeForegroundServiceUsed / 1000} seconds")
            }
        }
    }
}


fun getAppName(context: Context, packageName: String): String {
    return try {
        context.packageManager.getApplicationInfo(packageName, 0).loadLabel(context.packageManager)
            .toString()
    } catch (e: Exception) {
        packageName
    }
}

fun getUsageStats(usageStatsManager: UsageStatsManager): List<UsageStats> {
    val currentTime = System.currentTimeMillis()

    // Get the start of the current day (midnight)
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = calendar.timeInMillis

    return usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, currentTime
    ).sortedByDescending { it.totalTimeInForeground }
        .dropLastWhile { it.totalTimeInForeground <= 0 }
}

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}