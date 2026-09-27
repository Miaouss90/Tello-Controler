package com.miaouss90.tellocontroler.update

import org.json.JSONObject

data class ReleaseInfo(
    val tag: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
) {
    companion object {
        /** Parses a GitHub `releases/latest` response; null when it has no APK asset. */
        fun fromGitHubJson(json: String): ReleaseInfo? {
            val root = JSONObject(json)
            val assets = root.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    return ReleaseInfo(
                        tag = root.getString("tag_name"),
                        apkUrl = asset.getString("browser_download_url"),
                        apkSizeBytes = asset.optLong("size", -1L),
                    )
                }
            }
            return null
        }
    }
}
