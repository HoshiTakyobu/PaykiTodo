# 1.17.2 Windows 共享编辑与个人同步隔离验收

日期：2026-10-25。Android `1.17.2 / versionCode 394`，Windows `1.17.2.0`，Room `33`。开始基线 `1119f9d`。本轮修复 Windows 同步行为，重新构建两端安装包；Android 仅更新版本信息，没有推送、发布或部署服务端。

## 问题与修复

实际 Windows 原生表单保存共享日程时，普通 `DesktopWorkspace.Edit` 入口无条件标记个人数据待上传，导致多余的个人快照上传。原共享集成测试使用 `EditShared`，没有覆盖原生表单的真实入口。旧版操作前后个人快照摘要变化，但原有 6 条个人记录和设置一致；这是多余上传和版本冲突风险，没有证据表明个人内容泄露。旧备份到操作后的版本 21→24 包含启动同步，不能把三次版本增长全部归因于一次共享保存。

- 普通编辑仅在个人内容或设置实际改变时标记个人同步，保留原有待上传状态；共享内容继续通过独立共享接口同步。
- 个人上传、下载及明确保留本机上传期间，仅共享内容变化不会触发多余个人上传或个人冲突。
- 个人下载应用当前最新共享缓存，避免用请求开始时的旧共享缓存覆盖期间的修改。
- 正常个人轮询、遗漏 dirty 的保护及个人设置同步保留。

## 测试与真实操作

| 检查 | 结果与边界 |
| --- | --- |
| 缺陷回归 | 新测试在修复前失败：预期个人版本 1，实际 2；修复后 14 项同步/共享专项通过。覆盖共享新建、修改、实际个人设置同步，以及个人上传/下载期间共享编辑保留。 |
| Windows 最终回归 | 57 项通过、0 失败/跳过；另 1 条 14 阶段隔离 UIA 流程通过。安装后再次运行相同用例，不计为 114 项，也不把 14 个阶段算作 14 条工作流。 |
| 安装包生命周期 | 真实 1.17.1→1.17.2 升级、安装、修复、payload 哈希、安装后 EXE、卸载和原 Windows 用户资料不变均通过。便携与 MSI 主程序集哈希相同。 |
| 生产专用账号原生操作 | 在隔离桌面运行打包后的 1.17.2 EXE，通过实际创建表单保存一条无提醒、无参与者的合成共享日程。共享版本 1→2，事件 1→2，原事件不变；个人快照 SHA-256 前后一致，个人版本 24→24，原 6 条个人记录及设置一致。 |
| 现有事件编辑范围 | 本轮尝试通过事件行进入详情，但该入口没有 UIA Invoke；定向窗口消息也未打开详情。后续使用原生创建表单验证相同 `Edit` 保存入口。实际打包 EXE 的已有事件编辑未完成，相关修改与并发语义由回归测试覆盖。未使用用户物理键鼠。 |
| Android 构建 | Debug、签名 Release 和 Release 内置 lintVital 构建成功；独立签名校验通过，包名/版本核对为 `com.paykitodo.app / 1.17.2 / 394`。本轮未重复完整 Android 单测、Lint 或设备回归；1.17.1 的 222 项单测和设备结果属于历史验证。 |
| 原用户保护 | Android user 0 的 646 个文件全部保留并逐一匹配原备份 SHA-256，0 新增/缺失/改变；没有恢复旧会话覆盖当前会话。Windows 专用账号原有个人记录及设置保留，安装验证另检查正式用户数据不变。 |

初次新增测试曾因测试屏障只读属性而编译失败，修正测试辅助代码后完成上述验证。旧隔离 Windows 主机达到 30 分钟超时，测试主机终止了自己的进程树；这是测试工具超时，不是产品崩溃。新主机跳过旧命令，避免重复创建，最终在确认保存后停止专用客户端。

## 安装包

| 产物 | 仓库内构建路径 | SHA-256 |
| --- | --- | --- |
| Android Debug | `app/build/outputs/apk/debug/PaykiTodo-1.17.2-debug.apk` | `16312217F72D9C626BECC5C65EDEC44F6286EEDF4C9DB046D242DF94F9B3DF42` |
| Android 签名 Release | `app/build/outputs/apk/release/PaykiTodo-1.17.2-release.apk` | `BA614115934718EC13E4A6321754068E3FA3BE00970B69173B9C67509B2CFDBC` |
| Windows MSI | `desktop/installer-output/PaykiTodo-1.17.2-windows-x64.msi` | `D260A8D8D09EC89EB7F736AD0B987A74606EB5DA4193D0303A19147C69B869D6` |
| Windows EXE | `desktop/dist/PaykiTodo.exe` | `600861A7E9BF9E47FB4BB9654ACE04579AA7BB980152955F7473A140C0738AD8` |
| Windows 主程序集 | `desktop/dist/PaykiTodo.dll` | `9236B5F2C68DB7C25D2098091E01007D8872985436EE6D32B71285F5B3511F4F` |

Debug 为 40,457,610 字节，Release 为 20,873,078 字节，MSI 为 80,090,196 字节。EXE/DLL 版本为 1.17.2.0，便携使用须保留整个 dist。三个安装包已复制到 `G:\Workspace\Deliverables\PaykiTodo\1.17.2`，附说明与哈希，源文件和副本逐一一致。1.17.1 的 22 图预览保留原位，未重标为本轮 Android 验收。

## 证据与待验项

本地证据：`G:\Workspace\Scratch\PaykiTodo\collaboration-e2e-20261025`。

- `regression/shared-personal-before-fix.trx`、`shared-personal-after-fix.trx`；`windows-1.17.2`；`installer-1.17.2/install-0869fc81ddc644369209869c8ee5c551/result.json`。
- `packaged-production-result.json`、`before-fixed-create-production.json`、`after-fixed-create-production.json`、对应个人数据核对结果；个人快照摘要前后均为 `A4EBD597562F02198F410F07AEF1292BE6516F19A4B07350EF45D470742020E9`。
- `original-user0-preservation.json`；原备份 `Backups/PaykiTodo/emulator-before-collaboration-e2e-20261025-204733`，只读复核副本 `emulator-user0-verified-20261025-213948`；Windows 专用资料备份 `desktop-before-collaboration-e2e-20261025-2047`。
- 原始日志、账号、服务器地址和认证材料保留在本地，未进入 Git。未向其他用户发送邀请。

`emulator-5554 / Pixel_8` 当前在隔离 Android user 10 注册页，仍安装 `app/build/outputs/apk/debug/PaykiTodo-1.17.1-debug.apk`（1.17.1/393）。第二个合成账号已填表，但仍显示 **Verify you are human**，等待用户手动验证；不代点或绕过验证码，也没有安装新版覆盖该表单。原 user 0 保持原位且应用停止；user 10、测试日历和两条合成日程均保留。

生产双账号 Android/Windows 邀请、接受、编辑、回应和撤权仍未完成验收。后续先核对验证码与注册状态，只用已准备的专用身份继续。手机 OEM、Windows 物理输入和更广泛的忙闲排期、订阅源、会议资源能力仍遵循 [既有验收边界](CALENDAR_ACCEPTANCE_AUDIT_20261025.md)，不能由此次修复推定全部完成。
