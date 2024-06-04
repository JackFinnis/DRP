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

    fun getCompetitionData(competitionId: String, onResult: (Competition?) -> Unit) =
        fetchDocument(Competition.COLLECTION_NAME, competitionId, onResult)

    fun getEnrolledCompetitions(userId: String, onResult: (List<Competition>) -> Unit) {
        getUserData(userId) { user ->
            user?.let {
                val competitions = mutableListOf<Competition>()
                val pendingCompCount = user.enrolledIn.size

                if (pendingCompCount == 0) onResult(emptyList())
                else user.enrolledIn.forEach { competitionId ->
                    getCompetitionData(competitionId) { competition ->
                        competition?.let { competitions.add(it) }
                        if (competitions.size == pendingCompCount) onResult(competitions)
                    }
                }
            } ?: onResult(emptyList())
        }
    }

    fun addUser(name: String, onComplete: (String?) -> Unit) =
        addDocument(User.COLLECTION_NAME, User(name = name), onComplete)

    fun setUserName(userId: String, name: String, onComplete: (Boolean) -> Unit) {
        db.collection(User.COLLECTION_NAME).document(userId).update(User.FIELD_NAME, name)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

     fun enrollInCompetition(userId: String, competitionId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(User.COLLECTION_NAME).document(userId)
        val compRef = db.collection(Competition.COLLECTION_NAME).document(competitionId)

        db.runTransaction { transaction ->
            val user = transaction.get(userRef).toObject(User::class.java)
            val comp = transaction.get(compRef).toObject(Competition::class.java)

            if (user != null && comp != null) {
                transaction.update(userRef, User.FIELD_ENROLLED_IN, user.enrolledIn + competitionId)
                transaction.update(
                    compRef, Competition.FIELD_LEADERBOARD, comp.leaderboard + (userId to 0)
                )
            }
        }.addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
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

    fun removeFromCompetition(userId: String, competitionId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(User.COLLECTION_NAME).document(userId)
        val compRef = db.collection(Competition.COLLECTION_NAME).document(competitionId)

        db.runTransaction { transaction ->
            val user = transaction.get(userRef).toObject(User::class.java)
            val comp = transaction.get(compRef).toObject(Competition::class.java)
            if (user != null && comp != null) {
                transaction.update(userRef, User.FIELD_ENROLLED_IN, user.enrolledIn - competitionId)
                if (comp.leaderboard.size != 1) {
                    transaction.update(
                        compRef, Competition.FIELD_LEADERBOARD, comp.leaderboard - userId
                    )
                } else {
                    transaction.delete(compRef)
                }
            }
        }
    }

    private fun createCompetition(competitionName: String, onComplete: (String?) -> Unit) =
        addDocument(Competition.COLLECTION_NAME, Competition(name = competitionName, inviteCode = generateInviteCode()), onComplete)

    fun createCompetitionAndAddUser(userId: String, competitionName: String, onComplete: (Boolean) -> Unit) {
        createCompetition(competitionName) { competitionId ->
            competitionId?.let {
                enrollInCompetition(userId, it, onComplete)
            } ?: onComplete(false)
        }
    }

    fun updateScore(
        competitionId: String, userId: String, newScore: Int, onComplete: (Boolean) -> Unit
    ) {
        val compRef = db.collection(Competition.COLLECTION_NAME).document(competitionId)

        db.runTransaction { transaction ->
            val comp = transaction.get(compRef).toObject(Competition::class.java)
            comp?.let {
                transaction.update(
                    compRef, Competition.FIELD_LEADERBOARD, it.leaderboard + (userId to newScore)
                )
            }
        }.addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
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
