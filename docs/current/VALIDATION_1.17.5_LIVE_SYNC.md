# 1.17.5 已打开 Windows 窗口的同步补验

2026-10-28，接续用户的“继续”。开始提交 `13bb6d4`；应用源码、版本和已交付 APK/MSI/便携 EXE 保持不变，仅增加测试、测试入口和记录。初轮验收见 [VALIDATION_1.17.5.md](VALIDATION_1.17.5.md)。

## 本次结果

新增 1 项 Android instrumentation 和 1 项 Windows 测试，通过同一个本机测试服务器并行运行：

1. Android 专用 user 11 注册合成账号、创建两个真实待办并自动上传。
2. Windows 使用该账号下载初始快照，以独立数据目录启动现有 `desktop/dist/PaykiTodo.exe`，文件版本 `1.17.5.0`。UI Automation 确认两项未完成待办均已显示。
3. 电脑窗口已打开后，Android 调用真实 `TodoViewModel.completeTodo` 完成其中一项。
4. 此后不调用手动同步、不重新登录、不重开窗口。正式 EXE 的正常后台计时器接收并刷新界面，已完成事项从待办列表移除，另一事项仍在。
5. 从独立电脑存储重新读取：目标已完成、另一事项未完成、Dirty=false。EXE 正常关闭，Android 检查云端完成状态通过。

从 Windows 发出就绪信号、触发 Android 完成，到 Windows 显示刷新，共 **5092 毫秒**。此数值包括测试协调、Android 本地完成/自动上传和 Windows 自动接收/界面刷新，是本机联合测试的单次测量，不等同于轮询间隔或生产网络延迟上限。

## 验证与复现

- 新测试：`AccountSyncAcceptanceTest.openedWindowsAutomaticallyReceivesAndroidCompletion` 与 `LiveAndroidSyncTests.OpenPackagedWindowRefreshesAfterAndroidCompletionWithoutManualSync`。
- AndroidTest 构建通过；Windows 测试编译 0 错误、0 警告。最终定向运行两端各 1 项通过；未重复运行初轮已通过的完整回归。
- 累计不同测试为 Android 模拟器 29 项、Windows/跨端/UIA 68 项；初轮 229 个 Android 单测和安装包验收结果沿用，未声称本轮重新执行。
- Android 需 `localCloudFixture=true`、`liveWindowsFixture=true` 并仅选择上述方法；Windows 需 `PAYKI_LIVE_ANDROID_FIXTURE=1`，用 `Invoke-DesktopAcceptance.ps1 -PackagedExe ... -TestFilter FullyQualifiedName~LiveAndroidSyncTests` 启动。两个客户端必须同时运行，只连接本机夹具。
- Windows 通过现有隔离桌面入口启动，不接管日常鼠标、键盘或前台窗口。新增 `-TestFilter` 只运行指定检查，默认完整验收流程不变。

首次测试握手使用分块 HTTP 请求，但夹具的控制接口按 Content-Length 读取，导致请求失败及 Android 等待超时。测试端改为有明确长度的 JSON 请求后通过；未修改应用同步逻辑，也未放宽超时或状态断言。初次失败证据保留。

## 环境与数据

- 仅发现 `emulator-5554 / Pixel_8`，未连接物理手机。本次仍不覆盖 OEM 长待机、锁屏、振动或生产网络。
- 使用专用 user 11；保留测试前备份 `G:\Workspace\Backups\PaykiTodo\before-live-sync-20261028-173459\user-11.tar`，首次运行备份也保留。测试结束后应用停止，模拟器恢复到原 user 10 主屏，仍为无窗口运行。
- 日常 Windows 数据目录全部文件在本次前后哈希一致，无新增/缺失/修改。测试 EXE 已正常退出，本地 19443/19444 测试服务已停止。未操作生产账号或部署服务器。
- 最终证据：`G:\Workspace\Scratch\PaykiTodo\live-sync-20261028\run-2`；关键文件为 `android-live-test.txt`、`windows/windows-regressions.trx`、`windows/live-sync-result.json` 和 `preservation.json`。初次失败记录在其父目录。
- 交付仍为 `G:\Workspace\Deliverables\PaykiTodo\1.17.5` 的既有 APK/MSI/便携版；新增 `同步补验.json` 和本记录副本。没有新应用构建、版本升级、推送或公开发布。
