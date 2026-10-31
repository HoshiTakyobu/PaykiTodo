# PaykiTodo 当前源码与验收地图

双端1.17.12，Android404、Room34。规划和更新器已经落地；只有循环文本绝对提醒的规则及对应分支、最终复核/push待完成。先读[整体指南](MODEL_HANDOFF_20261101.md)、[语法契约](planning-input/INPUT_CONTRACT_20261101.md)和SESSION_HANDOFF。用rg定位符号，不照旧1.17.9入口重建已完成模块。

## 规划与循环提醒

下列Android文件相对app/src/main/java/com/example/todoalarm/，Windows文件相对desktop/PaykiTodo.Desktop/。

| Android入口 | 当前用途 |
| --- | --- |
| data/TransientPlanningInput.kt | 已实现本地扫描、日期/提醒/循环合同、候选校验和定位诊断；循环绝对提醒仍拒绝 |
| data/TransientPlanningRecognition.kt | 本地/可选AI识别，同一候选合同与校验 |
| ui/TransientPlanningDialog.kt | 每日看板临时草稿、编辑预览和确认，不保存新文档 |
| data/TodoRepository.kt | importTransientPlanning事务和稳定候选关系；循环生成、转换、系列/整课修改、滚动补齐；1.17.12实例绝对提醒保护 |
| data/CalendarSemantics.kt、TodoRecurrence.kt、RecurringTaskTemplate.kt | 时区、循环间隔/范围/锚点、CLAMP和持续滚动；模板不保存单次绝对提醒 |
| data/TodoItem.kt、CalendarEventDraft.kt、BackupManager.kt、AppDatabase.kt、DatabaseMigrations.kt | Room34绝对字段、草稿、备份/跨端合同，已有迁移不能遗漏 |
| sync/TransientPlanningWeb.kt | LAN临时规划接口，复用手机解析/导入 |
| ui/PlanningDeskPanel.kt及旧Planning*数据类型 | 旧原文只读与备份兼容；不作为新规划主路径，不删除历史数据 |

| Windows入口 | 当前用途 |
| --- | --- |
| TransientPlanningInput.cs | 与手机一致的本地候选合同及校验 |
| DesktopTransientPlanning.cs | AI合同与确认导入、父子关系/课程bundle |
| NativeWindow.TransientPlanning.cs | 临时输入、预览编辑和确认入口 |
| DesktopRecurrence.cs | 补齐/系列作用域；ResetOccurrence清空新实例绝对字段，WithOccurrenceReminders保留目标，RefreshOccurrenceReminders重算触发 |
| DesktopSnapshotDocument.cs、DesktopWorkspace.cs、PersonalSnapshotMerge.cs | 本地事务/持久化/跨端合并，保留数据和账号代际保护 |

LAN前端位于app/src/main/assets/desktop-web/，新入口复用transient-planning.js；新前端只读展示旧文档。旧planning/notes等接口仍保留兼容的读写操作，新规划流程不调用它创建文档，不能把新界面只读描述成旧API全部只读。

选择分支后修改入口与必要验收见[循环绝对提醒实施定位](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。当前实例保护与新文本归属是两个问题，不提前撤销候选校验。

## 已实现的更新、报告与帮助

| 模块 | 入口/合同 |
| --- | --- |
| Android更新 | update/GithubRelease.kt、AppUpdater.kt、ApkUpdateIdentity.kt及ui/AppUpdatePanel.kt；稳定Release、系统下载状态恢复、包名/版本/签名核对及系统安装入口 |
| Windows更新 | DesktopUpdateProtocol.cs、DesktopUpdateStore.cs、DesktopUpdater.cs、NativeWindow.Updates.cs及desktop/PaykiTodo.Launcher；签名/灰度/增量/full回退/nonce和失败回退 |
| 发布工具 | tools/build/New-WindowsUpdateRelease.ps1、Write-WindowsUpdateBaseline.ps1、Build-WindowsInstaller.ps1；协议见WINDOWS_UPDATE_PROTOCOL.md，私钥只在忽略目录 |
| 报告 | Android data/AiReportContext.kt、AiReportPolicy.kt和alarm/AiReportExpiryJobService.kt；Windows DesktopReportContext.cs、DesktopReportPolicy.cs；共用app/src/main/assets/ai/report-system.txt |
| 字体和帮助 | Windows DesktopFonts.cs、NativeUi.cs、NativeWindow.Help.cs；docs/WINDOWS_USER_GUIDE.md嵌入资源，设置/F1离线打开 |

新增行为才补相应回归，不重写这些模块；未来公开Release、真实手机系统安装和OEM后台行为没有被模拟验收证明。

## 测试入口与命令

Android单测TransientPlanningParserTest、CalendarSemanticsTest、TodoRepositoryRecurrenceAlignmentTest；设备RecurringAbsoluteReminderRepositoryTest、TransientPlanningRepositoryTest、CalendarUpdateDataContractTest、TransientPlanningUiTest、TransientPlanningWebContractTest。Windows针对TransientPlanningTests、RecurrenceScopeTests、CalendarSemanticsTests及实际包检查扩展，不重复计数重跑。

在仓库根执行，先核对版本与工作树。下面是必要时使用的命令，不是要求重跑已通过套件：

```powershell
git status --short
git log -1 --oneline --decorate
rg -n 'paykiVersionName|paykiVersionCode' app/build.gradle.kts
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
.\tools\tests\Invoke-DesktopAcceptance.ps1 -Evidence G:\Workspace\Scratch\PaykiTodo\<本轮目录> -PackagedExe <本轮payload/PaykiTodo.exe>
node --check app/src/main/assets/desktop-web/transient-planning.js
```

Gradle包装脚本会选择可用JDK，不硬设不存在的旧JAVA_HOME。Windows测试运行在独立Win32桌面，不开启物理输入模式或操作用户剪贴板。MSI生命周期只在有相关新变化时运行，成品构建成功与真实安装/升级验收分别记录。

ADB为C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe。复用emulator-5554/Pixel_8，最后user10主页、应用/测试停止，qemu PID20332暂停。先核对实际AVD/PID/线程状态再恢复，不能因ADB shell等待盲目重启。user0/user10是原用户，user11专用测试；安装前备份且最终比哈希，不动真实手机。主Activity完整类名为com.example.todoalarm.ui.MainActivity。

## 成品与证据定位

- Android最新Debug：app/build/outputs/apk/debug/PaykiTodo-1.17.12-debug.apk；签名Release：app/build/outputs/apk/release/PaykiTodo-1.17.12-release.apk；同证书及数据保留见[Android验收](VALIDATION_ANDROID_1.17.12.md)。258单测、Lint0错误、42数据库检查；冷启动等待超时与暖启动正常分别记录。
- WindowsMSI：desktop/installer-output/PaykiTodo-1.17.12-windows-x64.msi；payload：desktop/obj/installer/1.17.12-773fce09cffd481888a0003a9a2ecb1d/payload。51项不同检查及484文件便携清单见[Windows验收](VALIDATION_1.17.12.md)，不拿旧desktop/dist代替。
- Deliverables/PaykiTodo/1.17.12包含两APK、MSI、完整Windows-portable、说明和490文件哈希；当前公开Release仍v1.15.18。
- 本轮数据/失败/启动/签名证据：Scratch/PaykiTodo/recurrence-reminders-1.17.12；备份：Backups/PaykiTodo/recurrence-reminders-1.17.12。原user0 646/646、user10 148/148哈希相同，所有中途副本保留。
- 规划/更新阶段详见[1.17.10](VALIDATION_1.17.10.md)、[1.17.11](VALIDATION_1.17.11.md)；其证据位于对应阶段Scratch/Backups，旧成功套件不重跑。
- 未推送历史扫描/私有策略：Scratch/PaykiTodo/history-scrub-20261101，最近完整验证after-android-archive-1.17.12-verification.json（65提交/1710blob/167已分类URL，零未解决命中）；原始bundle保留于Backups/PaykiTodo/unpublished-before-scrub-20261101。新提交要再次复扫，不能把当前树干净等同于整个历史安全。

旧1.17.9源码地图保留在Git历史及Backups/PaykiTodo/handoff-docs-20261101-3e3b5c2，仅作历史定位，不作为当前待实施指南。
