package drp.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import drp.screentime.firestore.FirestoreManager
import drp.screentime.util.constants

@Composable
fun NoCompetitionView(userId: String) {
  var joinCompetitionCode by remember { mutableStateOf("") }
  var showJoinCompetitionAlert by remember { mutableStateOf(false) }

  Scaffold { contentPadding ->
    Column(modifier = Modifier.padding(contentPadding).padding(16.dp)) {
      Box(modifier = Modifier.weight(1f)) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
          Text("Welcome to ${constants.appName}", fontSize = 24.sp)
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              "Compete with your friends to decrease your screen time!",
              textAlign = TextAlign.Center)
        }
      }
      Row {
        LargeButton(
            modifier = Modifier.weight(1f),
            onClick = { FirestoreManager.addAndJoinCompetition(userId, onComplete = {}) },
            icon = Icons.Default.AddChart,
            text = "Start competition",
            tonal = false)
        Spacer(Modifier.width(16.dp))
        LargeButton(
            modifier = Modifier.weight(1f),
            onClick = { showJoinCompetitionAlert = true },
            icon = Icons.Default.Start,
            text = "Join competition",
            tonal = false)
      }
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
                onValueChange = { joinCompetitionCode = it.uppercase() },
                label = { Text("Invite code") },
                modifier = Modifier.fillMaxWidth())
          })
    }
  }
}
