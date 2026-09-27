package com.miaouss90.tellocontroler.update

/** Pure semantic-version comparison for release tags such as `v0.3.42` or `0.3.0-dev`. */
object AppVersion {
    fun parse(version: String): List<Int> =
        version.trim().trimStart('v', 'V').substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    fun isNewer(candidate: String, installed: String): Boolean {
        val a = parse(candidate)
        val b = parse(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }
}
