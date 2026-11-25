# Phase reports

This file records what was implemented and what was actually validated. “Compile verified” means the Gradle build succeeded; “unit verified” means JVM tests passed. It does not mean real-device functionality passed.

| Phase | Delivered | Validation status |
| --- | --- | --- |
| 0 — Analysis | Requirements decomposition, offline-only constraints, dependency choices, package and min API decisions. | Complete |
| 1 — Foundation | Kotlin/Compose project, Material 3 dark neon theme, Gradle wrapper, MainActivity, Navigation Compose. | Compile verified, debug APK produced |
| 2 — UI shell | Welcome, profile setup, splash, home, nearby devices, connection, one-to-one chat, groups, files, voice messages, voice call, settings, privacy, about screens. | Compile verified; navigation wired |
| 3 — Permissions | Version-aware Nearby/Bluetooth/Wi-Fi/location/nearby-device permissions with rationale and settings fallback. | Compile verified; no device permission-flow test |
| 4 — Storage | Room entities/DAOs for users, devices, conversations, messages, groups, members, transfers and calls; DataStore preferences. | Compile verified; schema version 1 |
| 5 — Connectivity | Real Google Nearby Connections advertising/discovery, accept/reject, connected endpoint state, payload flow, pending connection UI. | Compile verified; unit tests for endpoint codec; no real peer test |
| 6 — Identity & crypto | Keystore EC identity, public-key device ID, ECDH/HKDF/AES-GCM, replay protection, 6-digit verification code, block/trust controls. | Unit verified (`CryptoManagerTest`, `ReplayProtectionTest`); API 31+ caveat documented |
| 7 — Text messaging | Unified `WireProtocol`, encrypted one-to-one text, Room persistence, queued retry for disconnected peers, conversation list and search. | Compile verified; unit tests pass |
| 8 — File sharing | SAF picker, chunked encrypted transfer, accept/reject, cancel, progress, SHA-256 verification, Downloads storage. | Compile verified; no real-device transfer test |
| 9 — Voice messages | `MediaRecorder` AAC/M4A recording, encrypted file-pipeline transfer, received voice list and playback. | Compile verified; no real-device audio test |
| 10 — Groups | Host-coordinated create/list groups, connected-member add/remove, forwarding with original message IDs, Room group history. | Compile verified; no multi-device group test |
| 11 — Service & notifications | Foreground connection service and notification channel; local notifications for messages, calls and connection requests. | Compile verified; no background-device test |
| 12 — Voice calls | Real `AudioRecord`/`AudioTrack` local P2P call signalling and PCM transport, mute/speaker/duration UI. | Compile verified; no live call-quality test |
| 13 — Polish & docs | Removed stale placeholder UI, wired recent chats/search/theme/block controls, README, development/security/test reports, APK copies. | Compile verified; final validation recorded in `TESTING_REPORT.md` |

## Deliberately not claimed

- No three-real-device session was available, so end-to-end discovery, connection, transfers, groups and calls are **not** claimed as device-tested.
- No emulator was available with a usable system image and Play services, so no emulator UI test was run.
- No Play Store production signing key was created; the release APK is debug-signed for direct installation.
