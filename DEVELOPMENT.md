# Development guide

## Toolchain

- Kotlin 2.0.21 with the Compose compiler plugin.
- Gradle wrapper 9.9.2.
- Android Gradle Plugin with `compileSdk 34`, `targetSdk 34`, `minSdk 26`.
- JDK 17+ (Gradle 9 requirement).
- Room 2.7.2 via kapt, DataStore Preferences, Navigation Compose, Coroutines/Flow.

`local.properties` must point `sdk.dir` at a local Android SDK containing platform 34. It is intentionally gitignored.

## Commands

```powershell
# Compile and package the debug APK
.\gradlew.bat assembleDebug --no-daemon

# Compile and package the release APK (debug-signed for direct install)
.\gradlew.bat assembleRelease --no-daemon

# Run JVM unit tests for debug and release variants
.\gradlew.bat test --no-daemon

# Install debug build if a device/emulator is attached
.\gradlew.bat installDebug --no-daemon
```

## Project structure

See the architecture tree in [`README.md`](README.md). The most important entry points are:

- `AppContainer.kt` — manual singleton wiring and app-scoped coroutine scope.
- `NearbyTransport.kt` — real Nearby Connections lifecycle and byte payload flow.
- `ConnectionManager.kt` — encryption, replay checks and unified send/receive primitives.
- `WireProtocol.kt` / `WireKind` — one authenticated envelope for all features.
- `AppDatabase.kt` — Room entities and DAOs.
- `AppNavGraph.kt` — all Compose routes, including groups and voice calls.

## Adding a feature over the existing transport

1. Add a `WireKind` value in `ConnectionManager.kt` if a new message class is needed.
2. Send with `ConnectionManager.sendWireToDevice(...)` or `sendWireToAll(...)`.
3. Collect `connection.incomingWire` in an app-scoped repository/manager. The flow is `shareIn`-decoded once, so replay protection is not consumed twice.
4. Persist state in Room and expose it to a Compose ViewModel with `stateIn`.
5. Keep payloads small; file/voice data uses the chunked `FileTransferManager` pipeline.

## Database and preferences

- Database: `hb_offline_chat.db`, version 1, `exportSchema = true`.
- Preferences DataStore: `hb_chat_prefs` (dark theme, display name).
- No migration is required yet because the schema is at version 1. Any future schema change must add an explicit `Migration` in `AppContainer`.

## Testing notes

Unit tests cover crypto, replay protection and the endpoint-name codec. UI and real transport tests require physical devices; no emulator system images or attached devices were available during this work. Do not report a feature as device-tested unless it was actually exercised on at least two, preferably three, real devices.

## Git workflow

Commit source, docs and APKs together. APKs are stored in `apk/` so they are not hidden by the `**/build/` ignore rule. `java_pid2944.hprof` and `flutter.create` are pre-existing local/user files and must not be deleted.
