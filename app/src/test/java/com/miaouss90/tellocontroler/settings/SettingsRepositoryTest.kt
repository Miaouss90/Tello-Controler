package com.miaouss90.tellocontroler.settings

import android.content.SharedPreferences
import com.miaouss90.tellocontroler.controller.StickMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.lang.reflect.Proxy

class SettingsRepositoryTest {
    @Test
    fun `new installation follows default across unrelated edits and reload`() {
        val store = PreferenceStore()
        val repository = SettingsRepository(store.preferences)
        assertEquals(0.15f, repository.settings.value.deadZone)
        repository.update { it.copy(hudHorizon = false, rate = RateProfile.SLOW) }
        assertFalse(store.values.containsKey("dead_zone"))
        val reloaded = SettingsRepository(store.preferences).settings.value
        assertEquals(0.15f, reloaded.deadZone)
        assertFalse(reloaded.hudHorizon)
        assertEquals(RateProfile.SLOW, reloaded.rate)
        assertEquals(0f, StickMapper.shape(0.12f, reloaded.deadZone))
    }

    @Test
    fun `legacy and customized values survive load unrelated edit and restart`() {
        // 8 % is ambiguous: it may have been deliberately chosen.
        for (value in listOf(0.08f, 0.12f, 0.15f, 0.22f)) {
            val store = PreferenceStore(mapOf("dead_zone" to value))
            val repository = SettingsRepository(store.preferences)
            assertEquals(value, repository.settings.value.deadZone)
            repository.update { it.copy(rumbleAlerts = false) }
            assertEquals(value, store.values["dead_zone"])
            assertEquals(value, SettingsRepository(store.preferences).settings.value.deadZone)
        }
    }

    @Test
    fun `explicit default adoption removes override and preserves other settings`() {
        val store = PreferenceStore(mapOf("dead_zone" to 0.08f, "expo" to 0.4f))
        val repository = SettingsRepository(store.preferences)
        repository.useDefaultDeadZone()
        repository.useDefaultDeadZone()
        assertFalse(store.values.containsKey("dead_zone"))
        assertEquals(0.15f, repository.settings.value.deadZone)
        repository.update { it.copy(flightLogs = false) }
        val reloaded = SettingsRepository(store.preferences)
        assertEquals(0.15f, reloaded.settings.value.deadZone)
        assertEquals(0.4f, reloaded.settings.value.expo)
        assertFalse(reloaded.settings.value.flightLogs)
        assertFalse(store.values.containsKey("dead_zone"))
        assertEquals(0f, StickMapper.shape(0.12f, reloaded.settings.value.deadZone))
    }

    @Test
    fun `customization after adopting default persists including eight percent`() {
        val store = PreferenceStore(mapOf("dead_zone" to 0.08f))
        val repository = SettingsRepository(store.preferences)
        repository.useDefaultDeadZone()
        repository.update { it.copy(deadZone = 0.08f) }
        assertEquals(0.08f, SettingsRepository(store.preferences).settings.value.deadZone)
    }

    @Test
    fun `explicit slider change on new install persists`() {
        val store = PreferenceStore()
        SettingsRepository(store.preferences).update { it.copy(deadZone = 0.2f) }
        assertEquals(0.2f, SettingsRepository(store.preferences).settings.value.deadZone)
    }

    /** Small in-memory SharedPreferences adapter; exercises the real repository without Android runtime. */
    private class PreferenceStore(initial: Map<String, Any> = emptyMap()) {
        val values = initial.toMutableMap()
        private val pending = mutableMapOf<String, Any?>()
        private val editor = proxy(SharedPreferences.Editor::class.java) { method, args ->
            when {
                method.startsWith("put") -> {
                    pending[args[0] as String] = args[1]
                    editorInstance()
                }
                method == "remove" -> {
                    pending[args[0] as String] = null
                    editorInstance()
                }
                method == "apply" || method == "commit" -> {
                    pending.forEach { (key, value) ->
                        if (value == null) values.remove(key) else values[key] = value
                    }
                    pending.clear()
                    if (method == "commit") true else null
                }
                else -> error("Unexpected editor method: $method")
            }
        }

        private fun editorInstance(): SharedPreferences.Editor = editor

        val preferences = proxy(SharedPreferences::class.java) { method, args ->
            when {
                method == "edit" -> editor
                method == "contains" -> values.containsKey(args[0])
                method.startsWith("get") -> values[args[0]] ?: args[1]
                else -> error("Unexpected preference method: $method")
            }
        }

        private fun <T> proxy(type: Class<T>, call: (String, Array<out Any>) -> Any?): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                call(method.name, args ?: emptyArray())
            })
    }
}
