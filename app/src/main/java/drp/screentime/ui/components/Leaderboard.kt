package drp.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.functions.functions
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.util.formatDuration
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun LeaderboardView(competitionId: String, userId: String) {
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
            LeaderboardEntry(place = index + 1, user = user, myUserId = userId)
          }
        }
      }
}

@Composable
fun LeaderboardEntry(place: Int, user: User, myUserId: String) {
  val startTime = user.currentAppSince?.seconds ?: 0

  // number of seconds the user has been using the app
  var time by remember { mutableLongStateOf(0L) }

  LaunchedEffect(startTime) {
    while (true) { // isActive is true as long as the coroutine is active
      val diffInMillis = Timestamp.now().seconds - startTime
      time = diffInMillis
      delay(1.seconds)
    }
  }

  val fillColor: Color =
      when {
        myUserId == user.id -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
      }

  Card(
    colors = CardDefaults.cardColors(containerColor = fillColor),
      shape = RoundedCornerShape(16.dp)) {
    Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.padding(20.dp, 0.dp), verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            place.toString(),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.titleMedium
          )
          Text(user.name, style = MaterialTheme.typography.titleMedium)
          Spacer(
            Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          Text(text = formatDuration(user.score), style = MaterialTheme.typography.titleMedium)
        }
    if (myUserId != user.id && user.currentApp != null) {
      Row(
        modifier = Modifier.padding(20.dp, 8.dp, 20.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
//          Spacer(Modifier.width(36.dp))
        Text(
          "Using ",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          "${user.currentApp}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          " for ",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          formatDuration(time),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(
          Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        Button(onClick = {
          Firebase.functions.getHttpsCallable("poke")
            .call(mapOf("toUserID" to user.id, "fromUserID" to myUserId))
        }, shape = RoundedCornerShape(16.dp)) {
          Text("Poke")
        }
      }
    } else {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
