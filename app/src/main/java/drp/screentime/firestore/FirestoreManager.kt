package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    fun getUserData(userId: String, onResult: (User?) -> Unit) =
        fetchDocument(User.COLLECTION_NAME, userId, onResult)

    fun addUser(name: String, onComplete: (String?) -> Unit) =
        addDocument(User.COLLECTION_NAME, User(name = name), onComplete)

    fun setUserName(userId: String, name: String, onComplete: (Boolean) -> Unit) {
        db.collection(User.COLLECTION_NAME).document(userId).update(User.FIELD_NAME, name)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun enrollInCompetition(userId: String, competitionId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(User.COLLECTION_NAME).document(userId)
        userRef.update(User.FIELD_COMPETITION_ID, competitionId)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    private fun getCompetitionIdFromInviteCode(inviteCode: String, onResult: (String?) -> Unit) {
        db.collection(Competition.COLLECTION_NAME)
            .whereEqualTo(Competition.INVITE_CODE, inviteCode).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.size == 1) {
                    onResult(querySnapshot.documents.firstOrNull()?.id)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun enrollWithInviteCode(userId: String, inviteCode: String, onComplete: (Boolean) -> Unit) {
        getCompetitionIdFromInviteCode(inviteCode) { competitionId ->
            if (competitionId != null) {
                enrollInCompetition(userId, competitionId, onComplete)
            } else {
                onComplete(false)
            }
        }
    }

    private fun createCompetition(onComplete: (String?) -> Unit) =
        addDocument(
            Competition.COLLECTION_NAME,
            Competition(inviteCode = generateInviteCode()),
            onComplete
        )

    fun createCompetitionAndAddUser(userId: String, onComplete: (Boolean) -> Unit) {
        createCompetition { competitionId ->
            competitionId?.let {
                enrollInCompetition(userId, it, onComplete)
            } ?: onComplete(false)
        }
    }

    fun updateScore(userId: String, newScore: Long, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(User.COLLECTION_NAME).document(userId)
        userRef.update(User.FIELD_SCORE, newScore)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun listenForUserDataChanges(userId: String, onResult: (User?) -> Unit) {
        db.collection(User.COLLECTION_NAME).document(userId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) {
                onResult(null)
                return@addSnapshotListener
            }
            val user = snapshot.toObject(User::class.java)
            onResult(user)
        }
    }

    fun listenForCompetitionDataChanges(competitionId: String, onResult: (Competition?) -> Unit) {
        db.collection(Competition.COLLECTION_NAME).document(competitionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    onResult(null)
                    return@addSnapshotListener
                }
                val competition = snapshot.toObject(Competition::class.java)
                onResult(competition)
            }
    }

    fun listenForCompetitionUpdates(competitionId: String, onResult: (List<User>) -> Unit) {
        db.collection(User.COLLECTION_NAME).whereEqualTo(User.FIELD_COMPETITION_ID, competitionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                onResult(users)
            }
    }

    fun uploadUsageData(
        userId: String,
        usageData: Map<String, Long>,
        onComplete: (Boolean) -> Unit
    ) {
        val usageCollectionRef = db.collection(User.COLLECTION_NAME).document(userId)
            .collection(UsageData.COLLECTION_NAME)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        usageCollectionRef.document(today).get()
            .addOnSuccessListener { documentSnapshot ->
                val newData = UsageData(today, usageData, Timestamp(Date()))
                if (documentSnapshot.exists()) {
                    documentSnapshot.reference.update(
                        UsageData.FIELD_BREAKDOWN, usageData,
                        UsageData.FIELD_LAST_SYNC, newData.lastSync
                    ).addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    usageCollectionRef.document(today).set(newData)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }.addOnFailureListener { onComplete(false) }
    }

    fun updateCurrentApp(
        userId: String,
        appName: String?,
        since: Date?,
        onComplete: (Boolean) -> Unit
    ) {
        val userRef = db.collection(User.COLLECTION_NAME).document(userId)
        userRef.update(
            User.FIELD_CURRENT_APP,
            appName,
            User.FIELD_CURRENT_APP_SINCE,
            since?.let { Timestamp(it) }
        ).addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    private inline fun <reified T> fetchDocument(
        collection: String, documentId: String, noinline onResult: (T?) -> Unit
    ) {
        db.collection(collection).document(documentId).get()
            .addOnSuccessListener { onResult(it.toObject(T::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    private fun <T : Any> addDocument(collection: String, data: T, onComplete: (String?) -> Unit) {
        val newDocRef = db.collection(collection).document()
        newDocRef.set(data).addOnSuccessListener { onComplete(newDocRef.id) }
            .addOnFailureListener { onComplete(null) }
    }
}
