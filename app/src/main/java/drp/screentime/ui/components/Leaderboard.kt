package drp.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import drp.screentime.firestore.App
import drp.screentime.firestore.Competition
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderboardView(
    competition: Competition,
    userId: String,
    showEditNameAlert: MutableState<Boolean>,
    shownDay: Date,
    isToday: Boolean
) {
  var users by remember { mutableStateOf<List<Pair<Long, User>>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var dataIsToday by remember { mutableStateOf(isToday) }

  DisposableEffect(competition.id, shownDay) {
    val listener =
        FirestoreManager.addLeaderboardListener(competition.id) { newUsers ->
          users =
              newUsers
                  .mapNotNull {
                    val score =
                        if (isToday) it.score
                        else
                            (it.previousScores[
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(shownDay)])

                    if (score != null) Pair(score, it) else null
                  }
                  .sortedBy { pair -> pair.first }
          loading = false
          dataIsToday = isToday
        }
    onDispose { listener.remove() }
  }

  LazyColumn(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (competition.apps.isNotEmpty()) {
          item {
            Text(
                "Time using ${competition.apps.map(App::displayName).joinToString(", ")}",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth())
          }
        }
        itemsIndexed(users, key = { _, user -> user.second.id }) { index, user ->
          Box(modifier = Modifier.animateItem()) {
            LeaderboardRow(
                place = index + 1,
                user = user.second,
                score = user.first,
                isToday = dataIsToday,
                myUserId = userId,
                showEditNameAlert = showEditNameAlert,
                competition = competition)
          }
        }
      }
}
