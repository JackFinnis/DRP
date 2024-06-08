package drp.screentime.util

fun generateUserName(): String {
  val adjectives =
      listOf(
          "Funky",
          "Silly",
          "Clever",
          "Witty",
          "Happy",
          "Sad",
          "Angry",
          "Crazy",
          "Sneaky",
          "Sleepy",
          "Clumsy")
  val animals =
      listOf(
          "Octopus",
          "Elephant",
          "Kangaroo",
          "Penguin",
          "Panda",
          "Lion",
          "Tiger",
          "Bear",
          "Wolf",
          "Fox")
  return "${adjectives.random()} ${animals.random()}"
}
