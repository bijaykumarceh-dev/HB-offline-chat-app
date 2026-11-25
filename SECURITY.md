# Security notes

## Threat model

HB Offline Chat protects messages exchanged over a local peer-to-peer link from passive eavesdroppers and casual active tampering on that link. It does **not** protect against a compromised device, malicious Google Play services, or an attacker who already has the device unlock credential and Keystore access.

There is no account, cloud storage, server, SIM dependency or internet backend.

## Identity

- Each install generates an EC P-256 keypair in the Android Keystore (`hb_offline_chat_identity_v1`).
- The private key is generated with `PURPOSE_SIGN`, `PURPOSE_VERIFY` and, on API 31+, `PURPOSE_AGREE_KEY`.
- The public device ID is the first 8 bytes of `SHA-256(publicKey)`; the display name is `HB-` plus 6 ID characters.
- No phone number, contact list, location history or personal profile is broadcast.

## Transport encryption

1. Peers exchange public keys inside the Nearby endpoint name.
2. `DeviceIdentity.deriveSharedSecret()` performs ECDH.
3. `CryptoManager.deriveSessionKey()` applies HKDF-HMAC-SHA256 with a fixed app salt/info to derive an AES-256 key.
4. Every wire payload is encrypted with AES-256-GCM (12-byte random IV, 128-bit tag) via `CryptoManager.encrypt/decrypt`.
5. `ReplayProtection` rejects duplicate message IDs and timestamps outside a 120-second window.
6. Every feature (text, file chunks, group text, call audio) uses the same authenticated `WireMessage` envelope.

Standard platform primitives are used; no custom cipher or homemade cryptographic protocol is implemented.

## Verification and device controls

- **Privacy & security** shows a 6-digit code derived from the sorted local/remote public keys. Compare it on both devices. A mismatch means the peer is not the device you expected.
- Known peers can be marked trusted or blocked.
- Blocked peers are removed from discovery, their connection requests are rejected, and existing links are disconnected.

## Storage

- Messages, groups, transfers and call history are stored in local Room DB `hb_offline_chat.db`.
- Preferences are stored in DataStore `hb_chat_prefs`.
- Received files are written to app-visible Downloads locations using SAF.
- Plain text is stored locally for UI display; encrypted ciphertext is also stored for sent text messages. There is no remote backup.

## Limitations and cautions

- **API 26–30:** Keystore key agreement is not enabled, so `deriveSharedSecret()` fails and encrypted sessions cannot be established. API 31+ is required for full encrypted functionality.
- Nearby Connections requires Google Play services. Devices without it cannot participate.
- Session keys are derived once per connected endpoint and cached in memory; there is no forward secrecy or periodic rekeying.
- Short-code verification is user-driven; the app cannot force users to compare codes.
- Voice call audio is UDP-like and not retransmitted; packet loss, latency and echo depend on the radio and device hardware.
- Group messaging is coordinator-based and single-hop, not a cryptographic group-key protocol. The coordinator sees group plaintext.

## Reporting issues

When reporting a security concern, include the Android version, device model, Play services version, reproduction steps and whether the devices were physically connected. Do not include private keys or real user data.
