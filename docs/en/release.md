# Releases

The Android project is independently versioned and published from this repository. Push a `v*` tag to run `.github/workflows/release.yml`; CI builds and lints with JDK 17 and Android SDK 35. Releases contain `nl2sh-android-bridge-unsigned.apk`, `SHA256SUMS`, and the project license.

The release APK is **unsigned** and must be signed before installation. Keep a stable private signing key outside the repository and use Android SDK `zipalign` followed by `apksigner sign`, then `apksigner verify`. Keep that key for future upgrades; Android refuses an in-place update signed by a different key. Existing debug installations use a different key, so migrate deliberately before installing a signed release. Do not commit signing keys or passwords.

Pull requests and pushes to `main` build debug and unsigned release APKs, run Android lint, and upload CI artifacts. The debug APK can be installed for development; its generated debug key is not a stable release signing identity.

See the [guide](guide.md) for installation, service activation, and shell/root permissions. The native [nl2sh](https://github.com/nl2sh/nl2sh) executable has its own release cycle; coordinate interface changes between the projects.
