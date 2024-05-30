package drp.screentime.firestore

data class User(
    val id: String = "",
    val name: String = "",
    val enrolledIn: List<String> = listOf()
)

data class Competition(
    val id: String = "",
    val name: String = "",
    val leaderboard: Map<String, Int> = mapOf() // Map of userId to score
)