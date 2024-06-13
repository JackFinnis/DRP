package drp.screentime.firestore

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.firestore.Document.Companion.FIELD_LAST_UPDATED
import drp.screentime.notification.MessagingService
import drp.screentime.storage.StorageManager
import drp.screentime.usage.AppLiveUsageInfo
import drp.screentime.util.generateInviteCode
import drp.screentime.util.generateUserName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirestoreManager {
  val db = FirebaseFirestore.getInstance()

  fun getId(): String {
    return db.collection(Collections.USERS).document().id
  }

  fun createUser(userId: String, onComplete: (Boolean) -> Unit) {
    setDocument(
        Collections.USERS,
        userId,
        User(
            name = generateUserName(),
            fcmToken = MessagingService.fcmToken,
        ),
        onComplete)
  }

  fun setUserScore(userId: String, newScore: Long, onComplete: (Boolean) -> Unit) {
    updateDocument(
      Collections.USERS,
      userId,
      mapOf(
        ("${User::previousScores.name}.${
          SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Timestamp.now().toDate()
          )
        }") to newScore
      ),
      onComplete
    )
    updateDocument(Collections.USERS, userId, mapOf(User::score.name to newScore), onComplete)
  }

  fun setUserCurrentApp(
      userId: String,
      currentApp: AppLiveUsageInfo?,
      onComplete: (Boolean) -> Unit,
  ) {
    updateDocument(
        Collections.USERS,
        userId,
        mapOf(
            User::currentApp.name to currentApp?.appName,
            User::currentAppSince.name to currentApp?.usedSince?.let { Timestamp(Date(it)) },
            User::currentPackage.name to currentApp?.packageName,
            User::currentAppClass.name to currentApp?.className,
            User::currentAppActivity.name to currentApp?.activityName),
        onComplete)
  }

  private fun setUserCompetition(
      userId: String,
      competitionId: String,
      context: Context,
      onComplete: (Boolean) -> Unit
  ) {
    updateDocument(
        Collections.USERS, userId, mapOf(User::competitionId.name to competitionId), onComplete)
    getDocument<Competition>(Collections.COMPETITIONS, competitionId) { competition ->
      StorageManager(context)
          .preferences
          .edit()
          .putStringSet(StorageManager.Key.APPS.name, competition?.apps?.map(App::name)?.toSet())
          .apply()
    }
  }

  fun addLeaderboardListener(
      competitionId: String,
      onResult: (List<User>) -> Unit
  ): ListenerRegistration {
    return db.collection(Collections.USERS)
        .whereEqualTo(User::competitionId.name, competitionId)
        .addSnapshotListener { snapshot, _ ->
          if (snapshot == null) {
            onResult(emptyList())
            return@addSnapshotListener
          }
          val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
          onResult(users)
        }
  }

  private fun getCompetitionIdFromInviteCode(inviteCode: String, onResult: (String?) -> Unit) {
    db.collection(Collections.COMPETITIONS)
        .whereEqualTo(Competition::inviteCode.name, inviteCode)
        .get()
        .addOnSuccessListener { snapshot -> onResult(snapshot.documents.firstOrNull()?.id) }
        .addOnFailureListener { onResult(null) }
  }

  fun joinCompetition(
      userId: String,
      inviteCode: String,
      context: Context,
      onComplete: (Boolean) -> Unit
  ) {
    getCompetitionIdFromInviteCode(inviteCode) { competitionId ->
      if (competitionId == null) {
        onComplete(false)
      } else {
        setUserCompetition(userId, competitionId, context, onComplete)
      }
    }
  }

  fun createAndJoinCompetition(
      userId: String,
      apps: List<App>,
      context: Context,
      onComplete: (Boolean) -> Unit
  ) {
    val competitionId = getId()
    val competition = Competition(generateInviteCode(), apps)
    setDocument(Collections.COMPETITIONS, competitionId, competition) { success ->
      if (!success) {
        onComplete(false)
      } else {
        setUserCompetition(userId, competitionId, context, onComplete)
      }
    }
  }

  fun updateDocument(
      collection: String,
      documentId: String,
      data: Map<String, Any?>,
      onComplete: (Boolean) -> Unit,
  ) {
    db.collection(collection)
        .document(documentId)
        .update(data + (FIELD_LAST_UPDATED to Timestamp.now()))
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
  }

  inline fun <reified T> addDocumentListener(
      collection: String,
      documentId: String,
      noinline onResult: (T?) -> Unit
  ): ListenerRegistration {
    return db.collection(collection).document(documentId).addSnapshotListener { snapshot, _ ->
      if (snapshot == null || !snapshot.exists()) {
        onResult(null)
        return@addSnapshotListener
      }
      onResult(snapshot.toObject(T::class.java))
    }
  }

  fun <T : Any> setDocument(
      collection: String,
      documentId: String,
      data: T,
      onComplete: (Boolean) -> Unit
  ) {
    db.collection(collection)
        .document(documentId)
        .set(data)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
  }

  inline fun <reified T> getDocument(
      collection: String,
      documentId: String,
      noinline onResult: (T?) -> Unit
  ) {
    db.collection(collection)
        .document(documentId)
        .get()
        .addOnSuccessListener { document -> onResult(document.toObject(T::class.java)) }
        .addOnFailureListener { onResult(null) }
  }
}
