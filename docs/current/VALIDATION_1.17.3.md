# 1.17.3 共享搜索权限与日历显示验收

日期：2026-10-25。Android `1.17.3 / versionCode 395`，Windows `1.17.3.0`，Room `33`。开始提交 `4bed7d1`，前一产品基线为 `c7ab5a8 / 1.17.2`。用户已手动完成第二个专用账号的人机验证和注册；验证码不再是待办项。本轮重建两端安装包，没有推送、公开发布或再次部署服务端。

## 问题与修复

- 共享日历撤权后，旧版 Android 搜索仍显示缓存标题和摘要，打开详情才拒绝访问；Windows 搜索也缺少对应过滤。两端搜索现在按当前账号、服务器和日历访问权限过滤。Android 在权限变化时重新计算已经打开的结果，原始缓存保留，恢复授权后仍可正常显示。
- Android 关闭提醒后，详情不再展示保留的普通、全天或绝对时间规则；启用全天规则时不把暂存的普通偏移一起显示。零偏移文案为“开始时”。本次修正显示，没有把旧规则误显示认定为实际闹钟触发。
- Android 全天搜索使用日程时区的日期及排他结束范围，显示“全天”，避免 UTC 设备将 Asia/Shanghai 的 2026-10-26 显示成前一日 16:00。
- 保留 1.17.2 的个人快照隔离修复，个人内容和设置仍正常同步。

## 原生双账号验收

完整编辑者流程发生在 1.17.2，详见[原生协作记录](NATIVE_COLLABORATION_ACCEPTANCE_20261025.md)：Windows A 邀请 Android B 为编辑者，B 接受；A 创建含 B 参与者的全天日程；B 提交接受回应；手机修改回传电脑，再由电脑修改回传手机，已有回应保留；A 撤权后手机关闭已打开详情。打包 Windows EXE 的已有日程通过全天条目打开、编辑并保存，补齐前一轮实际编辑入口的验收缺口。

1.17.3 使用同一专用身份和保留的数据，完成以下复验：

| 检查 | 实际结果 |
| --- | --- |
| 撤权后升级 | user 10 备份后升级到 1.17.3/395，账号和三条缓存保留；搜索 `1.17.2` 不再显示已撤权内容。 |
| 恢复只读访问 | Windows 所有者重新邀请 B 为只读，Android 正常接受；日历显示双方角色。搜索显示两个匹配日程，主日程摘要为 `2026-10-26 全天`。 |
| 只读详情 | 显示正确日期、日程时区、组织者及已有“已接受”回应，没有关闭提醒的旧规则。取消、修改、删除按钮仍在详情中，对应父控件均为 `enabled=false`。 |
| 打开搜索时撤权 | 手机搜索保持打开、查询不变，Windows 移除 B 后，正常同步使两条结果自动消失；显示“没有找到和‘1.17.2’相关的内容”。没有手动刷新或重新输入查询。 |
| 最终服务状态 | 日历仅剩所有者，三条合成事件保留，主事件最终标题仍为 `Collab 1.17.2 - Windows confirmed`、全天日期 2026-10-26、时区 Asia/Shanghai、B 回应 ACCEPTED；全部提醒关闭。 |

Windows 操作在独立 Win32 桌面进行，通过 UIA 操作实际打包 EXE；未接管用户物理键鼠。手机使用 `emulator-5554 / Pixel_8` 的独立 Android user 10，交互镜像窗口 `PaykiTodo - Pixel_8` 保持可见。其余合成测试日历未操作。没有删除测试账号、日历或恢复旧认证会话。

## 自动验证与安装包

| 检查 | 结果 |
| --- | --- |
| Android 单测 | 42 suites，227 项，0 失败/错误/跳过；新增 5 项覆盖权限变化保留缓存、关闭提醒、全天与普通规则隔离、零偏移及跨日期边界。 |
| Android Lint | 0 errors，133 warnings，30 informational；本轮执行 `:app:testDebugUnitTest :app:lintDebug`。 |
| Android 构建 | Debug 和签名 Release 均重新构建；独立签名检查通过，模拟器已安装并启动 1.17.3/395。没有重跑完整 Android instrumentation 或服务端套件，旧版结果不计为本轮新结果。 |
| Windows 回归 | 安装后实际包配合 57 项回归全部通过，另 1 条含 14 阶段的隔离 UIA 工作流通过。已有原生控件测试扩展了搜索撤权、恢复缓存和服务器来源不匹配断言。 |
| MSI 生命周期 | 安装、真实 1.17.2→1.17.3 升级、修复、payload 哈希、安装后 EXE、卸载及正式用户数据不变均通过；便携 DLL 与 MSI payload 相同。 |

最初 Windows 源码验证为 56 通过、1 个打包入口用例因尚未提供包而跳过；安装后为 57/57，不重复累计测试数。一次编排脚本错误读取 PowerShell 脚本遗留的 `$LASTEXITCODE`，使编排单元报告失败，但 TRX 中测试已通过；之后构建和 MSI 验收独立成功。

Android 最初固定坐标未聚焦搜索框，截图实际是待办页，不是搜索通过证据；后改为按唯一 EditText 定位，使用 `revoked-search-verified` 与 `search-before-live-revoke-verified` 文件。打开搜索条目会导航至日历详情，返回后须重新进入搜索。Windows 个别树查询使用了错误或已经关闭的对话框名称，属于工具定位失败，没有造成额外成员操作；最终撤权以服务端成员表、手机现场结果与持久化角色共同确认。

| 产物 | 仓库内构建路径 | SHA-256 |
| --- | --- | --- |
| Android Debug | `app/build/outputs/apk/debug/PaykiTodo-1.17.3-debug.apk` | `E13599786871A5723D61F2B7C05959DD3DB4C495F3F44FDBED4AED43A42EFC83` |
| Android 签名 Release | `app/build/outputs/apk/release/PaykiTodo-1.17.3-release.apk` | `2D3048E4115B351BA392237273E8BA8A9DD6E6E1A2D0446F7CD98826036E00F3` |
| Windows MSI | `desktop/installer-output/PaykiTodo-1.17.3-windows-x64.msi` | `852BA6A35C0A31AC3F93252BC792A1039BCA3B7BDBEC3B35419BB1B7D36D502D` |
| Windows EXE | `desktop/dist/PaykiTodo.exe` | `9BB7160E59D58A7EB56DBADEB29D51D735B5059A1FAAA7AC237F3E9F79EAB197` |
| Windows 主程序集 | `desktop/dist/PaykiTodo.dll` | `CF012472201DC80E81C800D07691606F280A8200786D0ECC65AF2C89F591F272` |

Debug 为 40,459,206 字节，Release 为 20,873,378 字节，MSI 为 80,102,484 字节。EXE/DLL 版本 1.17.3.0；便携运行需保留整个 dist。三个安装包已复制至 `G:\Workspace\Deliverables\PaykiTodo\1.17.3`，附说明和哈希，副本逐一匹配。旧版本与 1.17.1 的 22 图预览保留原位及原版本标识。公开 Release 仍为 v1.15.18。

## 数据保护与收尾

- A 个人快照 SHA-256 在协作与新版复验期间均为 `A4EBD597562F02198F410F07AEF1292BE6516F19A4B07350EF45D470742020E9`，个人版本保持 24；原 6 条个人记录和设置一致，无待上传个人修改。更早备份的版本 21 与本次协作开始的 24 是不同基线，详见 1.17.2 记录。
- 原 Android user 0 的 646/646 个文件与协作前备份逐项匹配，0 缺失/变化/新增；最终核对副本 `Backups/PaykiTodo/emulator-user0-verified-20261025-235106`，没有恢复覆盖现有会话。
- user 10 最终备份 `Backups/PaykiTodo/emulator-user10-final-1.17.3-20261025-235103`，144 个文件，专用账号身份一致；数据库保留三条共享缓存，启用提醒数为 0，共享角色/模式均为 REVOKED，没有个人事项。
- 专用 Windows 客户端和隔离主机已停止。手机在最终备份后正常重新启动至每日看板，user 0 仍未启动。现有 scrcpy 镜像保留，取消了置顶，不需要再次完成验证码。

## 证据与边界

执行目录：`G:\Workspace\Scratch\PaykiTodo\collaboration-e2e-20261025`。凭据、原始资料和运行诊断不进入 Git。

- 汇总：`native-two-account-result.json`、`final-1.17.3-result.json`、`artifacts-1.17.3.json`。
- 现场：`android-1.17.3-revoked-search-verified`、`android-1.17.3-viewer-search`、`android-1.17.3-viewer-detail`、`android-1.17.3-search-after-live-revoke` 的树和截图；`android-1.17.3-viewer-disabled-controls.json`。
- 数据：`final-viewer-accepted-production.json`、`final-viewer-revoked-production.json`、`final-v1173-desktop-personal-preservation.json`、`android-final-1.17.3-preservation.json`、`original-user0-preservation.json`。
- 构建：`android-checks-1.17.3.txt`、`android-build-1.17.3.txt`、`windows-acceptance-1.17.3.txt`、`windows-publish-1.17.3.txt`、`windows-msi-1.17.3.txt`、`installer-acceptance-1.17.3.txt`。
- Windows 最终安装结果：`installer-1.17.3/install-ff81ec85473c435f808881242caf3497/result.json`；其 `installed-app` 下两个 TRX 为最终计数依据。

本轮完成指定双账号核心协作流程及发现问题的修复验收。手机 OEM 长期后台/锁屏/振动、Windows 物理键鼠全流程、所有权限与离线组合没有因此全部验证。参与者仍限已接受日历成员，组织者为日历所有者；跨成员忙闲排期、独立订阅源管理和会议资源服务仍不作为已完成能力，见[能力审计](CALENDAR_ACCEPTANCE_AUDIT_20261025.md)。
