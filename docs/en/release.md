# Release

Push a `v*` semantic version tag to run `.github/workflows/release.yml`. The tag sets both Android version fields. JDK 17 and Android SDK 35 build and lint the APK. Releases contain signed `nl2sh-android-bridge.apk`, `SHA256SUMS`, and LICENSE. The build packages `assets/nl2sh-runtime.json` with the same version and protocol as the running application.

The signing job requires the existing GitHub Actions secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. It restores the keystore only in the runner temporary directory, verifies the APK with `apksigner`, and removes the temporary keystore even on failure. Keep a stable signing identity for upgrades; Android refuses an in-place update signed by a different key. Existing debug installations need an explicit migration. Never commit keys or passwords.

Local builds without signing configuration may produce unsigned release APKs. To sign locally, use ignored `keystore.properties` containing `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`, or environment variables `NL2SH_BRIDGE_KEYSTORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Partial configuration fails the build. Production signing may be deferred locally. Debug APKs use a development key.

The native release aggregates this APK into its signed compatibility manifest, recording both the asset SHA-256 and the APK certificate SHA-256. These digests identify different things and both must match before installation. Publishing a companion does not activate Accessibility or select its keyboard. See the [guide](guide.md) for permissions and manual activation.
