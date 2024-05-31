package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

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

data class UsageData(
    @DocumentId val day: String = "",
    val breakdown: Map<String, Long> = emptyMap(),
    val lastSync: Timestamp = Timestamp(Date())
) {
    companion object {
        const val COLLECTION_NAME = "usage"
        const val FIELD_BREAKDOWN = "breakdown"
        const val FIELD_LAST_SYNC = "lastSync"
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