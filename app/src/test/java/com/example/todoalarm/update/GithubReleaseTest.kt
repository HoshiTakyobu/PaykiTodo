package com.example.todoalarm.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GithubReleaseTest {
    private fun release(version: String, draft: Boolean = false, prerelease: Boolean = false, asset: String = "PaykiTodo-$version-release.apk") =
        JSONObject().put("tag_name", "v$version").put("draft", draft).put("prerelease", prerelease)
            .put("html_url", "https://github.com/${GithubRelease.REPO}/releases/tag/v$version").put("body", "说明")
            .put("assets", JSONArray().put(JSONObject().put("name", asset).put("size", 5000)
                .put("browser_download_url", "https://github.com/${GithubRelease.REPO}/releases/download/v$version/$asset")))
    @Test fun versionsCompareNumericallyAndMalformedVersionsReject() {
        assertTrue(GithubRelease.compare("v1.17.11", "1.17.9") > 0)
        assertEquals(0, GithubRelease.compare("1.17.11", "v1.17.11"))
        assertTrue(runCatching { GithubRelease.compare("1.17.12-beta", "1.17.11") }.isFailure)
        assertTrue(GithubRelease.compare("2.0.0", "1.999.999") > 0)
    }
    @Test fun stableReleaseSelectionRequiresRepositoryAndExactReleaseApk() {
        val releases = JSONArray().put(release("1.17.9")).put(release("1.17.12", prerelease = true)).put(release("1.18.0", draft = true)).put(release("1.17.11"))
        val found = GithubRelease.parse(releases.toString(), "1.17.10")!!
        assertEquals("1.17.11", found.version); assertNotNull(found.apk)
        assertNull(GithubRelease.parse(releases.toString(), "1.17.11"))
        assertNull(GithubRelease.parse(JSONArray().put(release("1.17.11", asset = "PaykiTodo-1.17.11-debug.apk")).toString(), "1.17.10")!!.apk)
        assertNull(GithubRelease.parse(JSONArray().put(release("1.17.11").put("html_url", "https://example.com/download")).toString(), "1.17.10"))
    }
    @Test fun cacheRoundTripDoesNotAcceptArbitraryDownloadUrls() {
        val found = GithubRelease.parse(JSONArray().put(release("1.17.11")).toString(), "1.17.10")!!
        assertEquals(found, GithubRelease.restore(found.json()))
        assertNull(GithubRelease.restore(JSONObject(found.json()).put("apk", "https://example.com/app.apk").toString()))
    }
    @Test fun signingIdentityAcceptsRotationAndRejectsWrongOrEmptyKeys() {
        assertTrue(GithubRelease.compatibleSigners(setOf("old"), setOf("old"), emptySet()))
        assertTrue(GithubRelease.compatibleSigners(setOf("old"), setOf("new"), setOf("old", "new")))
        assertFalse(GithubRelease.compatibleSigners(setOf("debug"), setOf("release"), setOf("release")))
        assertFalse(GithubRelease.compatibleSigners(emptySet(), emptySet(), emptySet()))
        assertFalse(GithubRelease.compatibleSigners(setOf("a", "b"), setOf("a"), setOf("a", "b")))
    }
}
