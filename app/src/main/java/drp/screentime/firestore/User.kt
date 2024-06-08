package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val score: Long = 0,
    val competitionId: String? = null,
    val currentApp: String? = null,
    val currentAppSince: Timestamp? = null
) {
    companion object {
        const val COLLECTION_NAME = "users"
        const val FIELD_NAME = "name"
        const val FIELD_SCORE = "score"
        const val FIELD_COMPETITION_ID = "competitionId"
        const val FIELD_CURRENT_APP = "currentApp"
        const val FIELD_CURRENT_APP_SINCE = "currentAppSince"
    }
}