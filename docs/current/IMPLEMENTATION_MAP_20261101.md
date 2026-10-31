# PaykiTodo 当前源码与验收地图

## 当前1.17.14公开构建入口

已按既有授权正式发布稳定[v1.17.14](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.14)；Android1.17.14/406、Windows1.17.14.0、Room34。公开构建省略本机默认部署配置；新安装须自行选择有效服务器，已有非空地址继续优先，未存地址的旧安装可能需要重选。7项公开资产与真实下载哈希已核实，Windows范围100%，便携484条目、full 482条目、1.17.13增量4条目验签重建通过。1.17.13功能验收保留为历史，旧七项目标保持complete，本轮没有设备操作或服务器部署。

Android公开开关：app/build.gradle.kts的paykiPublicBuild严格布尔，true时不读取user/cloud.properties；tools/Build-PaykiTodoRelease.ps1的-PublicBuild传给Debug和Release。RemoteSyncCoordinator.cloudServerUrl和MainActivity/AccountSettingsPanel优先已存服务器；AppSettingsStore持久化地址，challenge由选定服务器/v2/auth/challenge提供。

## 1.17.13已验收实现与历史证据

下列历史功能基线为双端1.17.13，Android405、Windows1.17.13.0、Room34。原七项目标的实施、验收、成品、数据保护、文档、审计和main源码推送已完成。先读[整体指南](MODEL_HANDOFF_20261101.md)、[语法契约](planning-input/INPUT_CONTRACT_20261101.md)和SESSION_HANDOFF，核对基线后仅按新的用户任务继续。用rg定位符号，不照旧1.17.9入口重建模块或重问过期选择。

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

## 1.17.13历史成品与证据定位

- Android本轮Debug：app/build/outputs/apk/debug/PaykiTodo-1.17.13-debug.apk；签名Release：app/build/outputs/apk/release/PaykiTodo-1.17.13-release.apk。270单测、Lint0错误/137警告/30信息、三种构建、APK405元数据/同1.17.12证书/三类资产字节一致已核；64设备条目63通过/1旧跨端fixture条件跳过，本轮17新增全通过，见[1.17.13验收](VALIDATION_1.17.13.md)。
- WindowsMSI：desktop/installer-output/PaykiTodo-1.17.13-windows-x64.msi；payload：desktop/obj/installer/1.17.13-15bbf1bf1431488690872e90c2ae6327/payload。62项不同检查（60针对+2实际包）及逐文件清单通过，不拿旧desktop/dist代替。
- Deliverables/PaykiTodo/1.17.13共494文件，SHA256.json覆盖其余493；两APK、MSI、484文件完整便携目录、说明与更新资产保持。完整更新ZIP482条目/增量4条目验签和重建匹配，旧1.17.12成品不覆盖，公开Release仍v1.15.18。
- 本轮证据Scratch/PaykiTodo/planning-absolute-1.17.13，备份Backups/PaykiTodo/planning-absolute-1.17.13。LAN模拟HTTP实际DOM9/9与node--check通过，不冒称真实LAN网络；原user0/user10哈希与暂停恢复证据android-evidence-summary.json，所有中途/失败副本保留。
- 规划/更新阶段详见[1.17.10](VALIDATION_1.17.10.md)、[1.17.11](VALIDATION_1.17.11.md)；其证据位于对应阶段Scratch/Backups，旧成功套件不重跑。
- 历史审计/私有策略：Scratch/PaykiTodo/history-scrub-20261101；功能e014a23与归档f0902eb推送前，按旧远端83ff5cf基线全量审计68提交/1756blob/169已分类URL，零未解决命中与已审地址残留，diffcheck和忽略路径通过。main正常fast-forward至f0902ebe874fa185134d4755682080b08d12e632，实查本地/远端一致；最终文档补记仍从同一旧基线完整复扫并随main推送，未知补记提交SHA不预写。原始bundle在Backups/PaykiTodo/unpublished-before-scrub-20261101，原七项目标已完成。

旧1.17.9源码地图保留在Git历史及Backups/PaykiTodo/handoff-docs-20261101-3e3b5c2，仅作历史定位，不作为当前待实施指南。
