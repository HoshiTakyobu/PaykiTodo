# Android 1.17.6 备份快照一致性验收

2026-10-28，从 `9e4ff78` 接续用户要求继续检查。Android `1.17.6 / versionCode 398`，Room `33`；Windows 保持 `1.17.5.0`。这次是后台备份并发审查发现的修复，不改变原有界面或备份格式。

## 问题与修复

`TodoRepository.exportSnapshot` 原先依次查询多张表，没有统一的数据库事务。在已读完分组、尚未读取待办时，如果另一项操作原子替换了分组及待办，导出会包含旧分组和新待办，产生无法对应的引用。后台备份、手动导出等直接调用此方法的路径均可能遇到该交错；没有据此声称用户的旧备份已经损坏。

现在由仓库方法自身将多表读取放进一个 Room 事务，保证数据库记录来自同一已提交状态。自动备份仍在应用的 IO 协程里运行，其压缩和写文件位于这次快照读取之外。没有扩大到重新设计备份文件、云服务或 Windows 客户端。

## 复现与验证

- 新的真实 Room 并发测试，在分组和待办查询之间给原子替换机会。旧版明确失败：快照分组 ID 为 1，任务却引用分组 ID 2；修复后通过。测试断言关联内容一致，同时确认后续写入正常完成。
- 第二个新增测试覆盖已有同步事务中的嵌套导出，以及清单父子关系和分组在备份编码/解码后保留，避免给现有云同步调用引入死锁或契约回归。
- `testDebugUnitTest`：229 项通过，0 失败、0 跳过。Lint：0 错误 / 134 警告 / 30 提示。
- Debug、签名 Release、AndroidTest 构建通过。Release v2 签名有效，包名 `com.paykitodo.app`，版本 1.17.6/398；证书指纹与已交付 1.17.5 Release 一致，可按相同签名覆盖升级。
- 模拟器数据检查 24 项通过：2 项快照一致性、QuickChecklistRepositoryTest 和 CalendarUpdateDataContractTest 共 22 项。包含归档关系、共享数据排除与恢复、日历和清单数据契约。
- 账户同步 6 项通过：自动上传、离线恢复、上传/下载期间修改保护、人工冲突确认、安全副本恢复、重新认证和遗漏 dirty 检测等原有路径继续有效。上传挂起时真实完成调用在本次测量为 32 毫秒，5 次主线程心跳通过。
- 联合验收另外通过 Android 1 项和 Windows 1 项：已打开的正式 Windows 1.17.5.0 EXE 自动收到新 Android 的完成状态，目标事项从可见列表移除、另一事项保留，重新读取本地存储仍正确。无需手动同步或重新登录；本机联合测量 2408 毫秒，不作为延迟上限或与上次运行的性能对比。
- 本轮共 31 项针对性 Android 模拟器检查（24 数据 + 6 账户 + 1 联合），另 1 项 Windows 联合检查。未重复运行无变化的 Windows 完整回归/MSI 生命周期；原结果见 [1.17.5 验收](VALIDATION_1.17.5.md)。
- 正常 MainActivity 冷启动复查通过，`am start -W` 返回 `Status: ok / LaunchState: COLD`，安装版本为 1.17.6/398。首次用户切换后的等待命令超时，但随后截图已显示登录页；稳定用户下重新冷启通过。没有把首次超时记成成功，也没有进行生产账号登录或人工验证。

## 数据与结束状态

- 前后备份：`G:\Workspace\Backups\PaykiTodo\before-snapshot-fix-20261028-175530` 和 `after-snapshot-fix-20261028-183727`。以前的交付、备份及失败证据保留。
- 原始模拟器 user 0：646/646 文件哈希相同，无新增或缺失。既有 user 10：148/148 文件哈希相同，无新增或缺失。
- 专用测试 user 11：保留前后完整备份；112 个原文件中 87 个字节一致、25 个发生测试/运行变化，新增 36 个测试及运行文件。本轮数据契约数据库、合成账号和同步副本均只用于该测试用户，不声称测试用户逐字节未变。
- `emulator-5554 / Pixel_8` 无窗口运行；测试应用停止，当前用户恢复为 10 主屏。正式 Windows 测试 EXE 正常关闭，19443/19444 本地夹具服务已停止。
- 证据根：`G:\Workspace\Scratch\PaykiTodo\snapshot-consistency-20261028`。主要文件为 `before-fix-tests.txt`、`after-fix-data-tests.txt`、`final-build.log`、`signature.txt`、`upgrade-signature.txt`、`sync/account-tests.txt`、`sync/android-live.txt`、`sync/windows/live-sync-result.json`、`cold-start-retry.txt` 和 `after-backup.json`。

## 交付

Android 新成品目录：`G:\Workspace\Deliverables\PaykiTodo\1.17.6`。Windows 沿用 `G:\Workspace\Deliverables\PaykiTodo\1.17.5` 的 MSI/便携版，未重建或更改其版本。

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| PaykiTodo-1.17.6-release.apk | 21034478 | `03D6E7F6986A54D7CBF5EEE9957C5D07434133AC86A4871EA46E106C6F55E1F6` |
| PaykiTodo-1.17.6-debug.apk | 40700774 | `ABDB8E9E9531E22F2A02E719D13F76EE6A0CD8776BD2DCE94B054AE2C339AAEF` |

源码输出：`app/build/outputs/apk/release/PaykiTodo-1.17.6-release.apk`、`app/build/outputs/apk/debug/PaykiTodo-1.17.6-debug.apk`。开发和本地交付完成后作中文本地提交，不推送、创建公共标签、发布或部署服务；公开 GitHub Release 仍为 `v1.15.18`。

本轮没有物理手机接入，未覆盖 OEM 长待机、锁屏、振动或生产网络延迟。它们是验证边界，不是本轮未完成的开发步骤。
