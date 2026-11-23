package com.blankpages.trackify.update

import java.net.HttpURLConnection
import java.net.URL

/**
 * Pengecek rilis terbaru GitHub. Mengikuti pola `MapLinkResolver`: `HttpURLConnection`,
 * timeout, dan `runCatching` — tanpa Retrofit/OkHttp/Ktor. Blokir: panggil di IO.
 *
 * Tidak mengirim data aplikasi; hanya GET publik. Kegagalan jaringan menghasilkan `null`.
 */
object ReleaseUpdateChecker {
    private const val LATEST_URL = "https://api.github.com/repos/drezvolx/Trackify/releases/latest"
    private const val TIMEOUT_MS = 8_000

    fun fetchLatest(): ReleaseInfo? {
        val connection = runCatching { URL(LATEST_URL).openConnection() as HttpURLConnection }.getOrNull()
            ?: return null
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Trackify-UpdateChecker")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return ReleaseInfoParser.parse(body)
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }
}
