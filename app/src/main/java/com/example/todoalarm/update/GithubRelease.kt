package com.example.todoalarm.update

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

data class AppRelease(val version: String, val page: String, val notes: String, val apk: String?, val size: Long, val digest: String?) {
    fun json() = JSONObject().put("version", version).put("page", page).put("notes", notes).put("apk", apk).put("size", size).put("digest", digest).toString()
}

object GithubRelease {
    const val REPO = "HoshiTakyobu/PaykiTodo"
    const val API = "https://api.github.com/repos/$REPO/releases?per_page=30"
    const val PAGE = "https://github.com/$REPO/releases/latest"
    fun compare(a: String, b: String): Int {
        fun parts(v: String) = v.removePrefix("v").also { require(it.matches(Regex("\\d+\\.\\d+\\.\\d+"))) }.split('.').map(::BigInteger)
        return parts(a).zip(parts(b)).firstOrNull { it.first != it.second }?.let { it.first.compareTo(it.second) } ?: 0
    }
    fun parse(text: String, current: String): AppRelease? {
        val releases = JSONArray(text)
        return (0 until releases.length()).mapNotNull { i ->
            val r = releases.getJSONObject(i)
            if (r.optBoolean("draft") || r.optBoolean("prerelease")) return@mapNotNull null
            val tag = r.getString("tag_name")
            if (runCatching { compare(tag, current) }.getOrNull()?.let { it <= 0 } != false) return@mapNotNull null
            val version = tag.removePrefix("v")
            val page = "https://github.com/$REPO/releases/tag/$tag"
            if (r.optString("html_url") != page) return@mapNotNull null
            val name = "PaykiTodo-$version-release.apk"
            val assets = r.optJSONArray("assets") ?: JSONArray()
            val asset = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull {
                it.optString("name") == name && it.optString("browser_download_url") == "https://github.com/$REPO/releases/download/$tag/$name" && it.optLong("size") in 1..300_000_000
            }
            AppRelease(version, page, r.optString("body").take(12000), asset?.getString("browser_download_url"), asset?.getLong("size") ?: 0,
                asset?.optString("digest")?.takeIf { it.matches(Regex("sha256:[0-9a-fA-F]{64}")) }?.removePrefix("sha256:"))
        }.maxWithOrNull { a, b -> compare(a.version, b.version) }
    }
    fun restore(json: String): AppRelease? = runCatching {
        val r = JSONObject(json)
        val version = r.getString("version"); compare(version, version)
        val page = "https://github.com/$REPO/releases/tag/v$version"
        require(r.getString("page") == page)
        val apk = r.optString("apk").takeIf { it != "null" && it.isNotBlank() }
        require(apk == null || apk == "https://github.com/$REPO/releases/download/v$version/PaykiTodo-$version-release.apk")
        AppRelease(version, page, r.optString("notes"), apk, r.optLong("size"), r.optString("digest").takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) })
    }.getOrNull()
    fun compatibleSigners(installed: Set<String>, archive: Set<String>, history: Set<String>): Boolean = installed.isNotEmpty() &&
        (installed == archive || installed.size == 1 && archive.size == 1 && history.containsAll(installed))
}
