package com.blankpages.trackify.domain

import java.net.URLDecoder

/** Menarik koordinat dari berbagai format tautan Google Maps. Murni JVM, tanpa Android. */
object MapUrlParser {
    data class Coordinates(val latitude: Double, val longitude: Double)

    private val placePattern = Regex("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)")
    private val atPattern = Regex("@(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)")
    private val queryKeys = listOf("q", "query", "destination", "ll", "center")

    fun parse(raw: String): Coordinates? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val decoded = decode(text)
        placePattern.find(decoded)?.let { return coordinates(it.groupValues[1], it.groupValues[2]) }
        atPattern.find(decoded)?.let { return coordinates(it.groupValues[1], it.groupValues[2]) }
        for (key in queryKeys) {
            val match = Regex("(?:[?&])$key=([^&]+)", RegexOption.IGNORE_CASE).find(decoded) ?: continue
            val value = decode(match.groupValues[1])
            parsePair(value)?.let { return it }
        }
        return parsePair(decoded)
    }

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, "UTF-8")
    }.getOrDefault(value)

    private fun parsePair(value: String): Coordinates? {
        val cleaned = value.trim().trim('"', '\'')
        val match = Regex("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*[,;\\s]+\\s*(-?\\d+(?:\\.\\d+)?)(?:\\D.*)?$").find(cleaned)
            ?: return null
        return coordinates(match.groupValues[1], match.groupValues[2])
    }

    private fun coordinates(latText: String, lonText: String): Coordinates? {
        val latitude = latText.toDoubleOrNull() ?: return null
        val longitude = lonText.toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return Coordinates(latitude, longitude)
    }
}
