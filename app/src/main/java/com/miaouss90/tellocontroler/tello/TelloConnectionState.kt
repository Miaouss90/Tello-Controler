package com.miaouss90.tellocontroler.tello

/** CONNECTED requires an `ok` to `command`; LINK_LOST means state packets stopped arriving. */
enum class TelloConnectionState { DISCONNECTED, CONNECTING, CONNECTED, LINK_LOST, ERROR }
