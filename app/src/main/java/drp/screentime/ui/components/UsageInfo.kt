package drp.screentime.ui.components

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.getAppName

@Composable
fun UsageStatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageStats = UsageStatsProcessor(context.packageManager, usageStatsManager)
    val usageStatsList = usageStats.getApplicationUsageStats()

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
            usageStatsList.toList().sortedByDescending { it.second }.forEach { usageStat ->
                UsageStatItem(usageStat.first, usageStat.second)
            }
        }
    }
}

@Composable
fun UsageStatItem(packageName: String, usage: Long) {
    val pm = LocalContext.current.packageManager
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(end = 16.dp)) { AppIcon(packageName) }
            Text(text = pm.getAppName(packageName).trim())
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight())
            Text(text = "${usage}s")
        }
    }
}

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

