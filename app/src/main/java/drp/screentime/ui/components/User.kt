package drp.screentime.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import drp.screentime.firestore.Collections
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.usage.DataUploadWorker

@Composable
fun UserView(userId: String) {
  var user by remember { mutableStateOf<User?>(null) }
  val context = LocalContext.current

  DisposableEffect(userId) {
    val listener =
        FirestoreManager.addDocumentListener<User>(Collections.USERS, userId) { newUser ->
          if (newUser == null) {
            FirestoreManager.createUser(userId, onComplete = {})
          } else {
            user = newUser
          }
        }
    onDispose { listener.remove() }
  }

  if (user != null) {
    LaunchedEffect(Unit) { DataUploadWorker.uploadAsap(context) }

    if (user!!.competitionId != null) {
      CompetitionView(user = user!!, competitionId = user!!.competitionId!!)
    } else {
      NoCompetitionView(userId = userId)
    }
  } else {
    LoadingView()
  }
}
