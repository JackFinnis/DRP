package drp.screentime.firestore

import androidx.annotation.Keep
import java.util.Date

@Keep
data class Competition(
  val inviteCode: String = "",
  val apps: List<App> = emptyList(),
  val prize: String = "",
  val endDate: Date? = null,
) : Document()