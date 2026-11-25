# Testing and limitations report

Date: 2026-11-25

## Commands executed

```powershell
.\gradlew.bat assembleDebug --no-daemon
.\gradlew.bat assembleRelease --no-daemon
.\gradlew.bat test --no-daemon
adb devices
```

## Results

| Check | Result | Evidence |
| --- | --- | --- |
| Debug compilation and packaging | PASS | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |
| Release compilation and packaging | PASS | `BUILD SUCCESSFUL`, `app/build/outputs/apk/release/app-release.apk` |
| JVM unit tests (debug + release variants) | PASS | `BUILD SUCCESSFUL`; test classes below |
| Connected devices available | NONE | `adb devices` listed no device |
| Emulator system image available | NONE | No usable emulator image installed |
| Three-real-device end-to-end test | NOT PERFORMED | No physical devices available in this environment |

### Unit test classes

- `CryptoManagerTest` — AES-GCM round trip/tamper failure, HKDF derivation, short-code determinism, file hash.
- `ReplayProtectionTest` — fresh/duplicate/old/stale-future message handling.
- `NearbyTransportTest` — endpoint-name encode/decode and malformed endpoint rejection.

Both variants produced the same set of unit tests: 11 tests per variant, 22 total XML entries, 0 failures and 0 errors.

## APK verification

- Debug APK copied to `apk/hb-offline-chat-debug.apk`.
- Release APK copied to `apk/hb-offline-chat-release.apk`.
- Release uses the debug signing config, so it is installable but not suitable for Play Store upload.
- APK sizes are below GitHub's per-file limit.
- SHA-256:
  - `hb-offline-chat-debug.apk`: `96A8CB672EEAE3A844E1B6302FBE741DF1743B948F67B772D8AE89EDBAA7D106`
  - `hb-offline-chat-release.apk`: `E23B6AF94901747F6D9BCBE94510DD485F68D9FE5467AB5225EE170CFE0103AB`

## What was NOT validated

The following require real devices and therefore must not be reported as tested:

- Real Nearby Connections discovery across devices.
- Connection request accept/reject handshake on two phones.
- Encrypted text delivery, offline queue retry and conversation list on real peers.
- File/photo/video transfer with progress, cancel and SHA-256 verification on real peers.
- Voice-message recording/playback and encrypted transfer on real peers.
- Group coordinator forwarding with three devices.
- Live voice-call audio, mute/speaker behaviour and call teardown.
- Background notification delivery.
- Behaviour on devices without Google Play services.
- Behaviour on API 26–30 devices, where Keystore ECDH key agreement is unavailable.

## Required field validation before production use

1. Use three physical Android 31+ devices with Play services, Bluetooth and Wi-Fi enabled.
2. Verify mutual discovery and accept/reject on all devices.
3. Exchange text on one-to-one and group chats; verify duplicate suppression and offline queue retry.
4. Transfer a small and a large file; compare received SHA-256 values and Downloads visibility.
5. Record and play voice messages.
6. Place, answer, mute, speaker-toggle and end a voice call; record latency/echo observations.
7. Force-stop the app and verify notifications for incoming events.
8. Repeat a discovery check on a device without Play services and document the failure path.

Until these steps are completed with passing notes, the app should be described as **compile- and unit-tested**, not field-tested.
