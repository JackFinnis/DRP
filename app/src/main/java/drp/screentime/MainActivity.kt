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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import drp.screentime.ui.theme.ScreenTimeTheme
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.getAppName

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
    val usageStats = UsageStatsProcessor(context.packageManager, usageStatsManager)
    val usageStatsList by remember { mutableStateOf(usageStats.getUsageStatsSorted()) }

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
    val packageManager = LocalContext.current.packageManager
    val icon = packageManager.getApplicationIcon(packageName)
    val appName = packageManager.getAppName(packageName)
    Image(
        painter = rememberAsyncImagePainter(icon.toBitmap()),
        contentDescription = "Icon for $appName",
        modifier = Modifier.size(36.dp)
    )
}

@Composable
fun UsageStatItem(usageStat: UsageStats) {
    val usage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) usageStat.totalTimeVisible / 1000
        else usageStat.totalTimeInForeground / 1000
    val pm = LocalContext.current.packageManager
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                AppIcon(usageStat.packageName)
            }
            Text(text = pm.getAppName(usageStat.packageName).trim())
            Spacer(Modifier.weight(1f).fillMaxHeight())
            Text(text = "${usage}s")
        }
    }
}


fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}