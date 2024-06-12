package drp.screentime.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.util.formatDuration
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun LeaderboardView(
    competitionId: String,
    userId: String,
    showEditNameAlert: MutableState<Boolean>
) {
  var users by remember { mutableStateOf<List<User>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }

  DisposableEffect(competitionId) {
    val listener =
        FirestoreManager.addLeaderboardListener(competitionId) { newUsers ->
          users = newUsers.sortedBy { it.score }
          loading = false
        }
    onDispose { listener.remove() }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
          Box(modifier = Modifier.animateItem()) {
            LeaderboardEntry(
                place = index + 1,
                user = user,
                myUserId = userId,
                showEditNameAlert = showEditNameAlert)
          }
        }
      }
}

@Composable
fun LeaderboardEntry(
    place: Int,
    user: User,
    myUserId: String,
    showEditNameAlert: MutableState<Boolean>
) {
  val startTime = user.currentAppSince?.seconds ?: 0

  // number of seconds the user has been using the app
  var time by remember { mutableLongStateOf(0L) }
  var pokeMessage by remember { mutableStateOf("") }
  var showPokeAlert by remember { mutableStateOf(false) }
  val pokable = myUserId != user.id && user.currentApp != null

  LaunchedEffect(startTime) {
    while (true) { // isActive is true as long as the coroutine is active
      val diffInMillis = Timestamp.now().seconds - startTime
      time = diffInMillis
      delay(1.seconds)
    }
  }

  val fillColor: Color = when (user.id) {
    myUserId -> colorScheme.primary
    else -> colorScheme.secondaryContainer
  }
  val textColor: Color = when (user.id) {
    myUserId -> colorScheme.onPrimary
    else -> colorScheme.onSecondaryContainer
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = fillColor),
    onClick = {
      if (user.id == myUserId) {
        showEditNameAlert.value = true
      }
    },
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(end = 12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = when (place) {
              1 -> Color(0xFFFFD700)
              2 -> Color(0xFF999999)
              3 -> Color(0xFFCD7232)
              else -> fillColor
            },
            modifier = Modifier
              .width(32.dp)
              .height(32.dp),
            shadowElevation = if (place <= 3) 2.dp else 0.dp,
          ) {
            Text(
              text = place.toString(),
              style = typography.titleMedium,
              color = when (place) {
                1 -> Color.Black
                2 -> Color.White
                3 -> Color.White
                else -> textColor
              },
              textAlign = TextAlign.Center,
              modifier = Modifier
                .padding(4.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            )
          }
        }
        Text(
          text = user.name,
          style = typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        )
        Text(
          text = formatDuration(user.score),
          style = typography.titleMedium,
          modifier = Modifier.defaultMinSize(48.dp, Dp.Unspecified),
          textAlign = TextAlign.Right
        )
      }
      if (pokable) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = buildAnnotatedString {
              append("Using ")
              withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                append(user.currentApp!!)
              }
              append(" for ")
              withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                append(formatDuration(time))
              }
            },
            style = typography.bodyMedium,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          )
          Button(onClick = { showPokeAlert = true }) { Text("Poke") }
        }
      }
    }
  }

  if (showPokeAlert) {
    AlertDialog(
      confirmButton = {
        TextButton(
          colors = ButtonDefaults.buttonColors(),
          enabled = pokable,
          onClick = {
            Firebase.functions.getHttpsCallable("poke").call(
              mapOf(
                "toUserID" to user.id,
                "fromUserID" to myUserId,
                "message" to pokeMessage,
              )
            ).addOnFailureListener {
              Log.e("Leaderboard", "Failed to poke user", it)
              showPokeAlert = false
            }
            // TODO: remove once logging finished=
            FirebaseFirestore.getInstance().collection("config")
              .document("logging").get()
              .addOnSuccessListener { data ->
                if (data.getBoolean("pokes")!!) {
                  val ref = FirebaseFirestore.getInstance().collection("pokeCalls").document()
                  ref.set(mapOf(
                    "fromID" to myUserId,
                    "toID" to user.id,
                    "timestamp" to Timestamp.now()
                  ))
                }
              }

            pokeMessage = ""
            showPokeAlert = false
          },
        ) {
          Text("Poke")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPokeAlert = false }) { Text("Cancel") }
      },
      onDismissRequest = { showPokeAlert = false },
      icon = {
        Icon(
          imageVector = Icons.Default.TouchApp,
          contentDescription = "Poke",
        )
      },
      title = { Text("Poke user?") },
      text = {
        Column {
          Text(buildAnnotatedString {
            append("Send ")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
              append(user.name)
            }
            append(" a reminder to take a break from ")
            if (user.currentApp == null) {
              append("their device")
            } else {
              withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                append(user.currentApp)
              }
            }
            append("?")
          })
          Spacer(Modifier.height(16.dp))
          TextField(value = pokeMessage,
            onValueChange = { pokeMessage = it },
            label = { Text("Add message (optional)") },
            modifier = Modifier.fillMaxWidth()
          )
          if (!pokable) {
            Spacer(Modifier.height(16.dp))
            Text(
              "${user.name} is no longer using an app and cannot be poked.",
              style = typography.bodySmall,
              color = colorScheme.error
            )
          }
        }
      },
    )
  }
}
