package drp.screentime.firestore

import com.google.firebase.Timestamp

data class User(
    val name: String = "",
    val score: Long = 0, val fcmToken: String? = null,
    val competitionId: String? = null,
    val currentApp: String? = null,
    val currentAppSince: Timestamp? = null,
) : Document()