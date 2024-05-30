package drp.screentime.firestore

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val enrolledIn: List<String> = listOf()
)

data class Competition(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val leaderboard: Map<String, Int> = mapOf() // Map of userId to score
)