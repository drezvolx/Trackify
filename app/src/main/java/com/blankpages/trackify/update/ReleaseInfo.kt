package com.blankpages.trackify.update

import org.json.JSONObject

/** Data rilis terbaru yang dipakai dialog pembaruan. Tidak ada instal otomatis. */
data class ReleaseInfo(val tag: String, val name: String, val url: String, val downloadUrl: String? = null) {
    val version: ReleaseVersion?
        get() = ReleaseVersion.parse(tag)

    /** Membandingkan tag rilis dengan versi yang terpasang dari PackageManager. */
    fun isNewerThan(installedVersion: String?): Boolean {
        val remote = version ?: return false
        val installed = ReleaseVersion.parse(installedVersion) ?: return false
        return remote > installed
    }
}

/** Parser murni untuk payload `GET /repos/drezvolx/Trackify/releases/latest`. */
object ReleaseInfoParser {
    fun parse(body: String): ReleaseInfo? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) return null

        val tag = json.optString("tag_name").trim()
        val url = json.optString("html_url").trim()
        if (tag.isEmpty() || url.isEmpty()) return null

        val name = json.optString("name").trim().ifEmpty { tag }
        val assets = json.optJSONArray("assets")
        var downloadUrl: String? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val assetName = asset.optString("name").trim()
                val candidate = asset.optString("browser_download_url").trim()
                if (candidate.isNotEmpty() && assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = candidate
                    break
                }
            }
        }
        return ReleaseInfo(tag = tag, name = name, url = url, downloadUrl = downloadUrl)
    }
}
