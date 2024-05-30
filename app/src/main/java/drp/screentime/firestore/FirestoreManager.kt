package drp.screentime.firestore

import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.CountDownLatch

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_COMPETITIONS = "competitions"
    }

    // Fetch user data
    fun getUserData(userId: String, onComplete: (User?) -> Unit) {
        db.collection(COLLECTION_USERS).document(userId).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    onComplete(user)
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener { exception ->
                onComplete(null)
            }
    }

    // Fetch group data
    fun getCompetitionData(competitionId: String, onComplete: (Competition?) -> Unit) {
        db.collection(COLLECTION_COMPETITIONS).document(competitionId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val group = document.toObject(Competition::class.java)
                    onComplete(group)
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener { exception ->
                onComplete(null)
            }
    }

    // Fetch all competitions user is enrolled in
    fun getEnrolledCompetitions(userId: String, onComplete: (List<Competition>) -> Unit) {
        getUserData(userId) { user ->
            if (user != null) {
                val comps = mutableListOf<Competition>()
                val competitionIds = user.enrolledIn
                val latch = CountDownLatch(competitionIds.size)

                for (competitionId in competitionIds) {
                    getCompetitionData(competitionId) { comp ->
                        if (comp != null) {
                            comps.add(comp)
                        }
                        latch.countDown()
                    }
                }

                Thread {
                    latch.await()
                    onComplete(comps)
                }.start()
            } else {
                onComplete(emptyList())
            }
        }
    }

    fun addUser(firstName: String, lastName: String, onComplete: (String?) -> Unit) {
        val newUserRef = db.collection(COLLECTION_USERS).document()
        val newUser = User(newUserRef.id, firstName, lastName, emptyList())

        newUserRef.set(newUser)
            .addOnSuccessListener {
                onComplete(newUserRef.id)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun enrollInCompetition(userId: String, competitionId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(COLLECTION_USERS).document(userId)
        db.runTransaction { transaction ->
            val user = transaction.get(userRef).toObject(User::class.java)
            if (user != null) {
                val updatedEnrolledIn = user.enrolledIn.toMutableList()
                updatedEnrolledIn.add(competitionId)
                transaction.update(userRef, "enrolledIn", updatedEnrolledIn)
            }
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun createCompetition(competitionName: String, onComplete: (Boolean) -> Unit) {
        val newCompRef = db.collection(COLLECTION_USERS).document()
        val newComp = Competition(newCompRef.id, competitionName, emptyMap())

        newCompRef.set(newComp)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // Update score in a competition
    fun updateScore(competitionId: String, userId: String, newScore: Int, onComplete: (Boolean) -> Unit) {
        val compRef = db.collection(COLLECTION_COMPETITIONS).document(competitionId)
        db.runTransaction { transaction ->
            val comp = transaction.get(compRef).toObject(Competition::class.java)
            if (comp != null) {
                val updatedScores = comp.leaderboard.toMutableMap()
                updatedScores[userId] = newScore
                transaction.update(compRef, "leaderboard", updatedScores)
            }
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    // Add user to a competition
    fun addUserToCompetition(competitionId: String, userId: String, onComplete: (Boolean) -> Unit) {
        val compRef = db.collection(COLLECTION_COMPETITIONS).document(competitionId)
        db.runTransaction { transaction ->
            val comp = transaction.get(compRef).toObject(Competition::class.java)
            if (comp != null) {
                transaction.update(compRef, "leaderboard", comp.leaderboard.toMutableMap().put(userId, 0))
            }
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }
}