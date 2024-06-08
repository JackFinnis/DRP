package drp.screentime.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

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
