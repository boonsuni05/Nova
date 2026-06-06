package com.nova.assistant.commands

import com.nova.assistant.model.NovaCommand
import java.util.UUID

object SpeechParser {

    // Number capture is non-greedy (.+?) so the optional percent suffix is consumed
    // separately rather than being swallowed into group 1.
    private val VOLUME_RE     = Regex("""^(?:set )?volume(?: to)? (.+?)(?:\s+percent|\s*%)?$""")
    private val BRIGHTNESS_RE = Regex("""^(?:set )?brightness(?: to)? (.+?)(?:\s+percent|\s*%)?$""")
    private val FLASHLIGHT_RE = Regex("""^flashlight (on|off)$""")
    private val OPEN_RE       = Regex("""^open (.+)$""")

    fun parse(text: String): NovaCommand? {
        val t = text.trim().lowercase()

        VOLUME_RE.matchEntire(t)?.let {
            val level = parseLevel(it.groupValues[1]) ?: return@let
            return NovaCommand(id(), "set_volume",
                mapOf("level" to level.coerceIn(0, 100)))
        }

        BRIGHTNESS_RE.matchEntire(t)?.let {
            val level = parseLevel(it.groupValues[1]) ?: return@let
            return NovaCommand(id(), "set_brightness",
                mapOf("level" to level.coerceIn(0, 100)))
        }

        FLASHLIGHT_RE.matchEntire(t)?.let {
            return NovaCommand(id(), "set_flashlight",
                mapOf("on" to (it.groupValues[1] == "on")))
        }

        OPEN_RE.matchEntire(t)?.let {
            return NovaCommand(id(), "open_app", mapOf("query" to it.groupValues[1]))
        }

        return when (t) {
            "get volume"     -> NovaCommand(id(), "get_volume",     emptyMap())
            "get battery"    -> NovaCommand(id(), "get_battery",    emptyMap())
            "get brightness" -> NovaCommand(id(), "get_brightness", emptyMap())
            "status"         -> NovaCommand(id(), "get_status",     emptyMap())
            "play",
            "resume"         -> NovaCommand(id(), "media_play",     emptyMap())
            "pause"          -> NovaCommand(id(), "media_pause",    emptyMap())
            "next",
            "skip",
            "skip song",
            "next song"      -> NovaCommand(id(), "media_next",     emptyMap())
            "previous",
            "previous song"  -> NovaCommand(id(), "media_previous", emptyMap())
            else             -> null
        }
    }

    // Tries digit string first ("50"), then English number words ("fifty").
    private fun parseLevel(s: String): Int? = s.trim().toIntOrNull() ?: wordToInt(s.trim())

    private fun wordToInt(s: String): Int? {
        val w = s.replace("-", " ").trim() // normalise "twenty-one" → "twenty one"

        if (w == "one hundred" || w == "a hundred" || w == "hundred") return 100

        val ones = mapOf(
            "zero" to 0,  "oh" to 0,
            "one" to 1,   "two" to 2,       "three" to 3,     "four" to 4,
            "five" to 5,  "six" to 6,        "seven" to 7,     "eight" to 8,
            "nine" to 9,  "ten" to 10,       "eleven" to 11,   "twelve" to 12,
            "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16,
            "seventeen" to 17, "eighteen" to 18, "nineteen" to 19
        )
        val tens = mapOf(
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty"  to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
        )

        ones[w]?.let { return it }
        tens[w]?.let { return it }

        // Compound: "twenty one" … "ninety nine"
        val parts = w.split(" ")
        if (parts.size == 2) {
            val hi = tens[parts[0]] ?: return null
            val lo = ones[parts[1]] ?: return null
            if (lo == 0) return null // "twenty zero" is not a valid number word
            return hi + lo
        }

        return null
    }

    private fun id() = UUID.randomUUID().toString()
}
