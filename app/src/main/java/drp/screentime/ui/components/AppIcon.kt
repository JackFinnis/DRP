package drp.screentime.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import drp.screentime.util.getAppName

@Composable
fun AppIcon(packageName: String) {
  val packageManager = LocalContext.current.packageManager
  val icon = packageManager.getApplicationIcon(packageName)
  val appName = packageManager.getAppName(packageName)
  Image(
      painter = rememberAsyncImagePainter(icon.toBitmap()),
      contentDescription = "Icon for $appName",
      modifier = Modifier.size(36.dp))
}
