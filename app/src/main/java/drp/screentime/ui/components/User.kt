package drp.screentime.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.firestore.db
import drp.screentime.notification.PokeNotificationService
import drp.screentime.util.generateUserName

@Composable
fun UserView(userId: String) {
  var user by remember { mutableStateOf<User?>(null) }

  fun createUser() {
    db.collection(User.COLLECTION_NAME).document(userId)
      .set(User(name = generateUserName(), fcmToken = PokeNotificationService.fcmToken))
  }

  DisposableEffect(userId) {
    val listener =
        FirestoreManager.addDocumentListener<User>(User.COLLECTION_NAME, userId) { newUser ->
          if (newUser == null) {
            createUser()
          } else {
            user = newUser
          }
        }
    onDispose { listener.remove() }
  }

  if (user == null) {
    LoadingView()
  } else if (user!!.competitionId != null) {
    CompetitionView(user = user!!, competitionId = user!!.competitionId!!)
  } else {
    NoCompetitionView(userId = userId)
  }
}
