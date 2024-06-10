package drp.screentime.firestore

data class Competition(
  val inviteCode: String = "",
) : Document()