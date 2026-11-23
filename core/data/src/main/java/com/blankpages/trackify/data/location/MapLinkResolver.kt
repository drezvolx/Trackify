package com.blankpages.trackify.data.location

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Mengikuti redirect short link Google Maps sampai URL akhir yang memuat koordinat. Blokir: panggil di IO. */
object MapLinkResolver {
    private const val MAX_REDIRECTS = 5
    private const val TIMEOUT_MS = 8_000

    fun isShortLink(raw: String): Boolean {
        val lower = raw.trim().lowercase(Locale.ROOT)
        return lower.contains("maps.app.goo.gl") || lower.contains("goo.gl/maps")
    }

    fun resolve(raw: String): String? {
        var current = raw.trim()
        repeat(MAX_REDIRECTS) {
            val connection = runCatching { URL(current).openConnection() as HttpURLConnection }.getOrNull()
                ?: return null
            connection.instanceFollowRedirects = false
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            try {
                val code = connection.responseCode
                if (code !in 300..399) return current
                val next = connection.getHeaderField("Location") ?: return current
                current = if (next.startsWith("http")) next else URL(URL(current), next).toString()
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return current
    }
}
