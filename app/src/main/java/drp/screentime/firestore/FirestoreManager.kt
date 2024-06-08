package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import drp.screentime.util.generateInviteCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreManager {
  private val db = FirebaseFirestore.getInstance()

  fun getUser(userId: String, onResult: (User?) -> Unit) =
    getDocument(User.COLLECTION_NAME, userId, onResult)

  fun addUser(name: String, onComplete: (String?) -> Unit) =
    addDocument(User.COLLECTION_NAME, User(name = name), onComplete)

  private fun addCompetition(onComplete: (String?) -> Unit) =
    addDocument(
      Competition.COLLECTION_NAME, Competition(inviteCode = generateInviteCode()), onComplete)

  fun setUserName(userId: String, name: String, onComplete: (Boolean) -> Unit) {
    updateDocument(User.COLLECTION_NAME, userId, mapOf(User.FIELD_NAME to name), onComplete)
  }

  fun setUserScore(userId: String, newScore: Long, onComplete: (Boolean) -> Unit) {
    updateDocument(User.COLLECTION_NAME, userId, mapOf(User.FIELD_SCORE to newScore), onComplete)
  }

  fun setUserCurrentApp(
    userId: String,
    appName: String?,
    since: Date?,
    onComplete: (Boolean) -> Unit
  ) {
    db.collection(User.COLLECTION_NAME)
      .document(userId)
      .update(
        User.FIELD_CURRENT_APP,
        appName,
        User.FIELD_CURRENT_APP_SINCE,
        since?.let { Timestamp(it) })
      .addOnSuccessListener { onComplete(true) }
      .addOnFailureListener { onComplete(false) }
  }

  private fun setUserCompetition(
    userId: String,
    competitionId: String,
    onComplete: (Boolean) -> Unit
  ) {
    updateDocument(
      User.COLLECTION_NAME, userId, mapOf(User.FIELD_COMPETITION_ID to competitionId), onComplete)
  }

  fun addUserListener(userId: String, onResult: (User?) -> Unit): ListenerRegistration {
    return addDocumentListener(User.COLLECTION_NAME, userId, onResult)
  }

  fun addCompetitionListener(competitionId: String, onResult: (Competition?) -> Unit): ListenerRegistration {
    return addDocumentListener(Competition.COLLECTION_NAME, competitionId, onResult)
  }

  fun addLeaderboardListener(
    competitionId: String,
    onResult: (List<User>) -> Unit
  ): ListenerRegistration {
    return db.collection(User.COLLECTION_NAME)
      .whereEqualTo(User.FIELD_COMPETITION_ID, competitionId)
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
    db.collection(Competition.COLLECTION_NAME)
      .whereEqualTo(Competition.FIELD_INVITE_CODE, inviteCode)
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

  private fun updateDocument(
    collection: String,
    documentId: String,
    data: Map<String, Any>,
    onComplete: (Boolean) -> Unit
  ) {
    db.collection(collection)
      .document(documentId)
      .update(data)
      .addOnSuccessListener { onComplete(true) }
      .addOnFailureListener { onComplete(false) }
  }

  private inline fun <reified T> getDocument(
    collection: String,
    documentId: String,
    noinline onResult: (T?) -> Unit
  ) {
    db.collection(collection)
      .document(documentId)
      .get()
      .addOnSuccessListener { onResult(it.toObject(T::class.java)) }
      .addOnFailureListener { onResult(null) }
  }

  private inline fun <reified T> addDocumentListener(
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

  private fun <T : Any> addDocument(collection: String, data: T, onComplete: (String?) -> Unit) {
    val ref = db.collection(collection).document()
    ref.set(data)
      .addOnSuccessListener { onComplete(ref.id) }
      .addOnFailureListener { onComplete(null) }
  }

  fun uploadUsageData(userId: String, usageData: Map<String, Long>, onComplete: (Boolean) -> Unit) {
    val usageCollectionRef =
      db.collection(User.COLLECTION_NAME).document(userId).collection(UsageData.COLLECTION_NAME)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    usageCollectionRef
      .document(today)
      .get()
      .addOnSuccessListener { documentSnapshot ->
        val newData = UsageData(today, usageData, Timestamp(Date()))
        if (documentSnapshot.exists()) {
          documentSnapshot.reference
            .update(
              UsageData.FIELD_BREAKDOWN,
              usageData,
              UsageData.FIELD_LAST_SYNC,
              newData.lastSync)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
        } else {
          usageCollectionRef
            .document(today)
            .set(newData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
        }
      }
      .addOnFailureListener { onComplete(false) }
  }
}
