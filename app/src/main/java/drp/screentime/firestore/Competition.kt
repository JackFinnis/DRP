package drp.screentime.firestore

import androidx.annotation.Keep

@Keep
data class Competition(
  val inviteCode: String = "",
) : Document()