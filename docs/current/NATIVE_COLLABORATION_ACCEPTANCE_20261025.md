# 原生双账号协作验收

日期：2026-10-25。实际流程使用 Android `1.17.2 / 394` 与 Windows 打包 EXE `1.17.2.0`，连接已部署的账号服务。用户手动完成人机验证及第二个专用账号注册；以下检查通过实际客户端操作，没有绕过验证码、修改服务端数据文件或向其他用户发送邀请。

## 已验证的流程

| 步骤 | 实际结果 |
| --- | --- |
| 注册与升级 | 第二账号正常进入应用。独立 Android user 10 在备份后从 1.17.1 升级到 1.17.2，账号会话保留。 |
| Windows 邀请 | 所有者在“共享日历与邀请 → 成员”向第二个专用账号发送可编辑邀请，界面反馈已发送。 |
| Android 接受 | 手机共享管理显示正确邀请，接受后显示可编辑权限、日历所有者及双方成员。 |
| Windows 创建与编辑 | 在已有测试日历创建带第二账号参与者的合成全天日程；通过全天条目实际打开详情与编辑器，再次保存成功。日程时区 Asia/Shanghai、日期 2026-10-26。 |
| 手机回应 | 手机详情提交“接受”；服务器保存 ACCEPTED，Windows 已打开的详情刷新为接受。 |
| 手机修改到电脑 | 手机将标题改为 `Collab 1.17.2 - Android edited`，服务器与 Windows 详情均收到，已有回应保留。 |
| 电脑修改到手机 | Windows 将同一日程改为 `Collab 1.17.2 - Windows confirmed`，手机日历与详情收到，日期和回应保持。手机显示时区为 UTC，详情仍正确显示日程日期 2026-10-26。 |
| 撤权 | Windows 所有者移除第二账号，服务器成员表只保留所有者。手机随后关闭已打开详情，日历从两个变为一个；再次尝试打开旧搜索结果时详情守卫拒绝访问。 |
| 个人数据隔离 | 整个流程所有者个人快照 SHA-256 不变，个人版本保持 24；原 6 条个人记录及设置一致。共享版本依次为 2、3、4、5、6、7，撤权不改事件版本。 |

现有两条合成日程保留，本轮增加一条，最终共三条，提醒全部关闭。创建时切换全天会应用独立全天默认提醒；测试随后在触发时刻之前通过实际编辑器关闭，服务器与 Android 数据库均确认三条记录的 reminderEnabled 为 false，没有触发提醒。

## 发现并进入 1.17.3 修复的问题

- 撤权后的搜索仍显示之前缓存的标题和摘要，虽然打开详情已被拦截。两端搜索应按当前账号、服务器及日历权限过滤结果，同时保留恢复缓存。Android 通过真实场景复现，Windows 源码存在同类缺少过滤的路径。
- Android 已关闭提醒的详情仍显示保留的规则，甚至显示“提前0天”。实际数据库开关保持关闭，不是已证实的错误闹钟触发。
- Android 全天搜索摘要直接格式化设备本地时间，UTC 手机把 Asia/Shanghai 的 2026-10-26 全天日程显示成 2026-10-25 16:00；详情日期正确。

因此，本记录证明指定双账号流程成功，不表示 1.17.2 所有显示与访问边界均无缺陷。上述修复和新版复验单独记录在 1.17.3 验收中。

## 后续 1.17.3 复验

上述三项问题已修复，Debug、签名 Release 和 Windows MSI 已重建。新版重新邀请同一第二账号为只读，确认搜索日期、关闭提醒的详情及禁用修改控件；搜索保持打开时再次撤权，匹配结果自动消失，缓存仍保留。精确测试计数、最终数据核对和安装包见 [VALIDATION_1.17.3.md](VALIDATION_1.17.3.md)。

## 本地证据与数据保护

- 执行目录：`G:\Workspace\Scratch\PaykiTodo\collaboration-e2e-20261025`；汇总 `native-two-account-result.json`，各阶段 `two-account-*-production.json`，原生操作命令与结果、截图均保留。
- 关键截图：`android-invitation-received.png`、`android-invitation-accepted.png`、`android-rsvp-accepted.png`、`windows-android-rsvp-accepted.png`、`windows-received-android-edit.png`、`android-received-windows-edit.png`、`android-access-revoked.png`、`android-revoked-search.png`。
- 第二账号注册备份：`Backups/PaykiTodo/emulator-user10-registered-20261025-231054`（144 文件）。撤权后备份：`emulator-user10-revoked-before-1.17.3-20261025-233551`（145 文件）；其数据库保留三条共享缓存、零启用提醒、共享角色和模式均为 REVOKED，没有个人事项。
- Windows 专用账号个人数据核对：`two-account-final-desktop-personal-preservation.json`。原 Android user 0 保持独立，原始备份与后续核对不恢复覆盖现有会话。
- 测试最初误用少了 `.ui` 的 Android Activity 名称，修正为实际入口后启动成功；Windows 菜单操作曾定位到未打开的窗口，改走实际共享管理入口。两者均为验收脚本问题，没有登记为产品崩溃或功能通过。
- Windows 全程使用独立 Win32 桌面与 UIA，未接管用户物理键鼠。手机通过保留会话的可视镜像操作。凭据、令牌、服务器地址和原始数据不进入 Git。

本次不证明 OEM 锁屏、振动、长期待机或 Windows 所有物理输入行为，也不新增跨成员忙闲排期、订阅源管理和会议资源服务。其余边界见 [日历能力审计](CALENDAR_ACCEPTANCE_AUDIT_20261025.md)。
