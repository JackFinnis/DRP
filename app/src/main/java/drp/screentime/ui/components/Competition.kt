package drp.screentime.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.firestore.Collections
import drp.screentime.firestore.Competition
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionView(user: User, competitionId: String) {
  var showEditNameAlert by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf(user.name) }
  var competition by remember { mutableStateOf<Competition?>(null) }
  val context = LocalContext.current

  DisposableEffect(competitionId) {
    val listener: ListenerRegistration =
        FirestoreManager.addDocumentListener<Competition>(
          Collections.COMPETITIONS, competitionId
        ) { newCompetition ->
              competition = newCompetition
            }
    onDispose { listener.remove() }
  }

  if (competition == null) {
    LoadingView()
  } else {
    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Leaderboard") }) },
    ) { contentPadding ->
      Column(modifier = Modifier.padding(contentPadding)) {
        Box(modifier = Modifier.weight(1f)) { LeaderboardView(competitionId, user.id) }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              LargeButton(
                  modifier = Modifier.weight(1f),
                  onClick = {
                    val sendIntent: Intent =
                        Intent().apply {
                          action = Intent.ACTION_SEND
                          type = "text/plain"
                          putExtra(Intent.EXTRA_TEXT, competition!!.inviteCode)
                          putExtra(Intent.EXTRA_SUBJECT, "Join my screen time competition!")
                          putExtra(Intent.EXTRA_TITLE, "Competition invite code")
                        }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                  },
                  icon = Icons.Default.PersonAdd,
                  text = "Invite friends",
                  tonal = false)
              LargeButton(
                  modifier = Modifier.weight(1f),
                  onClick = { showEditNameAlert = true },
                  icon = Icons.Default.Edit,
                  text = "Edit name",
                  tonal = true)
            }
      }
    }
  }

  if (showEditNameAlert) {
    AlertDialog(
        onDismissRequest = { showEditNameAlert = false },
        confirmButton = {
          TextButton(
              onClick = {
                showEditNameAlert = false
                FirestoreManager.updateDocument(
                  Collections.USERS,
                  user.id,
                  mapOf(User::name.name to name)
                ) {}
              }) {
                Text("Save")
              }
        },
        dismissButton = { TextButton(onClick = { showEditNameAlert = false }) { Text("Cancel") } },
        title = { Text("Enter your name") },
        text = {
          TextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())
        })
  }
}
