package drp.screentime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import drp.screentime.firestore.App
import drp.screentime.firestore.FirestoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCompetitionView(userId: String, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val selectedApps = remember { mutableStateListOf<App>() }
  var prize by remember { mutableStateOf("") }
  var days by remember { mutableIntStateOf(0) }
  fun setSelected(app: App, selected: Boolean) {
    if (selected) selectedApps.add(app) else selectedApps.remove(app)
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("New Competition", fontWeight = FontWeight.Medium) },
            actions = { TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 17.sp) } })
      }) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(contentPadding).padding(16.dp).fillMaxHeight()) {
              Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Text("Select the apps you want to use less")
                App.entries.forEach { app ->
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedApps.contains(app),
                        onCheckedChange = { selected -> setSelected(app, selected) })
                    Text(app.displayName)
                  }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Enter the prize for the winner (optional)")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = prize,
                    onValueChange = { prize = it },
                    label = { Text("Prize") },
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select the length of the competition")
                intArrayOf(0, 3, 7, 14).forEach { day ->
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = days == day, onClick = { days = day })
                    Text(if (day == 0) "No length" else "$day days")
                  }
                }
              }
              Button(
                  enabled = selectedApps.isNotEmpty(),
                  onClick = {
                    FirestoreManager.createAndJoinCompetition(
                        userId,
                        selectedApps.toList(),
                        prize,
                        days,
                        context,
                        onComplete = { onDismiss() })
                  }) {
                    Text("Start Competition", fontSize = 17.sp)
                  }
            }
      }
}
