# 1.17.7 双端同步冲突修复验收

日期：2026-10-29。开始基线 `63fc037`，Android `1.17.6 / 398`、Windows `1.17.5.0`；最终 Android `1.17.7 / versionCode 399`、Windows `1.17.7.0`，Room `33`。本轮为本地修复与用户连接手机的恢复，不推送、不创建 tag、不发布 GitHub Release；公开版本仍是 `v1.15.18`。

## 原因与修复范围

服务器只读记录与手机导出的原始迁移包共同证实：双方账号与设备仍有效、持续访问服务器，但手机积累的延后/自动错过状态，与电脑上传的明确完成/取消不同。旧算法将“本机 dirty 且云端版本更新”一律视为整包冲突，后续自动轮询无法恢复。该问题不是手机必须开启“连接电脑”，也不是服务器没有收到电脑修改。

Android 和 Windows 现在保存已确认的个人快照。Android 基线按服务器、账号与版本关联，使用 AtomicFile 和已有密文格式；Windows 基线随账号状态存入 DPAPI 保护的工作区。基于共同副本进行三方比较：

- 独立记录的改动和不同设置的改动可以合并。
- 待办生命周期与内容/计划部分分开；明确完成/取消可以与另一端的延后、被动错过合并，结束后禁用提醒。
- 两端重复同一种结束动作且时间不同，保留较早动作时间。明确完成与明确取消不会按时间挑选。
- 同任务内容/计划部分被两端不同修改、删除与编辑、新增编号碰撞及受检查的关联破坏继续阻断。并非任意同记录字段都支持独立合并。
- 缺少可信基线的旧分歧继续保护，需要备份、检查后明确恢复或保留本机上传；不因升级自动选择某一份。

合并前保留双方安全副本，合并后保留尚未上传的本地变化，通过服务器版本条件继续上传。真实下载期间编辑仍受保护，诊断日志写入和内容未变的延迟 dirty 回调不再单独触发冲突。自动补齐循环实例安排在确认下载之后。个人快照与共享日历缓存、账号切换与登录保护维持原有边界。服务端加密快照协议无需变更，没有部署服务器代码。

## Android 与跨端验证

| 检查 | 结果与范围 |
| --- | --- |
| `testDebugUnitTest` | 237 通过，0 失败/跳过；新增 8 项三方合并单测 |
| `lintDebug` | 0 errors、134 warnings、30 informational |
| Debug / 签名 Release / AndroidTest 构建 | 通过；最终产品构建后仅调整测试与文档 |
| 账号 instrumentation | 9 个不同用例最终通过，含持久基线/重启、无效 dirty、独立编辑、完成与延后/错过、诊断日志、并发/断网/恢复和重登 |
| Android → 已打开的 Windows | 1 个协调流程通过：任务完成自动反映到既有窗口、保留另一任务、持久化；没有手动同步 |
| 单次本地端到端观测 | 2083 ms，从 Windows 就绪信号经过 Android ViewModel 完成/上传到 Windows 自动可见更新；不是公网或系统后台延迟保证 |
| 正式手机 | 同签名覆盖升级、恢复后导出核验、云端解密比对、自动续传及重启正常；详见下节 |

初次账号 9 项运行有 3 项失败：界面等待/夹具残留、仍按旧整包冲突行为编写的断言、测试服务器注册频率限制。夹具清理补齐模板等关联表；测试核对改为保留双方改动与最新 reviewed snapshot。首次复验中诊断日志用例通过，界面流程和并发断言仍失败；随后界面流程通过，最后单独复验并发/恢复/重登通过。保留所有失败输出，不把重跑相同用例计成新增覆盖。

证据根目录：`G:\Workspace\Scratch\PaykiTodo\sync-conflict-20261029`。账号输出依次在 `sync/account-tests.txt`、`sync-retry/account-tests.txt`、`sync-diagnostics/account-tests.txt`、`sync-final/account-tests.txt`；跨端结果为 `sync-final/windows/live-sync-result.json`。

## Windows 与 MSI 验证

完整安装后测试 TRX：73 项中 71 通过、2 项因测试入口条件跳过，0 失败。另有 1 条 14 阶段隔离 UIA 流程通过；使用实际安装的 1.17.7 EXE。初次 UIA 的退出检查仍硬编码早先已被明确云端删除的历史任务，现比较退出前后的完整数据指纹，针对性复验通过。此前 20 项定向同步测试是完整套件的子集，不另加到总数。

MSI 已验证：真实 1.17.5 安装后升级至 1.17.7、保留所选安装/快捷方式路径、repair、逐文件 payload 哈希、安装后 EXE 回归、隔离 UIA、卸载与日常用户资料哈希不变。测试 profile sentinel 保留。

最终测试安装目录是 `installer/install-6dd7c0cce9954f52a4675152f50c0926`。初次打包入口未找到 WiX，随后使用已有工具编译成功；收尾脚本曾假定卸载项在 HKCU，核查实际 HKLM 产品项、版本与专用快捷方式目标后完成测试卸载。失败日志与测试材料保留，未卸载普通用户安装。

最终证据：该目录 `result.json`、`installed-app/windows-regressions.trx`，以及 `installer-uia-retry/windows-regressions.trx`。MSI payload 与 `desktop/dist` DLL 哈希一致：`3FF1C404DB3EB378AEE141EC07D79B0CA83FD5EA4F6CDDF95648C02A5C715D50`。

## 正式手机恢复与数据核验

用户已授权 USB 调试、打开应用及同签名升级。vivo V2324HA 正式手机通过应用导出原始副本，安装前再次导出：除导出时间外内容完全一致。服务器原始密文同时备份。未卸载手机应用、未清除数据、未使用调试包替换正式版。

逐记录审核恢复候选：保留两端全部 715 条任务、85 份报告和手机设置；仅合并已核对的任务运行状态差异，明确完成/取消优先于被动错过，保留延后时间但关闭结束任务提醒。应用导入后再次导出，比对确认仅有序列化默认值、导出时间及诊断记录的预期变化。

22:00 手机通过“保留本机上传”的版本检查与确认流程将合并结果上传为云端版本 151。只读解密云端核对任务、报告、设置与恢复后手机一致。上传后曾出现手机连接超时；没有修改账号、网络配置或服务器，随后恢复正常。22:02 手机将原有调度产生的新日报和欢迎语索引变化自动上传为版本 152，表明后续正常变化能够继续同步。

报告恢复时是 85 份；既有 `DAYS_90` 保留策略随后在生成日报时清理 2 份过期报告并新增 1 份日报，最终 84 份。逐条核实被清理报告的生成时间均早于原有 90 天阈值，其余报告未变；原始报告仍在备份中。没有把这一正常保留策略误记为合并丢失。

手机重启应用后显示最近同步成功，没有待上传或冲突提示；今日剩余待办为 0。目标三项完成/取消状态正确，全部任务在恢复后没有再变化，未来任务的提醒设置随整表核对保持。最终手机与云端的业务数据及设置完全一致，仅导出时间和诊断日志不同。

另一台活跃 Windows 仍持续访问服务器，但本轮没有读取或操作其实体 UI，不能声称已验证该电脑上的实际呈现或完成升级。新版 Windows 的自动合并与窗口刷新由隔离打包客户端验收覆盖。

私人证据：`phone-recovery-result.json`、`phone-account-after-restart.xml`、`phone-account-final.xml`、`phone-board-final.xml`。备份目录：`G:\Workspace\Backups\PaykiTodo\before-sync-fix-20261029`，含 `phone.paykibak`、`phone-before-install.paykibak`、`phone-after-restore.paykibak`、`phone-after-sync.paykibak` 及恢复前/后的云端密文。个人内容、账号凭据、服务器地址与诊断脚本不进入 Git；不要重放旧恢复包覆盖后续新修改。

## 测试环境与保留

使用已有 `emulator-5554 / Pixel_8`，合成账号同步测试仅在 user 11；没有在正式手机上运行测试夹具。模拟器为 headless，未声称有可见交互窗口。最终 active user 10 Home，应用和测试停止，测试服务器与隔离 Windows 进程结束，测试 MSI 已卸载。

成功前备份 `Backups/PaykiTodo/before-sync-conflict-20261029-212142`，后备份 `after-sync-conflict-20261029-215735`。user 0：646/646 原始文件哈希一致；user 10：148/148 一致。user 11：205 个文件，原始 148 个中 142 个一致、6 个测试/运行文件变化、57 个新增文件，全部保留。此前因其他用户尚未解锁造成的部分备份也保留，没有删除失败产物。

## 交付与哈希

交付目录：`G:\Workspace\Deliverables\PaykiTodo\1.17.7`。包含签名 Release、Debug、MSI、完整 `Windows-portable`、安装说明和校验文件；483 个便携文件逐一与构建目录比对一致。旧版本均保留。本次未生成新的视觉预览。

| 文件 | SHA-256 |
| --- | --- |
| `PaykiTodo-1.17.7-release.apk` | `1FC3A9B8616894B0442C3B654055F281FB40FF11B1DA606901AED9E744229A5F` |
| `PaykiTodo-1.17.7-debug.apk` | `2748DB13B07A7B6C57A3049B475465B179ECB841CBB6E11DF9BC52101262A1EC` |
| `PaykiTodo-1.17.7-windows-x64.msi` | `2A50BC35B576645779259EB77B2C3DEE7839148AD7B20180A7D8BB9103E2BD0C` |
| `Windows-portable/PaykiTodo.exe` | `4E77BD494ED8CE6396C575DF735FE414B0DBBC105DA452A56A506D6918EC00EC` |

正式签名证书 SHA-256：`34E6094BFD8E14D675FCFED2F06FC121500349F9B45D4D0B84F9810399970610`，与 1.17.6 完全相同。项目内 APK 路径为 `app/build/outputs/apk/release/PaykiTodo-1.17.7-release.apk` 和 `app/build/outputs/apk/debug/PaykiTodo-1.17.7-debug.apk`；MSI 为 `desktop/installer-output/PaykiTodo-1.17.7-windows-x64.msi`，便携版来自完整 `desktop/dist`。

边界：未重复完整 Android instrumentation 或服务端套件；未覆盖所有 OEM 锁屏/长时待机/震动及 Windows 物理输入情形。真实手机恢复与自动续传通过，不代表另一实体电脑已升级，也不代表任意离线并发修改都能自动解决。
