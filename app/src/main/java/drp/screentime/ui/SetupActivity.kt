package drp.screentime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.compose.AppTheme
import drp.screentime.App.Companion.areAllPermissionsGranted
import drp.screentime.ui.components.PermissionCheckRow
import drp.screentime.usage.AppUsageTrackingService
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.areAppNotificationsEnabled

class SetupActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(content = { PermissionsScreen(it) },
                    topBar = { LargeTopAppBar(title = { Text("Enable permissions") }) })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (areAllPermissionsGranted(this)) {
            // Nothing to do here, start MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}

@Composable
fun PermissionsScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        val context = LocalContext.current
        val packageName = context.packageName

        Text(
            text = "Authorising the following permissions is necessary for the app to function properly.",
            modifier = Modifier.padding(16.dp)
        )

        PermissionCheckRow(
            intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
            isEnabled = UsageStatsProcessor.Companion::hasUsageStatsAccess,
            description = "Usage data access",
            icon = Icons.Default.StackedBarChart
        )

        PermissionCheckRow(
            intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            isEnabled = AppUsageTrackingService.Companion::isEnabled,
            description = "Accessibility service",
            icon = Icons.Default.AccessibilityNew
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PermissionCheckRow(
                intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) },
                isEnabled = Context::areAppNotificationsEnabled,
                description = "Push notifications",
                icon = Icons.Default.NotificationsActive
            )
        }
    }
}
