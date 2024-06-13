package drp.screentime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import drp.screentime.firestore.App
import drp.screentime.firestore.FirestoreManager

@Composable
fun NewCompetitionView(userId: String, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val selectedApps = remember { mutableStateListOf<App>() }
  fun setSelected(app: App, selected: Boolean) {
    if (selected) selectedApps.add(app) else selectedApps.remove(app)
  }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text("Select the apps you want to use less", modifier = Modifier.padding(16.dp))
      App.entries.forEach { app ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
              checked = selectedApps.contains(app),
              onCheckedChange = { selected -> setSelected(app, selected) })
          Text(app.displayName)
        }
      }
    }
    Button(
        modifier = Modifier.padding(16.dp),
        enabled = selectedApps.isNotEmpty(),
        onClick = {
          FirestoreManager.createAndJoinCompetition(
              userId, selectedApps.toList(), context, onComplete = { onDismiss() })
        }) {
          Text("Start competition")
        }
  }
}
