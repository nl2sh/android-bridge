# 发布

本 Android 项目在本仓库独立版本管理和发布。推送 `v*` 标签触发 `.github/workflows/release.yml`，使用 JDK 17 与 Android SDK 35 构建并运行 lint。Release 包含 `nl2sh-android-bridge-unsigned.apk`、`SHA256SUMS` 和项目许可证。

Release APK **未签名**，安装前必须签名。将稳定的私有签名密钥保存在仓库外，依次使用 Android SDK 的 `zipalign`、`apksigner sign`，最后 `apksigner verify`。以后升级须使用相同密钥；Android 拒绝不同密钥签名的覆盖安装。现有 debug 安装使用不同密钥，安装正式签名版前需显式安排迁移。不得提交签名密钥或密码。

PR 和 `main` 推送构建 debug 与未签名 release APK，运行 Android lint，并上传 CI artifact。debug APK 可用于开发安装，其自动生成的 debug 密钥不作为稳定的正式发布签名。

安装、启用服务与 shell/root 权限见[指南](guide.md)。原生 [nl2sh](https://github.com/nl2sh/nl2sh) 有独立发布周期，接口变化需在两项目之间协调。
