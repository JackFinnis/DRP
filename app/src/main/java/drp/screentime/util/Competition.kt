package drp.screentime.util

fun generateInviteCode(): String {
  val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  return (1..5).map { chars.random() }.joinToString("")
}
