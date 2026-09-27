# Tello protocol

Target: Tello EDU / RoboMaster TT SDK 3.0.

| Function | Endpoint |
|---|---|
| Commands | Tello 192.168.10.1:8889 UDP |
| State | local UDP 8890 |
| Video | local UDP 11111 |

Startup sequence: lock the `TELLO-*` Wi-Fi as an explicit Internet-less network -> bind sockets to it -> send `command` -> confirm `ok` -> send `streamon` -> receive/decode video.

Android routes unbound sockets through the *default* network, which is mobile data when the Wi-Fi has no Internet: every Tello socket must be bound to the Tello `Network`.

Manual control uses `rc a b c d` for left/right, forward/back, up/down and yaw velocity values in the SDK-defined range.

Primary reference: DJI/Ryze RoboMaster TT Tello SDK 3.0 User Guide.
