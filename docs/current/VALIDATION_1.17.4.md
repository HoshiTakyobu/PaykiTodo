# 1.17.4 手机操作响应修复验证

日期：2026-10-27。Android `1.17.4 / versionCode 396`，Room `33`。本轮没有修改服务端或 Windows 客户端。

## 修复范围

- Room 本地写入后的 Widget IPC、同步脏标记/调度和共享日历刷新调度改为 `TodoApplication.applicationScope` 后台执行，不再占用完成操作的调用线程。
- 自动备份的快照导出和 SAF 文件写入改到 `Dispatchers.IO`，并用互斥锁串行化重叠备份。
- 云同步仍保留，仍由后台协调器按原有延迟、网络和冲突规则执行。

## 验证结果

| 检查 | 结果 |
| --- | --- |
| Android 单元测试 | `:app:testDebugUnitTest` 通过 |
| Android Lint | `:app:lintDebug` 通过 |
| Debug 构建 | `:app:assembleDebug` 通过 |
| 模拟器安装 | `emulator-5554 / Pixel_8`，安装 `app/build/outputs/apk/debug/PaykiTodo-1.17.4-debug.apk`，versionCode 396 |
| 冷启动 | 进入每日看板，无启动崩溃 |
| 实际操作 | 在待办页创建并完成一条 `latency test`，完成后今日待办立即变为 0；未出现多秒阻塞 |

模拟器当前仍是专用测试用户 10，交互镜像 `PaykiTodo - Pixel_8` 保持可用。此次操作留下了一条用于观察历史记录的合成测试事项；未触碰原始用户 0 数据、生产账号或服务端。

## 产物

- Debug APK：`app/build/outputs/apk/debug/PaykiTodo-1.17.4-debug.apk`
- 本轮未构建签名 Release APK、Windows 安装包或公开发布版本。

## 未覆盖范围

本轮没有重复完整 instrumentation、真机 OEM 长待机/锁屏/振动、Windows 回归或服务端套件；这些范围沿用 1.17.3 的既有验证记录。
