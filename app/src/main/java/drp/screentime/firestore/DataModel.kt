package drp.screentime.firestore

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val enrolledIn: List<String> = listOf()
) {
    companion object {
        const val COLLECTION_NAME = "users"
        const val FIELD_NAME = "name"
        const val FIELD_ENROLLED_IN = "enrolledIn"
    }
}

data class Competition(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val leaderboard: Map<String, Int> = mapOf()
) {
    companion object {
        const val COLLECTION_NAME = "competitions"
        const val FIELD_NAME = "name"
        const val FIELD_LEADERBOARD = "leaderboard"
    }
}