package drp.screentime.firestore

import com.google.firebase.firestore.FirebaseFirestore

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

                user.enrolledIn.forEach { competitionId ->
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

    fun createCompetition(competitionName: String, onComplete: (String?) -> Unit) =
        addDocument(Competition.COLLECTION_NAME, Competition(name = competitionName), onComplete)

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
