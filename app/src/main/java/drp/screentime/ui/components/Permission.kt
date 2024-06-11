package drp.screentime.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.areAppNotificationsEnabled
import drp.screentime.util.areBatteryOptimisationsDisabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsView() {
  Scaffold(topBar = { LargeTopAppBar(title = { Text("Enable permissions") }) }) { contentPadding ->
    Column(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
      val context = LocalContext.current
      val packageName = context.packageName

      Text(
          text =
              "Authorising the following permissions is necessary for the app to function properly.",
          modifier = Modifier.padding(16.dp))

      PermissionRow(
          intent =
              Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
              },
          isEnabled = UsageStatsProcessor.Companion::hasUsageStatsAccess,
          description = "Usage data access",
          icon = Icons.Default.StackedBarChart)

      PermissionRow(
          intent =
              Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", packageName, null)
              },
          isEnabled = Context::areBatteryOptimisationsDisabled,
          description = "Run in the background",
          icon = Icons.Default.BatteryStd)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PermissionRow(
            intent =
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                  putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            isEnabled = Context::areAppNotificationsEnabled,
            description = "Push notifications",
            icon = Icons.Default.NotificationsActive)
      }
    }
  }
}

/**
 * A row that displays a permission check button.
 *
 * @param intent The intent to launch where the permission can be enabled.
 * @param isEnabled A function that checks if the permission is enabled.
 * @param description A short tagline describing the permission.
 * @param icon A vector icon displayed alongside the description.
 */
@Composable
fun PermissionRow(
    intent: Intent,
    isEnabled: (Context) -> Boolean,
    description: String,
    icon: ImageVector
) {
  Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp))

        Text(text = description, modifier = Modifier.padding(start = 8.dp))

        Spacer(modifier = Modifier.weight(1f))

        PermissionToggle(intent, isEnabled)
      }
}

/**
 * A button that checks if a permission is enabled and opens the settings if it is not.
 *
 * @param intent The intent to launch where the permission can be enabled.
 * @param isEnabled A function that checks if the permission is enabled.
 */
@Composable
fun PermissionToggle(intent: Intent, isEnabled: (Context) -> Boolean) {
  val context = LocalContext.current
  var hasPermission by remember { mutableStateOf(isEnabled(context)) }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasPermission = isEnabled(context)
      }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasPermission = isEnabled(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val hapticFeedback = LocalHapticFeedback.current
  Button(
      onClick = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        launcher.launch(
            intent.apply {
              addFlags(
                  Intent.FLAG_ACTIVITY_NEW_TASK or
                      Intent.FLAG_ACTIVITY_CLEAR_TASK or
                      Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            })
      },
      colors =
          ButtonDefaults.buttonColors(
              containerColor =
                  if (hasPermission) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.error)) {
        if (hasPermission) {
          Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Enabled",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        } else {
          Text("Enable", color = MaterialTheme.colorScheme.onError)
        }
      }
}
