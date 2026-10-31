# PaykiTodo 当前源码与验收地图

双端1.17.13，Android405、Windows1.17.13.0、Room34。规划、更新器及全课循环绝对提醒唯一归属已实施，本轮验收、成品和数据保护已完成；当前收尾文档/提交和审计后的源码push。先读[整体指南](MODEL_HANDOFF_20261101.md)、[语法契约](planning-input/INPUT_CONTRACT_20261101.md)和SESSION_HANDOFF。用rg定位符号，不照旧1.17.9入口重建模块或重问过期选择。

## 规划与循环提醒

下列Android文件相对app/src/main/java/com/example/todoalarm/，Windows文件相对desktop/PaykiTodo.Desktop/。

| Android入口 | 当前用途 |
| --- | --- |
| data/TransientPlanningInput.kt | 本地扫描、日期/提醒/循环合同、候选校验与定位诊断；允许循环绝对提醒并验证合法目标 |
| data/PlanningAbsoluteAssignment.kt | assignPlanningAbsoluteReminders全课统一分配；周期跳转后复用实际循环匹配器，支持窗口外目标 |
| data/TransientPlanningRecognition.kt | 本地/可选AI识别，同一候选合同与校验 |
| ui/TransientPlanningDialog.kt | 每日看板临时草稿、编辑预览与归属重算、确认，不保存新文档 |
| data/TodoRepository.kt | importTransientPlanning全课分配后事务导入，additionalDates提前物化远期拥有者；ensureRecurringInstancesAhead检查当前窗口缺口，不从远期最新实例跳过中间课程；1.17.12实例保护保持 |
| data/CalendarSemantics.kt、TodoRecurrence.kt、RecurringTaskTemplate.kt | 时区、循环间隔/范围/锚点、CLAMP和持续滚动；模板不保存单次绝对提醒 |
| data/TodoItem.kt、CalendarEventDraft.kt、BackupManager.kt、AppDatabase.kt、DatabaseMigrations.kt | Room34绝对字段、草稿、备份/跨端合同，已有迁移不能遗漏 |
| sync/TransientPlanningWeb.kt、DesktopSyncCoordinator.kt | LAN临时规划接口与absoluteAssignments派生JSON；编辑复用原session冻结now及账号保护，重校验不写库 |
| ui/PlanningDeskPanel.kt及旧Planning*数据类型 | 旧原文只读与备份兼容；不作为新规划主路径，不删除历史数据 |

| Windows入口 | 当前用途 |
| --- | --- |
| TransientPlanningInput.cs | 本地候选合同及AssignAbsoluteReminders全课唯一归属 |
| DesktopTransientPlanning.cs | AI合同与确认事务导入、远期拥有者物化、父子/课程bundle；内部NONE候选直接使用本次initial |
| NativeWindow.TransientPlanning.cs | 临时输入、编辑预览与实时归属重算、确认入口 |
| DesktopRecurrence.cs | NextPlanningOccurrence和CreateRecurringOccurrence复用实际匹配器；补齐/系列作用域、实例绝对字段保护与触发重算 |
| DesktopSnapshotDocument.cs、DesktopWorkspace.cs、PersonalSnapshotMerge.cs | 本地事务/持久化/跨端合并，保留数据和账号代际保护 |

LAN前端位于app/src/main/assets/desktop-web/，新入口复用transient-planning.js；新前端只读展示旧文档。旧planning/notes等接口仍保留兼容的读写操作，新规划流程不调用它创建文档，不能把新界面只读描述成旧API全部只读。

规则与具体数据/预览流程见[循环绝对提醒实施说明](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。决策014按最后单一推荐方案后用户继续执行，不伪写数字回复。LAN前端修改后200ms重校验，pending/invalid禁用确认，请求前后检查revision/epoch/connectionGeneration丢弃旧回调；派生字段不作为可信输入。

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

Android新增单测PlanningAbsoluteAssignmentTest；设备RecurringPlanningAbsoluteRepositoryTest、RecurringPlanningAbsoluteUiTest、RecurringPlanningAbsoluteWebContractTest。受影响旧合同包括RecurringAbsoluteReminderRepositoryTest、CalendarUpdateDataContractTest、CalendarRepositoryTest、TransientPlanningRepositoryTest、TransientPlanningUiTest、TransientPlanningWebContractTest。Windows针对TransientPlanningTests、RecurrenceScopeTests、CalendarSemanticsTests、真实WPF预览及实际包检查，不重复计数重跑。

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

ADB为C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe。复用emulator-5554/Pixel_8，仅user11执行测试后已停应用/测试、回user10主页，核身份后qemu PID20332恢复原暂停（353线程Wait Suspended）。原user0 646/646和user10 148/148逐文件哈希完全相同，零增删改；user11 after218文件保留备份，前后副本在Backups/PaykiTodo/planning-absolute-1.17.13。不能因ADB shell等待重启/清空，不动真实手机。主Activity完整类名com.example.todoalarm.ui.MainActivity。

## 成品与证据定位

- Android本轮Debug：app/build/outputs/apk/debug/PaykiTodo-1.17.13-debug.apk；签名Release：app/build/outputs/apk/release/PaykiTodo-1.17.13-release.apk。270单测、Lint0错误/137警告/30信息、三种构建、APK405元数据/同1.17.12证书/三类资产字节一致已核；64设备条目63通过/1旧跨端fixture条件跳过，本轮17新增全通过，见[1.17.13验收](VALIDATION_1.17.13.md)。
- WindowsMSI：desktop/installer-output/PaykiTodo-1.17.13-windows-x64.msi；payload：desktop/obj/installer/1.17.13-15bbf1bf1431488690872e90c2ae6327/payload。62项不同检查（60针对+2实际包）及逐文件清单通过，不拿旧desktop/dist代替。
- Deliverables/PaykiTodo/1.17.13共494文件，SHA256.json覆盖其余493；两APK、MSI、484文件完整便携目录、说明与更新资产保持。完整更新ZIP482条目/增量4条目验签和重建匹配，旧1.17.12成品不覆盖，公开Release仍v1.15.18。
- 本轮证据Scratch/PaykiTodo/planning-absolute-1.17.13，备份Backups/PaykiTodo/planning-absolute-1.17.13。LAN模拟HTTP实际DOM9/9与node--check通过，不冒称真实LAN网络；原user0/user10哈希与暂停恢复证据android-evidence-summary.json，所有中途/失败副本保留。
- 规划/更新阶段详见[1.17.10](VALIDATION_1.17.10.md)、[1.17.11](VALIDATION_1.17.11.md)；其证据位于对应阶段Scratch/Backups，旧成功套件不重跑。
- 未推送历史扫描/私有策略：Scratch/PaykiTodo/history-scrub-20261101，最近完整验证after-current-handoff-refresh-verification.json（HEAD d765f14，66提交/1715blob/167已分类URL，零未解决命中）；原始bundle在Backups/PaykiTodo/unpublished-before-scrub-20261101。新增功能/归档/文档提交继续全量复扫，实查远端后只push main，远端HEAD核实一致后才完成原goal。

旧1.17.9源码地图保留在Git历史及Backups/PaykiTodo/handoff-docs-20261101-3e3b5c2，仅作历史定位，不作为当前待实施指南。
