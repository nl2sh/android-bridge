# Release

推送 `v*` 语义版本标签触发 `.github/workflows/release.yml`，标签决定 Android 的两个版本字段。使用 JDK 17 与 Android SDK 35 构建并运行 lint。Release 包含已签名的 `nl2sh-android-bridge.apk`、`SHA256SUMS` 与 LICENSE。APK 中的 `assets/nl2sh-runtime.json` 使用与运行应用相同的版本及协议。

签名任务使用现有 GitHub Actions secrets：`KEYSTORE_BASE64`、`STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。仅在 runner 临时目录恢复密钥，使用 `apksigner` 验证 APK，失败时也清理临时密钥。升级必须保留稳定签名身份；Android 拒绝不同密钥签名的覆盖安装。已有 debug 安装需要显式迁移。不得提交密钥或密码。

本地缺少签名配置时可生成未签名 release APK。需要本地签名时，使用被忽略的 `keystore.properties`（`storeFile`、`storePassword`、`keyAlias`、`keyPassword`），或环境变量 `NL2SH_BRIDGE_KEYSTORE_FILE`、`STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。不完整配置会使构建失败。本地可暂缓生产签名；debug APK 使用开发密钥。

原生发布会把此 APK 收录到签名兼容性 Manifest，同时记录资产 SHA-256 和 APK 证书 SHA-256。两者标识不同对象，安装前都必须匹配。发布 companion 不会自动启用无障碍或选择键盘。权限与手动启用方式见[指南](guide.md)。
