# 下一模型的源码、验证与运行环境地图

最新Windows1.17.12独立修复在DesktopRecurrence.cs：ResetOccurrence清空新实例的绝对提醒，WithOccurrenceReminders按目标实例保留，RefreshOccurrenceReminders在模板应用后重算。51个不同检查及MSI/484文件便携目录见VALIDATION_1.17.12。Android保持1.17.11/403、Room34；规划绝对循环归属仍待确认，不能借本修复撤销候选校验。

2026-11-01完成60提交历史清理，阶段归档HEAD为83e2a45，更新功能提交092afab，规划功能提交6f8d24d；469个实现/资源/工具/CI文件与验收版本相同。旧编号指清理前历史，见[审计](PUBLIC_HISTORY_AUDIT_20261101.md)。仅待循环绝对提醒归属及对应实现、最终复核和push；下面旧未实现定位不覆盖最新交接。

最新1.17.11更新模块：Android update/GithubRelease.kt、AppUpdater.kt、ApkUpdateIdentity.kt和ui/AppUpdatePanel.kt；Windows DesktopUpdateProtocol/Store/Updater及NativeWindow.Updates；desktop/PaykiTodo.Launcher是独立启动器。tools/build/New-WindowsUpdateRelease.ps1生成签名资产，Write-WindowsUpdateBaseline.ps1进入MSI和便携构建。验收见VALIDATION_1.17.11，协议见WINDOWS_UPDATE_PROTOCOL。下面“更新模块尚无实现”是旧定位。Windows构建可传-WebView2Bootstrapper复用已验证微软签名的缓存，避免重复等待下载。

> 2026-11-01后续实现：1.17.10规划主体已落代码并通过阶段验收，见 [当前交接](SESSION_HANDOFF.md) 和 [验收](VALIDATION_1.17.10.md)。下面“尚未实现”描述是1.17.9交接时的历史定位。新入口为TransientPlanningInput/Recognition、TransientPlanningDialog、TodoRepository.importTransientPlanning，Windows同名新解析/导入/NativeWindow.TransientPlanning，LAN复用TransientPlanningWeb及transient-planning.js。更新模块仍待实现。

配合 [整体指南](MODEL_HANDOFF_20261101.md) 使用。这里只列本轮相关入口，行号可能随实现变化；用 `rg` 定位符号，不全盘扫描工作区。

## 代码基线与可复用能力

1.17.9功能基线为 `87240cd`。没有新的规划语法实现、临时规划编辑器、GitHub检查器、更新下载服务、更新签名密钥或增量包工具。本次在用户要求交接前仅只读查看过更新入口，整体指南中的更新架构是建议。

### 规划：Android

下表路径相对于 `app/src/main/java/com/example/todoalarm/`。

| 入口 | 用途/修改重点 |
| --- | --- |
| `ui/DashboardScreen.kt` | 当前每日看板的 `QuickChecklistActionButton` 与编辑器显示状态；替换入口 |
| `ui/DashboardChrome.kt` | 独立 `PlanningDeskPanel` 路由/导航；取消独立模块时一并检查 |
| `ui/PlanningDeskPanel.kt` | 旧文档选择、新建/保存、识别预览流程；复用适合的编辑控件，不保留新文档持久化 |
| `ui/QuickChecklistUi.kt` | 原快速清单输入与父子任务呈现；入口可移除，已存任务能力要保留 |
| `data/PlanningMarkdownParser.kt` | 旧本地语法；新扫描器应与旧解析边界明确，避免兼容规则覆盖用户已确认新写法 |
| `data/PlanningRecognitionService.kt`、`PlanningAiRecognizer.kt`、`PlanningAiCaller.kt` | 旧本地/AI识别协调、请求和服务选择；新候选必须支持绝对提醒及课程多时段 |
| `data/PlanningImportCandidate.kt` | 旧候选与校验，存在下述已核实冲突，不能直接视作新合同 |
| `data/PlanningNode.kt`、`PlanningNote.kt`、`PlanningLineMapping.kt` | 旧文档/节点/关联数据，仅取消入口不等于可删除这些数据和备份兼容 |
| `data/QuickChecklist.kt`、`TodoItem.kt` | 父子待办、排序、折叠等字段，包含 `parentTodoId`；新导入要保留关系 |
| `data/TodoRepository.kt` | 真实写入、旧文档默认创建、导入、同步快照和循环补齐；查 `createQuickChecklist`、`ensureDefaultPlanningNote`、`ensurePlanningNodeLinkedItems`、`exportSnapshot`、`importSnapshot` |
| `data/CalendarEventMultiSlot.kt` | `buildWeeklyMultiSlotEventDrafts`，已有 `multiSlotBundleId`；当前默认90天结束且重建每周配置，不能原样支持无限期/每2周合同 |
| `data/TodoRecurrence.kt`、`RecurringTaskTemplate.kt`、`CalendarSemantics.kt` | 已有循环、间隔、锚点、月末/闰日策略，可复用但须验证跨时段一致性 |
| `alarm/AlarmScheduler.kt`、`ReminderDispatchTracker.kt` | 实际调度与派发；过去提醒不补响及避免重复提醒，不应只在预览文本“模拟支持” |
| `data/BackupManager.kt`、`BackupArchiveCodec.kt`、`AppDatabase.kt`、`DatabaseMigrations.kt` | 新持久化字段如有需要，必须贯穿Room迁移、备份、跨端读写及完整性校验 |

已经核实的旧候选冲突：

1. `PlanningImportCandidate.validate` 对待办提醒有“必须晚于当前时间”检查，与新语法允许历史时刻冲突。
2. 无DDL且 `reminderEnabled=true` 时拒绝保存；标题-only条目若套默认 `(5,0)`，不能因此变成不可创建。
3. `recurrenceError` 强制 `endDate` 非空，与范围可省略/无限循环冲突。
4. `reminderOffsetsMinutes` 只代表分钟偏移；新语法允许多个绝对提醒，不可把它们简单丢弃、合成一个时点或钳制成0。
5. `buildWeeklyMultiSlotEventDrafts` 缺省 `baseDate + 90天`；新无限循环必须修复真实持久化和补齐逻辑，不能仅改提示文案。

### 规划：Windows与LAN

Windows目录：`desktop/PaykiTodo.Desktop/`。

| 文件 | 用途 |
| --- | --- |
| `NativeWindow.cs`、`NativeWindow.Pages.cs` | 导航、每日看板、搜索及刷新，保留编辑草稿和账号代际保护 |
| `NativeWindow.Planning.cs` | 旧规划文档编辑/保存页面与报告页；规划改造不能误删已完成报告功能 |
| `NativeWindow.Ai.cs`、`DesktopAi.cs` | 旧AI候选识别与预览，必须加入本地识别及相同候选校验 |
| `QuickChecklistParser.cs`、`QuickChecklistEditorDialog.cs` | 旧批量父子待办输入，需整理重复入口 |
| `DesktopSnapshotDocument.cs` | JSON模型与事务候选写入，`DesktopItemEdit`/`SaveItem`/归档编码 |
| `DesktopRecurrence.cs` | `SaveEventSlots`、`SaveRecurringItem`、实例生成和修改范围；保留bundle，补齐整门课管理 |
| `NativeEditors.cs`、`NativeCalendarEditors.cs` | 可复用事项编辑器与循环范围UI；取消不得写入 |
| `DesktopWorkspace.cs`、`PersonalSnapshotMerge.cs` | 本地持久化、编辑代际和同步合并；新字段要兼容，不能绕开现有事务流程 |

LAN页面为 `app/src/main/assets/desktop-web/`。后台在 `sync/DesktopSyncCoordinator.kt`，目前同时存在 `/api/quick-checklists/*` 与 `/api/planning/notes`、`/api/planning/parse`、`/api/planning/import` 等旧路径。新规划应摆脱新建note依赖，保持权限校验和明确确认。修改JS后至少做语法检查及适当浏览器流程验证。

### 更新机制入口

- Android `ui/SettingsPanel.kt` 中 `AboutPanel` 目前只有版本等信息，仍含“本地单机/仅保存在当前设备”的旧文案；加入更新入口时一并对齐现有账号同步事实。`TodoApplication.kt`可管理应用级检查生命周期。尚无 `DownloadManager` 更新逻辑或GitHub Release检查器。
- Windows设置位于 **`NativeWindow.Account.cs` 的 `SettingsPage()`**，不是不存在的 `NativeWindow.Settings.cs`。`Program.cs`现在直接启动WPF，尚无更新启动器/版本切换。
- `.github/workflows/android-ci.yml`目前是Android测试/Debug构建，不是完整双端发布或增量包流水线。
- `tools/build/Build-WindowsInstaller.ps1`能生成自包含payload和MSI及文件哈希清单；当前清单不是自动更新的签名信任机制。
- `tools/Build-PaykiTodoRelease.ps1`构建签名Android Release并核验签名，不发布GitHub Release。真实签名材料在忽略路径，不能抄录。

### 已完成模块，后续保持回归

- 报告：`AiReportContext.kt`、`AiReportPolicy.kt`、`AiReportExpiryJobService.kt`、`DailyReportGenerator.kt`；Windows `DesktopReportContext.cs`、`DesktopReportPolicy.cs`。共同prompt在 `app/src/main/assets/ai/report-system.txt`，Windows嵌入同一资源。
- 字体：`DesktopFonts.cs`、`NativeUi.cs`、设置字体对话框；中文/英文别名和12–22px事务预览已完成。
- 帮助：`NativeWindow.Help.cs`将 `docs/WINDOWS_USER_GUIDE.md` 嵌入，设置/F1离线打开。改说明后要重建Windows资源。

## 验证环境与命令

在仓库根运行。功能变化先按AGENTS规则升版本；把下面的版本、文件名、证据子目录改成当前轮，避免覆盖上轮证据。

```powershell
git status --short
git log -1 --oneline --decorate
rg -n 'paykiVersionName|paykiVersionCode' app/build.gradle.kts

$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain

.\tools\tests\Invoke-DesktopAcceptance.ps1 -Evidence G:\Workspace\Scratch\PaykiTodo\<本轮目录>
node --check app/src/main/assets/desktop-web/app.js

.\tools\build\Build-WindowsInstaller.ps1 -WixPath G:\Workspace\Tools\PaykiTodo\wix\wix.exe
```

`Invoke-DesktopAcceptance.ps1`在独立Win32桌面启动整个测试进程，不注入全局键鼠、不改剪贴板。需要测试打包EXE时用 `-PackagedExe <本轮payload/PaykiTodo.exe>`。不要开启物理输入模式来完成普通验收。WPF有进程级Application/资源线程归属约束；新界面检查优先复用既有原生UI测试线程，避免每个测试新起一个争用静态资源的WPF线程。

`Test-WindowsInstaller.ps1`可验证升级/修复/安装后成品/卸载和数据保留。先读脚本与当前安装状态，不覆盖用户正在用的安装；有相关新变化才重复生命周期验收。不要把“MSI构建成功”写成“安装升级已经实测”。

### Android模拟器：先看状态，不能盲目重启

- ADB：`C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe`。
- AVD为Pixel_8；最后配置为无窗口，停在user10主页，应用与测试包均停止。qemu进程最后观测PID20332，**已按原状态暂停**；PID会变化，必须核对实际进程参数/状态，不能硬编码后直接恢复/暂停别的进程。
- 暂停时 `adb devices` 可能仍显示device，但shell命令会一直等。用进程状态辨别，不因一次观察超时就启动第二个实例或清空AVD。
- user0是需保护的原用户；user10也保留资料；user11为既有独立测试用户。升级安装包会更新共享APK版本；数据库/UI测试仅在独立用户执行，先备份原用户文件并比较哈希。
- 无窗口ADB设备不能作为“用户已经看见交互页面”的证明。需要人操作时先确认可见镜像/窗口；无需人参与的Compose/Room检查可保持隔离。
- 最近完整AVD及各用户前后备份：`G:\Workspace\Backups\PaykiTodo\reports-1.17.9`，含 `Pixel_8-before-restart`、清单与tar。不自动删除或拿旧副本覆盖较新的用户内容。

独立用户测试命令示例（先确认当前连接/用户/备份）：

```powershell
& 'C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 install --user 11 -r app/build/outputs/apk/debug/PaykiTodo-<版本>-debug.apk
& 'C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 install --user 11 -r app/build/outputs/apk/androidTest/debug/PaykiTodo-<版本>-debug-androidTest.apk
& 'C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 shell am instrument --user 11 -w -r -e class <测试类> com.paykitodo.app.test/androidx.test.runner.AndroidJUnitRunner
```

## 现有证据与成品

- [1.17.8验收](VALIDATION_1.17.8.md)：设置、字体、帮助、MSI真实升级/修复/卸载等。
- [1.17.9验收](VALIDATION_1.17.9.md)：242 Android单测、Lint0错误/134警告/30提示、4模拟器检查；75项不同Windows回归及另1条14阶段UIA。两项Android联动夹具未启用，不能写成已通过。
- 1.17.9新MSI已构建，482个payload文件校验通过；本阶段没有再做MSI生命周期，没有签名Release或真实手机升级，没有调用真实AI提供者作输出质量验收。
- 最新已存在APK：`app/build/outputs/apk/debug/PaykiTodo-1.17.9-debug.apk`。交付副本在 `G:\Workspace\Deliverables\PaykiTodo\1.17.9`，包含MSI、完整Windows-portable、使用说明和SHA256.json。
- 1.17.9 Windows真实payload：`desktop/obj/installer/1.17.9-62e3a88bd42b4160a79c1d38aa57d972/payload`。`desktop/dist`仍旧，不能用它代替1.17.9成品做新版本验收。
- 验证日志在 `G:\Workspace\Scratch\PaykiTodo\experience-20261031`，报告阶段以 `reports-` 开头。历史初筛结果也在此，但包含私有复核内容，不得提交或直接打印。

新增功能需要自己的验收证据。已有测试通过意味着原有范围通过，不能借用它证明规划新语法、后台增量更新或历史清理已经完成。
