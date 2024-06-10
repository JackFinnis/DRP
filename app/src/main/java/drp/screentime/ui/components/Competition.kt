package drp.screentime.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.firestore.FirestoreManager

@Composable
fun CompetitionView(
    modifier: Modifier = Modifier,
    userId: String,
    firestoreManager: FirestoreManager = FirestoreManager(),
    showBottomSheet: MutableState<Boolean>
) {
  var competitionId by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(true) }
  var inviteCode by remember { mutableStateOf("") }
  var showInviteDialog by remember { mutableStateOf(false) }
  var showJoinCompetitionDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  DisposableEffect(userId) {
    val listener =
        firestoreManager.addUserListener(userId) { user ->
          competitionId = user?.competitionId
          loading = false
        }

    onDispose { listener.remove() }
  }

  DisposableEffect(competitionId) {
    val listener: ListenerRegistration? =
        competitionId?.let {
          firestoreManager.addCompetitionListener(it) { competition ->
            inviteCode = competition?.inviteCode ?: ""
          }
        }

    onDispose { listener?.remove() }
  }

  var joinCompetitionCode by remember { mutableStateOf("") }
  if (showJoinCompetitionDialog) {
    AlertDialog(
        onDismissRequest = { showJoinCompetitionDialog = false },
        confirmButton = {
          TextButton(
              onClick = {
                loading = true
                showJoinCompetitionDialog = false
                firestoreManager.joinCompetition(userId, joinCompetitionCode, onComplete = {})
              }) {
                Text("Join")
              }
        },
        dismissButton = {
          TextButton(onClick = { showJoinCompetitionDialog = false }) { Text("Cancel") }
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

  if (!loading) {
    Column(modifier = modifier) {
      competitionId?.let { Box(modifier = Modifier.weight(1f)) { LeaderboardView(it, userId) } }

      Column(modifier = Modifier.padding(16.dp)) {
        Row {
          if (competitionId == null) {
            LargeButton(
                modifier = Modifier.weight(1f),
                onClick = {
                  // Add a new competition
                  loading = true
                  firestoreManager.addAndJoinCompetition(userId, onComplete = {})
                },
                icon = Icons.Default.AddChart,
                text = "Start a competition",
                tonal = false)
            Spacer(Modifier.width(16.dp))
            LargeButton(
                modifier = Modifier.weight(1f),
                onClick = { showJoinCompetitionDialog = true },
                icon = Icons.Default.Start,
                text = "Join competition",
                tonal = false)
          } else {
            LargeButton(
                modifier = Modifier.weight(1f),
                onClick = {
                  val sendIntent: Intent =
                      Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, inviteCode)

                        // Picked up by chosen application if it supports a subject
                        // field
                        putExtra(Intent.EXTRA_SUBJECT, "Join my screen time competition!")

                        // This is the title of the dialog that the user will see
                        putExtra(Intent.EXTRA_TITLE, "Competition invite code")

                        type = "text/plain"
                      }

                  val shareIntent = Intent.createChooser(sendIntent, null)
                  context.startActivity(shareIntent)
                },
                icon = Icons.Default.PersonAdd,
                text = "Invite friends",
                tonal = false)
            Spacer(Modifier.width(16.dp))
            LargeButton(
                modifier = Modifier.weight(1f),
                onClick = { showBottomSheet.value = true },
                icon = Icons.Default.Edit,
                text = "Edit name",
                tonal = true)
          }
        }
      }
    }
  } else {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      when {
        loading -> CircularProgressIndicator()
      }
    }
  }

  if (showInviteDialog) {
    AlertDialog(
        onDismissRequest = { showInviteDialog = false },
        title = { Text("Invite code") },
        text = { Text(inviteCode) },
        confirmButton = { TextButton(onClick = { showInviteDialog = false }) { Text("OK") } })
  }
}
