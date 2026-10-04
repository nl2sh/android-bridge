# nl2sh android-bridge

[简体中文](README.zh-CN.md) | [English](README.md)

面向 [nl2sh](https://github.com/nl2sh/nl2sh) 的 Android API 26+ 无障碍与输入法 companion。包名 `com.nl2sh.bridge`、Binder authority `com.nl2sh.bridge.ops` 保持兼容。

构建要求：JDK 17、Android SDK Platform 35 / Build Tools 35.0.0 和 `ANDROID_HOME`；项目自带 Gradle Wrapper 8.9。

```bash
git clone https://github.com/nl2sh/android-bridge.git
cd android-bridge
./gradlew --no-daemon :app:assembleDebug :app:lintDebug :app:assembleRelease
```

[构建、使用与安全说明](docs/zh/guide.md) · [Release](docs/zh/release.md) · [MIT License](LICENSE)
