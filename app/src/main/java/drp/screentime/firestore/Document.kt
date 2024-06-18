package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

abstract class Document(
  @DocumentId val id: String = "",
  @Suppress("unused") val updated: Timestamp = Timestamp.now(),
) {
  companion object {
    const val FIELD_LAST_UPDATED = "updated"
  }
}
