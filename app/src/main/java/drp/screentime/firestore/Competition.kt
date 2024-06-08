package drp.screentime.firestore

import com.google.firebase.firestore.DocumentId

data class Competition(
    @DocumentId val id: String = "",
    val inviteCode: String = ""
) {
    companion object {
        const val COLLECTION_NAME = "competitions"
        const val FIELD_INVITE_CODE = "inviteCode"
    }
}