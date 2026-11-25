# HB Offline Chat

A native Android app for direct device-to-device communication **without SIM, mobile data, internet, router, cloud, or central server**. Peers are discovered and connected with Google Nearby Connections over local Bluetooth/Wi-Fi links. All application payloads are encrypted end-to-end with keys derived on the device.

> **Status:** The codebase compiles for debug and release, JVM unit tests pass, and APKs are included in [`apk/`](apk/). Live validation on three physical devices has **not** been performed in this environment; see [`docs/TESTING_REPORT.md`](docs/TESTING_REPORT.md) for the honest validation record.

## Features

- Real Nearby Connections discovery, advertising, connect, accept/reject, disconnect and retry.
- Android Keystore EC identity; random device ID and name, no phone number or account.
- ECDH + HKDF-SHA256 + AES-256-GCM authenticated encryption for every wire payload.
- Replay protection with a sliding window and unique message IDs.
- One-to-one text chat persisted in Room, with queued retry for offline peers.
- Host-coordinated group chat with member add/remove and duplicate message suppression.
- Chunked encrypted file/photo/video/document transfer, progress, cancel, SHA-256 verification and Downloads storage via SAF.
- Voice messages recorded with `MediaRecorder`, transferred through the encrypted file pipeline, and played back locally.
- Peer-to-peer voice calls using `AudioRecord`/`AudioTrack` over the encrypted wire channel (mute, speaker, duration).
- Local notifications for incoming messages, calls and connection requests.
- Device block/trust controls, 6-digit public-key verification codes, chat search, dark/light theme setting.

## Requirements

- Android 8.0 (API 26) or newer.
- Google Play services available on both devices (Nearby Connections dependency).
- Bluetooth and/or Wi-Fi enabled; location/nearby-devices permissions as required by the Android version.
- Microphone permission for voice messages/calls; notification permission on Android 13+.

The app does **not** require a SIM, internet connection, account, Firebase, cloud backend or server.

## Build

```powershell
.\gradlew.bat assembleDebug --no-daemon
.\gradlew.bat assembleRelease --no-daemon
.\gradlew.bat test --no-daemon
```

APK outputs:

| Variant | Source output | Committed copy |
| --- | --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `apk/hb-offline-chat-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` | `apk/hb-offline-chat-release.apk` |

The release variant currently uses the debug signing config so it can be installed directly. For Play Store distribution, replace it with a private upload key.

## Usage

1. Install the APK on two or more nearby Android devices.
2. Open the app, grant the requested discovery permissions and keep Bluetooth/Wi-Fi on.
3. Tap **Scan / Nearby**, wait for peers, then tap a device to request a connection.
4. The receiving device accepts or rejects the request. Once connected, use chat, groups, files, voice notes or voice calls.
5. In **Settings → Privacy & security**, compare verification codes and block/trust known peers.

## Architecture

```text
app/src/main/java/com/hb/offlinechat/
├── connectivity/    Nearby transport, wire protocol, session keys
├── core/            Permissions, crypto, identity, utilities
├── data/            Room database, DAOs, DataStore, repositories
├── domain/          Shared models
├── messaging/       Group coordinator
├── presentation/    Compose screens, ViewModels, navigation, theme
├── service/         Foreground service and notifications
├── transfer/        Chunked file transfer
└── voice/           Voice messages and calls
```

Dependency injection is manual through `AppContainer`. Coroutines/Flow connect the managers, Room persists data, and all features share one authenticated encrypted `ConnectionManager.incomingWire` flow.

## Known limitations

- Nearby Connections uses Google Play services; devices without Play services cannot connect. Native Wi-Fi Direct and Bluetooth transports are not implemented.
- Keystore ECDH key agreement is enabled on API 31+. On older devices, app-level session keys cannot be established, so encrypted features will not work.
- It is a single-hop local cluster, not a multi-hop mesh; group forwarding depends on the coordinator being in range.
- Three-device field testing, live call-quality testing and Play Store release signing have not been completed.
- Voice call quality depends on the local radio and device echo-cancellation hardware.

See [`docs/TESTING_REPORT.md`](docs/TESTING_REPORT.md), [`docs/PHASE_REPORTS.md`](docs/PHASE_REPORTS.md) and [`SECURITY.md`](SECURITY.md) for details.

## Repository

Remote: `https://github.com/bijaykumarceh-dev/HB-offline-chat-app.git`
Branch: `main`
Package: `com.hb.offlinechat`
