package drp.screentime.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import drp.screentime.firestore.App

@Composable
fun NewCompetitionView() {
  val selectedApps = remember { mutableStateListOf<App>() }

  Scaffold { contentPadding ->
    LazyColumn(contentPadding = contentPadding) {
      items(App.entries.toTypedArray()) { app ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
              checked = selectedApps.contains(app),
              onCheckedChange = { selected ->
                if (selected) selectedApps.add(app) else selectedApps.remove(app)
              })
          Text(app.name)
        }
      }
    }
  }
}
