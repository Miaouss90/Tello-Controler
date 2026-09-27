package com.miaouss90.tellocontroler.tello

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.PatternMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TelloWifiState { IDLE, SEARCHING, LOCKED, LOST, UNAVAILABLE }

/**
 * Holds an explicit, Internet-less Wi-Fi network for the Tello so Android does not route our UDP traffic
 * to mobile data. Android 10+: WifiNetworkSpecifier on `TELLO-*` (system confirmation dialog on first use).
 * Android 8–9: the Wi-Fi the user joined manually. Sockets are bound per socket, never process-wide, so the
 * rest of the app (in-app update) keeps Internet over mobile data.
 *
 * Wi-Fi lock validated on the owner's phone (2026-09-27).
 * HARDWARE-UNVERIFIED: loss/auto-reconnection path; dialog persistence varies by phone vendor.
 */
class TelloWifiManager(context: Context) {
    companion object {
        const val SSID_PREFIX = "TELLO-"
    }

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    // Keeps the radio out of power save while linked: fewer dropped video datagrams, lower latency.
    @Suppress("DEPRECATION")
    private val wifiLock = context.applicationContext.getSystemService(WifiManager::class.java)?.createWifiLock(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) WifiManager.WIFI_MODE_FULL_LOW_LATENCY else WifiManager.WIFI_MODE_FULL_HIGH_PERF,
        "tello-controler",
    )?.apply { setReferenceCounted(false) }
    private var callback: ConnectivityManager.NetworkCallback? = null

    private val _state = MutableStateFlow(TelloWifiState.IDLE)
    val state: StateFlow<TelloWifiState> = _state.asStateFlow()

    private val _network = MutableStateFlow<Network?>(null)
    val network: StateFlow<Network?> = _network.asStateFlow()

    /**
     * Idempotent while searching or locked. After a loss the old request is dead (Android does not re-deliver
     * a specifier network), so it is replaced by a fresh one.
     */
    fun request() {
        val state = _state.value
        if (callback != null && (state == TelloWifiState.SEARCHING || state == TelloWifiState.LOCKED)) return
        release()
        val builder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setNetworkSpecifier(
                WifiNetworkSpecifier.Builder()
                    .setSsidPattern(PatternMatcher(SSID_PREFIX, PatternMatcher.PATTERN_PREFIX))
                    .build(),
            )
        }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runCatching { wifiLock?.acquire() }
                _network.value = network
                _state.value = TelloWifiState.LOCKED
            }

            override fun onLost(network: Network) {
                if (_network.value == network) {
                    runCatching { wifiLock?.release() }
                    _network.value = null
                    _state.value = TelloWifiState.LOST
                }
            }

            override fun onUnavailable() {
                _network.value = null
                _state.value = TelloWifiState.UNAVAILABLE
            }
        }
        _state.value = TelloWifiState.SEARCHING
        try {
            connectivity.requestNetwork(builder.build(), cb)
            callback = cb
        } catch (_: RuntimeException) {
            _state.value = TelloWifiState.UNAVAILABLE
        }
    }

    fun release() {
        callback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }
        callback = null
        runCatching { wifiLock?.release() }
        _network.value = null
        _state.value = TelloWifiState.IDLE
    }
}
