package com.blankpages.trackify

import com.blankpages.trackify.update.ReleaseInfoParser
import com.blankpages.trackify.update.ReleaseVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseUpdateTest {

    @Test
    fun parseVersionHandlesTagPrefixAndShortForm() {
        assertEquals(ReleaseVersion(1, 0, 0), ReleaseVersion.parse("v1.0.0"))
        assertEquals(ReleaseVersion(1, 1, 0), ReleaseVersion.parse("1.1"))
        assertEquals(ReleaseVersion(2, 0, 0), ReleaseVersion.parse("V2"))
        assertEquals(ReleaseVersion(1, 1, 0), ReleaseVersion.parse("v1.1.0-beta"))
        assertEquals(ReleaseVersion(1, 1, 0), ReleaseVersion.parse("v1.1.0+build7"))
    }

    @Test
    fun parseVersionRejectsInvalidInput() {
        assertNull(ReleaseVersion.parse(null))
        assertNull(ReleaseVersion.parse(""))
        assertNull(ReleaseVersion.parse("latest"))
        assertNull(ReleaseVersion.parse("1.2.3.4"))
        assertNull(ReleaseVersion.parse("1.-2.0"))
    }

    @Test
    fun comparesVersionsNumerically() {
        assertTrue(ReleaseVersion.parse("v1.1.0")!! > ReleaseVersion.parse("1.0")!!)
        assertTrue(ReleaseVersion.parse("v1.0.1")!! > ReleaseVersion.parse("v1.0.0")!!)
        assertTrue(ReleaseVersion.parse("v1.10.0")!! > ReleaseVersion.parse("v1.9.9")!!)
        assertEquals(ReleaseVersion.parse("v1.0"), ReleaseVersion.parse("1.0.0"))
    }

    @Test
    fun detectsNewerReleaseAgainstInstalledVersion() {
        val payload = """
            {
              "tag_name": "v1.1.0",
              "name": "Trackify v1.1.0",
              "html_url": "https://github.com/drezvolx/Trackify/releases/tag/v1.1.0",
              "assets": [],
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()
        val release = ReleaseInfoParser.parse(payload)!!
        assertTrue(release.isNewerThan("1.0"))
        assertFalse(release.isNewerThan("1.1"))
        assertFalse(release.isNewerThan("v1.2.0"))
        assertFalse(release.isNewerThan(null))
    }

    @Test
    fun parsesGithubLatestPayloadAndFirstApkAsset() {
        val payload = """
            {
              "tag_name": "v1.1.0",
              "name": "Trackify v1.1.0",
              "html_url": "https://github.com/drezvolx/Trackify/releases/tag/v1.1.0",
              "assets": [
                { "name": "notes.txt", "browser_download_url": "https://example.com/notes.txt" },
                { "name": "trackify-v1.1.0.apk", "browser_download_url": "https://example.com/app.apk" }
              ]
            }
        """.trimIndent()
        val release = ReleaseInfoParser.parse(payload)!!
        assertEquals("v1.1.0", release.tag)
        assertEquals("Trackify v1.1.0", release.name)
        assertEquals("https://github.com/drezvolx/Trackify/releases/tag/v1.1.0", release.url)
        assertEquals("https://example.com/app.apk", release.downloadUrl)
    }

    @Test
    fun parserRejectsMalformedOrUnusablePayloads() {
        assertNull(ReleaseInfoParser.parse("not json"))
        assertNull(ReleaseInfoParser.parse("""{"tag_name":"v1.1.0"}"""))
        assertNull(ReleaseInfoParser.parse("""{"html_url":"https://example.com"}"""))
        assertNull(
            ReleaseInfoParser.parse(
                """{"tag_name":"v1.1.0","html_url":"https://example.com","prerelease":true}""",
            ),
        )
        val noAssets = ReleaseInfoParser.parse(
            """{"tag_name":"v1.0.0","html_url":"https://example.com","assets":[]}""",
        )!!
        assertNull(noAssets.downloadUrl)
        assertEquals("v1.0.0", noAssets.name)
    }
}
