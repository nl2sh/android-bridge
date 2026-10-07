# Android Bridge companion

可选 companion APK 为单文件 nl2sh 提供实时 Accessibility 节点、Unicode 输入与手势，不包含 Agent、模型客户端、网络监听或批准接口。

## 构建与启用

主机需要 JDK 17、Android SDK Platform 35 / Build Tools 35.0.0，配置 `ANDROID_HOME`：

```bash
git clone https://github.com/nl2sh/android-bridge.git
cd android-bridge
./gradlew --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

打开设备的 **nl2sh Android Bridge**，进入无障碍设置手动启用服务；侧载应用可能需先在应用信息页允许受限设置。**nl2sh Keyboard** 可在键盘设置中手动选择，唯一按键切回原键盘。两项服务独立，不会由程序代改。

## 权限与目标绑定

`content://com.nl2sh.bridge.ops` 使用 Binder，Manifest DUMP 权限与调用 UID 检查只允许 shell(2000)/root(0)。没有配对密钥或网络端口；普通 Termux UID 不能访问。同步升级 native 程序和 companion。

节点点击核对包名、类名、资源 ID、文字/描述摘要及 bounds；输入核对焦点身份，缺包名或密码字段拒绝。节点文字超过 256 字符时展示截断并携带全文 UTF-16 SHA-256；树达到节点/字节预算标为部分，不能用于证明语义目标唯一。

## Unicode 输入后端

`android.input_text` 在确认前固定后端：可编辑字段用 `ACTION_SET_TEXT`；隐藏 editable 但 `can_write` 为真用剪贴板粘贴；两者都不成立时用已选输入法的 `InputConnection`。输入法核对包名与 field ID。`mode: "replace"` 仅输入法支持，其他后端准备阶段拒绝；提交成功但无法回读的窗口级 WebView 连接报告失败。

`android.swipe` / `android.scroll` 可用 Accessibility 手势并等待完成/取消。companion 在准备前不可用时可选择 shell；批准后不会静默切换后端。截图、应用启动/停止、坐标点击与导航仍用 shell。

## 直接 ADB 输入调试

选中键盘并加载后：

```bash
adb shell am broadcast -a com.nl2sh.bridge.IME_TEXT --es msg '中文'
adb shell am broadcast -a com.nl2sh.bridge.IME_TEXT_B64 --es msg '5Lit5paH'
adb shell am broadcast -a com.nl2sh.bridge.IME_CLEAR
adb shell am broadcast -a com.nl2sh.bridge.IME_TEXT --es mode replace --es msg '替换整段'
```

接收器由输入法运行时注册，发送方需 DUMP，仅 shell/root 可调用。这是显式本地调试，权限等同 adb shell，**不经过 nl2sh 确认链**。Base64 支持标准和 URL-safe 字母表。关闭键盘只影响 IME，关闭无障碍只影响 Accessibility；批准后服务消失则动作失败。

## 独立排查伴侣状态

确认已安装 `com.nl2sh.bridge`，打开应用查看服务与键盘状态。启用无障碍与选择键盘是独立操作：树、节点点击、手势需要前者，IME 提交需要后者。即使两者均启用，普通应用 UID 也不能调用 provider。

Provider 权限错误需要改用 shell/root 调用方，不应扩大导出权限。树不可用时检查服务；部分树需重新取得完整快照才能作语义选择。目标过期或包名/bounds 不一致时重读当前 UI，重新准备动作并审批。手势或输入报失败不证明界面完全未变；决定重试前再次读取屏幕。

原生工具目录是 Agent 的公开接口。Binder provider 与 shell 广播是原生运行时及显式调试的兼容接口，不是远程审批端点，不应增加网络监听来暴露它们。接口变更必须同时核对原生工具准备代码与伴侣执行校验。

## Capability 协议 v2

shell/root 可调用 `content call --uri content://com.nl2sh.bridge.ops --method capabilities`，
即使两个服务都未启用也可查询。base64url 编码的 `data` 回复包含 `protocol_version: 2`、
`app_version`、`build`（版本代码）、`installed_at`、独立的 `services.accessibility` 与
`services.ime`、当前可用能力 ID 和实际限额（250 个树节点、65536 回复字节、4096 输入字节；
ACTION_SET_TEXT 保留原有更小限额）。服务运行不表示已有焦点编辑器或完整节点树；执行时仍需校验目标身份。

新客户端通过 `--method invoke --extra payload_b64:s:<base64url>` 发送无 padding 的 UTF-8 JSON：
`{"protocol":2,"request_id":"unique-id","method":"capabilities","args":{}}`。
回复为 `{"protocol":2,"request_id":"unique-id","ok":true,"result":{...}}`；失败则返回
`ok:false` 和 `error`。编码请求最多 32768 字节，请求 ID 最多 128 字符。编码错误、协议不支持、
非对象参数及递归 invoke 在分派前拒绝。保留旧的逐方法 extras，以兼容旧原生客户端。
查询与 invoke 都保持 DUMP 和 shell/root 限制，不提供审批接口。

### 协议回归验证

在模拟器安装 Debug APK 后执行 `python3 scripts/protocol-smoke.py --serial emulator-5554`
（可用 `--adb` 指定 ADB）。检查只读能力查询、服务能力标记、v2 请求关联、非法请求及普通应用 UID
拒绝。分别在两个服务关闭、仅无障碍、仅输入法和都开启时执行。API 26 的四种组合通过，
Debug/Release 构建与 lint 通过。

诊断页面显示应用版本、协议版本、无障碍启用/运行状态、键盘启用/选中/运行状态、Provider
注册状态与当前能力。返回页面或点击“刷新诊断”可刷新，长内容支持滚动。Provider 已注册不代表
普通应用有调用权限；兼容性按协议说明，不把示例版本号当成最低版本要求。

API 26 诊断页面验证覆盖竖屏展示及窄横屏滚动至底部输入框；Debug/Release 构建与 lint 通过。
