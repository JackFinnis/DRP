package drp.screentime.util

fun generateFirstName(): String {
    val adjectives = listOf(
        "Funky",
        "Silly",
        "Clever",
        "Witty",
        "Happy",
        "Sad",
        "Angry",
        "Crazy",
        "Sneaky",
        "Sleepy"
    )
    return adjectives.random()
}

fun generateLastName(): String {
    val animals = listOf(
        "Octopus",
        "Elephant",
        "Kangaroo",
        "Penguin",
        "Panda",
        "Lion",
        "Tiger",
        "Bear",
        "Wolf",
        "Fox"
    )
    return animals.random()
}