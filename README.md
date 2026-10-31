# PaykiTodo

[![Release](https://img.shields.io/github/v/release/HoshiTakyobu/PaykiTodo)](https://github.com/HoshiTakyobu/PaykiTodo/releases/latest)
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/HoshiTakyobu/PaykiTodo/actions/workflows/android-ci.yml)

把待办、日程和提醒放在一起，在 Android 和 Windows 上安排每一天。

PaykiTodo 为事项保留本地副本，支持日历、循环安排、倒数日、批量规划输入、备份和账号同步。AI 识别与报告是可选功能；手动编辑和本地规划识别不需要 AI API。

## 下载与版本

**[打开 GitHub Releases](https://github.com/HoshiTakyobu/PaykiTodo/releases/latest)**，查看已发布的安装包和更新说明。

| 版本 | 当前状态 |
| --- | --- |
| 已公开发布 | v1.15.18，Android Release APK |
| 当前开发源码 | Android 1.17.12 / versionCode 404；Windows 1.17.12.0 |
| 本轮开发范围 | 临时规划、设置、字体、报告与应用内更新已实现；未推送历史已清理，循环绝对提醒规则和最终推送仍待完成 |

当前源码包含发布版之后的增量，不能把源码版本当作已有公开安装包。[更新日志](CHANGELOG.md)记录各版本变化。

- Android：8.0 及以上。正式使用选择签名 Release APK；Debug 包与 Release 包签名不同，不能直接互相覆盖安装。
- Windows：Windows 10 1809+ / Windows 11 x64，提供 MSI 安装版及完整便携目录。便携版不能只复制单个 EXE。账号验证使用 Microsoft Edge WebView2；安装包包含其安装入口。

在“设置 → 关于”检查稳定版本。Android Release 支持后台下载、校验签名后交给系统安装；Debug 显示签名差异和发布页。Windows 自动检查并按发布灰度后台准备更新，手动下载可提前参与；更新包校验完成后下次启动切换，启动失败会回退。没有签名更新资产的旧发布仍通过发布页面安装。[更新资产维护说明](docs/current/WINDOWS_UPDATE_PROTOCOL.md)介绍发布流程。

## 先用起来

1. 首次启动时选择服务提供者给出的登录服务器，注册或登录账号。首次登录需要联网；登录后保留本地数据，临时断网仍可查看、编辑和提醒。
2. 在每日看板查看今天的待办和日程。没有固定时段的事情建成待办；需要安排起止时间的事情建成日程。
3. 编辑标题、时间、地点与提醒，点击“完成”保存。取消编辑会放弃草稿。
4. Android 按设置中的提示开启通知、精确闹钟等权限；Windows 保持客户端运行，以便收到桌面提醒。
5. 在“设置 → 数据与备份”导出一份 .paykibak 迁移包。它可以在手机和电脑恢复，应保存在你信任的位置。

## 能做什么

| 功能 | 使用方式 |
| --- | --- |
| 每日看板 | 汇总今日待办、剩余日程和近期目标 |
| 待办 | 可选截止时间、分组、提醒、循环、完成与历史恢复 |
| 日历 | 日/周/月/列表视图，多日历、时区、全天事件、循环和多时段安排 |
| 规划输入 | 每日看板输入文本，本地规则或可选 AI 识别，修改预览后确认导入；支持父子待办与多时段循环课程，不保存新文档 |
| 提醒 | 提前或到点提醒；Android 提供通知与全屏提醒，Windows 提供桌面提醒 |
| 倒数日 | 关注目标日期，完成后可保留纪念 |
| 账号同步 | Windows 与 Android 各自保存本地副本，联网后传输加密快照，保护真正的编辑冲突 |
| 共享日历 | 使用支持相应接口的服务器管理成员、权限与日程回应 |
| AI 报告 | 两端共用提示规则，结合前后七天的相关记录；日报生成满 168 小时过期，周报按设置保留 |
| 备份 | 导出/恢复迁移包，关键恢复操作前保留安全副本 |

## 使用说明

- **Windows**：[完整使用说明](docs/WINDOWS_USER_GUIDE.md)。客户端“设置 → 常规 → 使用说明”或 F1 可离线打开同一份内容。
- **Android**：设置中的内置使用说明覆盖主要流程和系统权限。
- **字体**：Windows“设置 → 常规 → 选择字体”支持中文、英文名称查找，12—22 px 字号与实时预览；点击完成才应用。
- **快捷键**：Ctrl+F 搜索，Ctrl+N 新增事项，F1 打开帮助，Esc 取消日历拖动。
- **安装细节**：[Windows 安装说明](desktop/installer/README.md)。

### 电脑客户端与“连接手机”有什么区别？

Windows 客户端有独立的本地数据库副本，通过账号服务器同步，不要求手机持续在线。

手机的“连接电脑”是另外保留的局域网浏览器入口。浏览器直接操作手机数据，手机必须保持连接并开启该服务。它不是 Windows 客户端的账号服务器。

### 数据会一直留在本机吗？

日程与待办保存在本地，支持导出和恢复。账号同步传输个人数据的加密快照；共享日历使用单独的权限接口。明确退出账号会锁定工作区，但不会清空已有本地数据。

同步不是备份的替代品。删除、恢复和冲突处理前应核对范围与副本；详细数据行为见[隐私政策](PRIVACY.md)。

### 提醒为什么可能没有弹出？

Android 通知权限、精确闹钟权限、电池管理、锁屏策略及不同厂商系统都会影响送达。Windows 关机、休眠或退出程序期间无法按时弹出提醒。先检查对应设备的权限和运行状态；重要事项应保留其他确认方式。

## 从源码构建

Android 使用 JDK 17（可使用 Android Studio 的 JBR）、Android SDK 34 和项目 Gradle Wrapper：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 位于 app/build/outputs/apk/debug/。签名配置从 keystore.properties.example 建立本地副本；真实密钥和密码不能进入 Git。

Windows 使用 .NET 9 SDK；MSI 另需 WiX 4.0.6 和相应 UI 扩展：

```powershell
dotnet build desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj
pwsh -File tools/build/Build-WindowsInstaller.ps1 -WixPath <wix.exe路径>
```

开发与隔离验证说明见 [desktop/README.md](desktop/README.md)、[贡献指南](CONTRIBUTING.md)。服务端部署示例位于 server/ 和 docs/templates/，请自行配置账号服务，勿提交私有部署信息。

## 反馈、贡献与许可

报告问题请附版本、系统、复现步骤和脱敏后的错误提示。[支持说明](SUPPORT.md)与[安全问题报告方式](SECURITY.md)提供对应渠道。

项目由维护者提出需求、审阅与验收，AI 工具辅助实现和验证。欢迎通过 issue 或 pull request 提供可复现的问题、修复与文档改进。

代码采用 [MIT License](LICENSE)。字体和其他第三方资源分别遵守原许可，详见 [NOTICE.md](NOTICE.md)。
