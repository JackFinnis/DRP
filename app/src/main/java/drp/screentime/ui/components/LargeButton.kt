package drp.screentime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun LargeButton(
    modifier: Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    tonal: Boolean
) {
  if (tonal)
      FilledTonalButton(
          shape = RoundedCornerShape(24.dp),
          contentPadding = PaddingValues(16.dp),
          modifier = modifier,
          onClick = onClick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(icon, contentDescription = text, modifier = Modifier.size(36.dp))
              Spacer(Modifier.height(8.dp))
              Text(text, style = MaterialTheme.typography.labelLarge)
            }
          }
  else
      Button(
          shape = RoundedCornerShape(24.dp),
          contentPadding = PaddingValues(16.dp),
          modifier = modifier,
          onClick = onClick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(icon, contentDescription = text, modifier = Modifier.size(36.dp))
              Spacer(Modifier.height(8.dp))
              Text(text, style = MaterialTheme.typography.labelLarge)
            }
          }
}
