package drp.screentime.firestore

import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.CountDownLatch

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    // Fetch user data
    fun getUserData(userId: String, onComplete: (User?) -> Unit) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
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
        db.collection("competitions").document(competitionId).get()
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
}