package dev.gregross.challenges.json

class JsonParseException(
    message: String,
    val position: Position? = null,
) : Exception(
    if (position != null) "$message at $position" else message
)
