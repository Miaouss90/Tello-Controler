# Tello protocol

Target: Tello EDU / RoboMaster TT SDK 3.0.

| Function | Endpoint |
|---|---|
| Commands | Tello 192.168.10.1:8889 UDP |
| State | local UDP 8890 |
| Video | local UDP 11111 |

Startup target: open transport -> send `command` -> confirm SDK acknowledgement -> send `streamon` -> receive/decode video.

Manual control uses `rc a b c d` for left/right, forward/back, up/down and yaw velocity values in the SDK-defined range.

Primary reference: DJI/Ryze RoboMaster TT Tello SDK 3.0 User Guide.
