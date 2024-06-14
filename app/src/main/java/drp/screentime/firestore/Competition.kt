package drp.screentime.firestore

import androidx.annotation.Keep

@Keep
data class Competition(
  val inviteCode: String = "",
  val apps: List<App> = emptyList(),
  val prize: String = "",
  val days: Int = 0,
) : Document()