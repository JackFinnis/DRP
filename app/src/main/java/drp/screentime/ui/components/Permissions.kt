package drp.screentime.ui.components

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * A button that checks if a permission is enabled and opens the settings if it is not.
 *
 * @param intent The intent to launch where the permission can be enabled.
 * @param isEnabled A function that checks if the permission is enabled.
 */
@Composable
fun PermissionCheckButton(intent: Intent, isEnabled: (Context) -> Boolean) {
  val context = LocalContext.current
  var hasPermission by remember { mutableStateOf(isEnabled(context)) }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasPermission = isEnabled(context)
      }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(key1 = lifecycleOwner) {
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

/**
 * A row that displays a permission check button.
 *
 * @param intent The intent to launch where the permission can be enabled.
 * @param isEnabled A function that checks if the permission is enabled.
 * @param description A short tagline describing the permission.
 * @param icon A vector icon displayed alongside the description.
 */
@Composable
fun PermissionCheckRow(
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

        PermissionCheckButton(intent, isEnabled)
      }
}
