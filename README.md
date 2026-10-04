# nl2sh android-bridge

[简体中文](README.zh-CN.md) | [English](README.md)

Optional Android API 26+ Accessibility and keyboard companion for [nl2sh](https://github.com/nl2sh/nl2sh). The package and Binder authority remain `com.nl2sh.bridge` and `com.nl2sh.bridge.ops`.

Build requirements: JDK 17, Android SDK Platform 35 / Build Tools 35.0.0, and `ANDROID_HOME`. Gradle Wrapper 8.9 is included; Python 3 is required for the helper packaging script.

```bash
git clone https://github.com/nl2sh/android-bridge.git
cd android-bridge
./gradlew --no-daemon :app:assembleDebug :app:lintDebug :app:assembleRelease
```

[Guide and safety boundaries](docs/en/guide.md) · [Release](docs/en/release.md) · [MIT License](LICENSE)
