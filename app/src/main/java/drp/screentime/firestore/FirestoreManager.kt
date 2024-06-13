package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.firestore.Document.Companion.FIELD_LAST_UPDATED
import drp.screentime.usage.AppLiveUsageInfo
import drp.screentime.util.generateInviteCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val db = FirebaseFirestore.getInstance()

object FirestoreManager {
  private fun addCompetition(onComplete: (String?) -> Unit) = addDocument(
    Collections.COMPETITIONS, Competition(inviteCode = generateInviteCode()), onComplete
  )

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
      Collections.USERS, userId, mapOf(
        User::currentApp.name to currentApp?.appName,
        User::currentAppSince.name to currentApp?.usedSince?.let { Timestamp(Date(it)) },
        User::currentPackage.name to currentApp?.packageName,
        User::currentAppClass.name to currentApp?.className,
        User::currentAppActivity.name to currentApp?.activityName
      ), onComplete
    )
  }

  private fun setUserCompetition(
    userId: String,
    competitionId: String,
    onComplete: (Boolean) -> Unit
  ) {
    updateDocument(
      Collections.USERS, userId, mapOf(User::competitionId.name to competitionId), onComplete
    )
  }

  fun addLeaderboardListener(
    competitionId: String,
    onResult: (List<User>) -> Unit
  ): ListenerRegistration {
    return db.collection(Collections.USERS).whereEqualTo(User::competitionId.name, competitionId)
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
    db.collection(Collections.COMPETITIONS).whereEqualTo(Competition::inviteCode.name, inviteCode)
      .get()
      .addOnSuccessListener { snapshot -> onResult(snapshot.documents.firstOrNull()?.id) }
      .addOnFailureListener { onResult(null) }
  }

  fun joinCompetition(userId: String, inviteCode: String, onComplete: (Boolean) -> Unit) {
    getCompetitionIdFromInviteCode(inviteCode) { competitionId ->
      if (competitionId == null) {
        onComplete(false)
      } else {
        setUserCompetition(userId, competitionId, onComplete)
      }
    }
  }

  fun addAndJoinCompetition(userId: String, onComplete: (Boolean) -> Unit) {
    addCompetition { competitionId ->
      if (competitionId == null) {
        onComplete(false)
      } else {
        setUserCompetition(userId, competitionId, onComplete)
      }
    }
  }

  fun updateDocument(
    collection: String,
    documentId: String,
    data: Map<String, Any?>,
    onComplete: (Boolean) -> Unit,
  ) {
    db.collection(collection).document(documentId)
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

  fun <T : Any> addDocument(collection: String, data: T, onComplete: (String?) -> Unit) {
    val ref = db.collection(collection).document()
    ref.set(data)
      .addOnSuccessListener { onComplete(ref.id) }
      .addOnFailureListener { onComplete(null) }
  }
}