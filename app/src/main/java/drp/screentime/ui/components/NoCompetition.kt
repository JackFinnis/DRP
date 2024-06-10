package drp.screentime.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import drp.screentime.firestore.FirestoreManager

@Composable
fun NoCompetitionView(userId: String) {
  var joinCompetitionCode by remember { mutableStateOf("") }
  var showJoinCompetitionAlert by remember { mutableStateOf(false) }

  Scaffold { contentPadding ->
    Row(modifier = Modifier.padding(contentPadding)) {
      LargeButton(
          modifier = Modifier.weight(1f),
          onClick = { FirestoreManager.addAndJoinCompetition(userId, onComplete = {}) },
          icon = Icons.Default.AddChart,
          text = "Start a competition",
          tonal = false)
      Spacer(Modifier.width(16.dp))
      LargeButton(
          modifier = Modifier.weight(1f),
          onClick = { showJoinCompetitionAlert = true },
          icon = Icons.Default.Start,
          text = "Join competition",
          tonal = false)
    }

    if (showJoinCompetitionAlert) {
      AlertDialog(
          onDismissRequest = { showJoinCompetitionAlert = false },
          confirmButton = {
            TextButton(
                onClick = {
                  showJoinCompetitionAlert = false
                  FirestoreManager.joinCompetition(userId, joinCompetitionCode, onComplete = {})
                }) {
                  Text("Join")
                }
          },
          dismissButton = {
            TextButton(onClick = { showJoinCompetitionAlert = false }) { Text("Cancel") }
          },
          title = { Text("Enter invite code") },
          text = {
            TextField(
                value = joinCompetitionCode,
                onValueChange = { joinCompetitionCode = it },
                label = { Text("Invite code") },
                modifier = Modifier.fillMaxWidth())
          })
    }
  }
}
