# 1.16.21 实际操作验收与测试范围（2026-10-18）

## 测试到底在测什么

启动测试只回答“能否打开”。功能验收还要回答“操作后是否得到预期结果”：

- 新建后重新打开仍在；取消后不存在；完成进入历史；恢复回到待办。
- 提醒到时进入系统通知栏；点击进入正确页面；完成或延后真的改变任务和闹钟。
- 断网修改保留在磁盘，联网自动补传；上传/下载中继续编辑不能丢数据。
- 冲突操作先显示确认，取消不改数据，恢复后还能找回操作前副本。
- 深色界面可读，控件没有被遮挡，键盘 Enter 行为符合帮助。

单元测试检查规则；集成测试检查数据库/网络/系统服务组合；界面验收检查真实输入和可见结果；日志辅助定位失败。它们不能互相冒充。截图只用于必要的外观和通知检查，不是功能通过的充分条件。

## 本轮基线与安全

- 从 `main / 30c3176` 开始，Android `1.16.21 / 386`、Room `31`、Windows `1.16.21.0`。本轮只有测试和文档改动，没有产品行为修改。
- 使用用户正在运行的 `Pixel_8 / emulator-5554 / Android 14`，没有另建模拟机。产品 Debug APK保持已安装版本，只重新构建/安装 AndroidTest APK。
- Windows 打开真实生产 `NativeWindow` 和 `DesktopReminders`，使用隔离的加密本地数据；不是 HTML 外壳，也不是屏幕外 `RaiseEvent`。本轮没有重新从独立 EXE 启动入口验证。
- 云测试只连接本机受控服务器，Android 使用 `10.0.2.2:19443`，Windows 使用随机 loopback 端口。未使用个人密码，未请求生产账号、推送或发布。
- Android 测试 TLS 例外只在 `androidTest` 中，主机限定 `10.0.2.2`，结束恢复默认。生产 TLS 未改。
- 测试前完整私有数据备份：`G:/Workspace/Backups/PaykiTodo/emulator-before-interactive-20261018/private-data.tar`，SHA-256 `07B521C3EECE09102B39BC004F109553EDB947058E87B769E462E0777EA58CCF`。恢复后原始归档 19 个文件逐个 SHA-256 全部匹配。
- 模拟机保持运行，回到原登录入口。Wi-Fi/移动数据开启，临时精确闹钟/全屏 app-op 回到原默认，18765 转发移除，受控服务器停止。归档覆盖恢复不删除额外测试诊断/安全副本；这些只含合成数据，不能宣称目录逐字节完全相同。

## 功能结果

| 功能 | 真正执行的操作与结果 | 证据类型/范围 |
| --- | --- | --- |
| 手机过期通知 | 实际展开系统通知栏、点击过期通知，打开账号设置并显示重新登录。周期调用不重复发通知、dirty/revision保留另有自动断言。 | A01；系统点击 + 原有可靠性测试。 |
| 可选账号/密码记忆 | 正式登录组件真实键盘输入、点击记住密码自动勾选账号、点击协议及登录；真实本地认证成功；关闭重开填入账号并保留记忆勾选；分别取消密码/账号记忆清除存储。 | S08；正式组件受控宿主，不是整条 MainActivity/生产 Turnstile E2E。加密往返和备份排除为 S01。 |
| 过期/离线后台提醒 | 原有 AlarmManager 回归按 Home 后收到通知；本轮断网过期账号收到通知，实际点击进入提醒页。独立重启后未手动打开应用，闹钟重建并收到系统通知。 | A03/A04/A13；重启精确闹钟为 `window=0`。未精确测量投递延迟。 |
| 提醒动作 | 真实点击我知道了；点击延后10分钟后出现新的精确闹钟；另一轮点击我已完成回到今日待办0，历史显示已完成。 | A04/A14/A15。没有等待完整10分钟二次响铃。 |
| 显式退出与过期不同 | 过期仍离线编辑/提醒；实际退出确认回到登录页。集成验证退出后本机任务仍存在但访问锁定；同账号重登保留revision/dirty。 | A09/S07/W09/W10；不是用清空数据库模拟退出。 |
| 手机自动同步 | 真实 repository 写入触发上传；实际关闭Wi-Fi/数据并制造服务器中断，dirty磁盘保留；恢复网络后不点立即同步自动补传。LAN未启用。 | S02/S03；生产客户端和本地生产服务实现，不是 mock fetch。 |
| 同步期间继续写入 | 受控HTTP上传、下载等待时新增本机任务；旧上传不能清dirty，新任务下一次进入云端；旧下载不能替换新增任务；过时确认拒绝。 | S04/S05/S06；Windows对应18项回归。 |
| 手机云冲突操作 | 正式账号面板真实点击检查云端、取消、再检查确认；取消未替换，确认后云端任务替换；点击恢复操作前副本找回本机任务，再确认保留本机上传并核对远端。 | S09；正式组件受控宿主，设置入口导航另在A01/A06。 |
| Android无标题清单 | 实际输入短横线父子任务和说明，预览后保存，主页显示0/2；相同输入被去重阻止；子任务详情保留说明且无DDL。 | A02/A05/A07；任意深度、注释/备份规则另由单测和Room测试覆盖，不宣称手动点击无穷层级。 |
| LAN无标题清单 | 可见Edge连接手机真实18765服务、输入三层无标题任务/说明/注释、点击保存；真实snapshot含空标题group、任务和说明而无注释；手机主页核对3条；浏览器实际Enter续写。 | L01-L04；ADB转发只替代网络路径，不能证明真实LAN发现/路由/防火墙。旧断连测试仍为模拟响应回归。 |
| Android设置深色UI | 实际打开设置并查看截图：中性蓝灰层次、品牌强调色、文字和控件；不替换看板背景/字体。 | A06；少量人工视觉检查，不是只有日志。 |
| Android普通待办/历史 | 实际编辑创建、确认取消，在历史找到已取消任务。独立提醒完成任务实际点历史恢复，回到已错过待办。 | A08/A15/A16。恢复不把旧DDL改成未来日期。 |
| Android日历/规划台 | 日历时间格双击创建日程，输入保存，日历显示标题和对应时间，点卡片打开详情。规划台新建文档输入、导航离开回来保留、识别预览1条、确认导入后待办页找到任务。 | A10-A12；日程修改保存尝试尚未确认通过，不能据创建通过扩展声称。 |
| Windows原生基本体验 | 实际OS鼠标/Unicode键盘/Enter创建待办、取消草稿、保存无标题父子清单及说明；创建日历日程；规划草稿跨导航保留后保存；点击深色切换并走7个导航入口。 | W01-W08；1160x820可见生产窗口。鼠标点击前校验真实坐标命中目标。 |
| Windows提醒/退出 | 合成账号过期后实际Timer弹出提醒，真实点击完成写入本地；点击退出和系统确认后工作区及重启锁定，磁盘数据保留。 | W09/W10；程序运行条件，不是开机常驻服务。 |
| Windows云冲突 | 点击保留本机上传和系统确认后远端含本机编辑；检查云端并确认恢复后换为云端任务；点击恢复操作前副本找回本机修改。 | W11/W12；真实本地HTTP与生产WPF操作。 |

## 原始结果：不要把步骤数当作测试数

本地证据目录：`G:/Workspace/Scratch/PaykiTodo/interactive-1.16.21/`。失败记录也保留，不只保存成功输出。

- `unit-lint.txt`：186 Android单测通过；Lint 0 error / 128 warning / 28 information。
- `android-original-regression.txt`：原有23项在同一Pixel_8重新运行通过（102.708秒）。部分为组件/系统/数据库回归，不是23条完整用户流程。
- `android-sync-login-conflict-final.txt`：新增3项整组通过（59.778秒），S01-S09。`android-sync-steps-final.txt`保留实际步骤；重复行来自重跑。
- `android-external-steps.txt`：A01-A16实际ADB操作记录，含输入失败后重试。外部fixture的JUnit通过只表示准备成功，不能算功能验收。
- `windows-visible-hit-2.trx`：1条可见窗口流程通过（56秒），覆盖W01-W12。`windows-regression-final.trx`：18项自动回归通过。原18项屏幕外控件测试不是OS鼠标测试。
- `lan-visible-steps.txt`、`lan-synthetic-snapshot.json`：可见Edge与手机真实服务；旧 `desktop-web-connection.cjs` 仍是模拟响应浏览器回归。
- 服务端12项回归通过；新JS/Python/PowerShell语法及AndroidTest构建通过。
- 本轮13张PNG总计1,663,482字节，约1.6MB，磁盘保存，不持续录屏；没有必要每次点击都截图。
- 原有Debug、签名Release和EXE的SHA-256本轮重核未变；产品安装包未重新构建，不以相同版本宣称新产物。路径见 [VALIDATION_1.16.21.md](VALIDATION_1.16.21.md)。

## 失败与边界

1. Android真实输入初次没有形成合法层级，校验阻止保存；修正输入节奏后成功。Windows初期物理输入受焦点/坐标影响，不能用RaiseEvent绕过；加入实际物理坐标命中和前台检查后整组通过。
2. 强行停止后的第一次重启没有提醒；正常打开/Home后重启重新调度。未授予精确权限的重启收到延迟兜底通知。测试授权必须核对UID级权限，并写入系统设置再重启；不是假定命令返回成功等于持久授权。
3. 精确重启轮有Pixel Launcher无响应弹窗，通知送达且闹钟为精确，但不能声称未遮挡全屏抢占通过。关闭无响应Launcher弹窗后提醒页和按钮可用。不是PaykiTodo崩溃证据，也不能据此推断PaykiTodo没有性能问题。
4. 多日闲置、OEM杀进程、真机锁屏全屏/振动、升级和时间改变完整E2E、十分钟二次提醒仍未验证。只证明具体已执行条件，不用一句系统限制替代产品验收。
5. Windows记忆/同账号登录的存储和服务回归不等于完整可见登录/生产Turnstile测试；Windows农历循环创建、OCR、多时段批量创建仍是产品缺口。独立EXE入口、不同DPI/窄窗全流程也未在本轮重新验收。
6. 本轮不是全部平台所有功能全覆盖；未付费调用AI、未测试生产账号或长期多设备压力。不能把“实现轮已提交”改写为“全部需求和平台边界都通过验收”。

## 可重跑入口

必须先备份用户数据，只用合成账号。以下均是主动前台/网络/数据测试，不自动接入CI生产设备：

- `tools/tests/Invoke-AndroidInteractiveAcceptance.ps1` 是ADB操作辅助函数，不是完整自动验收器。`Read-Ui/Click-Text/Type-Text`用于真实输入；`Capture`只在需要视觉证据时调用。读取UI可能包含个人账号，勿将原始输出公开。
- `ExternalUiFixtureTest` 默认跳过，只有 `-e externalFixture true` 才准备合成数据；可用 `-e lanFixture true`、`-e reminderDelayMs 120000`。**它故意不自行恢复，外部操作结束或失败都必须从私有数据备份恢复**；不能无备份在个人设备运行。
- 启动 `python tools/tests/interactive-cloud-fixture.py <Scratch证据目录>` 前，在该目录准备仅供本地使用的 `localhost.crt/key`（含localhost/10.0.2.2证书名）；默认loopback19443，仅此fixture关闭人机验证。之后用ADB `am instrument -w -r -e localCloudFixture true -e class com.example.todoalarm.sync.AccountSyncAcceptanceTest com.paykitodo.app.test/androidx.test.runner.AndroidJUnitRunner`。测试内部恢复快照/偏好，外部仍保留完整恢复备份。
- Windows设置 `PAYKI_VISIBLE_ACCEPTANCE=1` 和 `PAYKI_UI_EVIDENCE=<Scratch目录>` 后，运行 `dotnet test desktop/PaykiTodo.Desktop.Tests --no-restore --filter FullyQualifiedName~VisibleUserAcceptanceTests`。窗口会实际占用鼠标/键盘和剪贴板，期间勿交叉操作；不启用时显示Skip，不冒充通过。
- LAN测试设置相同证据目录与Playwright环境、准备合成LAN fixture和18765转发后运行 `node tools/tests/desktop-web-real-ui.cjs`。仅供ABCD合成token的测试配置，不能对含个人数据的服务直接运行。结束移除转发。
- 恢复仅覆盖已备份的files/shared_prefs/databases，不静默删除额外文件；按归档成员逐个校验SHA-256，不输出凭据。停止本轮测试服务器、恢复网络/app-op，保留用户模拟机运行。
