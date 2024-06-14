package drp.screentime.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.firestore.App
import drp.screentime.firestore.Collections
import drp.screentime.firestore.Competition
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionView(user: User, competitionId: String) {
  val showEditNameDialog = remember { mutableStateOf(false) }
  var showLeaveCompetitionAlert by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf(user.name) }
  var competition by remember { mutableStateOf<Competition?>(null) }
  var shownDay by remember { mutableStateOf(Date()) }

  val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
  val isToday = fmt.format(shownDay).equals(fmt.format(Date()))

  val context = LocalContext.current

  DisposableEffect(competitionId) {
    val listener: ListenerRegistration =
        FirestoreManager.addDocumentListener<Competition>(
            Collections.COMPETITIONS, competitionId) { newCompetition ->
              competition = newCompetition
            }
    onDispose { listener.remove() }
  }

  if (competition == null) {
    LoadingView()
  } else {
    Scaffold(
        topBar = {
          LargeTopAppBar(
              title = { Text("Leaderboard") },
              actions = {
                IconButton(onClick = { showLeaveCompetitionAlert = true }) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                      contentDescription = "Leave competition")
                }
              },
          )
        },
        floatingActionButton = {
          ExtendedFloatingActionButton(
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
          ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Invite friends",
                modifier = Modifier.padding(end = 12.dp))
            Text("Invite friends")
          }
        },
    ) { contentPadding ->
      Column(modifier = Modifier.padding(contentPadding)) {
        if (competition!!.apps.isNotEmpty()) {
          Text("Time using ${competition!!.apps.map(App::displayName).joinToString(", ")}", modifier = Modifier.padding(horizontal = 16.dp))
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(0.dp, 16.dp, 0.dp, 8.dp)) {
              Button(
                  onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.time = shownDay
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    shownDay = calendar.time
                  },
                  shape = CircleShape,
                  modifier = Modifier.size(36.dp),
                  contentPadding = PaddingValues(0.dp), // avoid the little icon
              ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous Day",
                )
              }
              Text(
                  if (isToday) "Today"
                  else SimpleDateFormat("EEEE d MMMM yyyy", Locale.US).format(shownDay),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.width(256.dp),
                  textAlign = TextAlign.Center)

              Button(
                  enabled = !isToday,
                  onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.time = shownDay
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    shownDay = calendar.time
                  },
                  shape = CircleShape,
                  modifier = Modifier.size(36.dp),
                  contentPadding = PaddingValues(0.dp), // avoid the little icon
              ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Next Day",
                )
              }
            }
        LeaderboardView(competition!!, user.id, showEditNameDialog, shownDay, isToday)
      }
    }
  }

  if (showEditNameDialog.value) {
    AlertDialog(
        onDismissRequest = { showEditNameDialog.value = false },
        confirmButton = {
          TextButton(
              onClick = {
                name = name.trim()
                showEditNameDialog.value = false
                FirestoreManager.updateDocument(
                    Collections.USERS, user.id, mapOf(User::name.name to name)) {}
              }) {
                Text("Save")
              }
        },
        dismissButton = {
          TextButton(onClick = { showEditNameDialog.value = false }) { Text("Cancel") }
        },
        title = { Text("Enter your name") },
        text = {
          TextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())
        })
  }

  if (showLeaveCompetitionAlert) {
    AlertDialog(
        title = { Text("Leave competition?") },
        text = {
          Text(
              "All your progress will be lost and you won't be able to rejoin unless invited back. Are you sure you want to leave?")
        },
        onDismissRequest = { showLeaveCompetitionAlert = false },
        confirmButton = {
          TextButton(
              onClick = {
                competition = null
                FirestoreManager.updateDocument(
                    Collections.USERS, user.id, mapOf(User::competitionId.name to null)) {}
                showLeaveCompetitionAlert = false
              }) {
                Text("Leave", color = MaterialTheme.colorScheme.error)
              }
        },
        dismissButton = {
          TextButton(onClick = { showLeaveCompetitionAlert = false }) { Text("Cancel") }
        },
    )
  }
}
