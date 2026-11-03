# Current Task

## 2026-11-03：10 项体验修复的本地实现、验收与交付完成

用户要求继续的 261103-2 已从 main / 50c217e 增量完成 R1–R10，Android 1.17.15/407、Windows 1.17.15.0、Room 34。实现、失败修正、实际 APK/MSI/完整便携目录、帮助校准和交付哈希已核对，详见 [真实验收](VALIDATION_UX_1.17.15.md)。273 Android 单测、12 模拟器检查、Lint 0 错误；Windows 139 通过/0 失败/2 同期跨端网络 fixture 跳过，共享 Chromium 7 项通过。没有待重新实施或重复已成功套件的本轮必项；提交以实际 git log 为准。

推荐边界按 UX_IMPLEMENTATION_HANDOFF_20261103.md 2.1 推进，没有冒称逐项确认。多 DPI 跨屏、生产账号与真实设备边界保留为未验证；原规划 Prompt 保留且不混入提交。公开版本仍 v1.17.14，本轮不包含公开发布、push、服务器部署或真实设备安装。下方文档轮与旧发布记录仅为历史，后续只按新的用户指令继续。

## 2026-11-03：10 项体验问题的实施文档已交付，产品尚未修改

用户明确要求本轮只理解需求并编写供另一模型执行的详细文档。已从本地 `main / 50c217e`、Android `1.17.14 / 406`、Windows `1.17.14.0` 核对直接相关代码与现有资料，完成 [实施交接](UX_IMPLEMENTATION_HANDOFF_20261103.md)、[验收设计](UX_ACCEPTANCE_20261103.md) 和 [实施启动提示词](START_UX_IMPLEMENTATION_20261103.txt)。

待后续实施的范围是关闭到托盘、选色色块、日程三行标题/时间/地点、现有双色样式完善、双端 Markdown 报告、双端 HTML 帮助、Windows 字体显示调查、按周列表、矢量下拉箭头，以及看板待办/日程混排。推荐边界与排序规则见交接文档 2.1；不是已实施或已逐项获用户确认的功能。

本轮没有产品代码、版本、数据库、构建、设备或服务器操作，没有 commit/push/发布。当前交接任务已完成；后续模型收到实施指令后再按最新仓库执行。下面 v1.17.14 发布及更早记录是历史已完成工作，不重新驱动。

## 已完成：v1.17.14正式发布与100%Windows更新资产

已按既有授权正式发布稳定[v1.17.14](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.14)；Android1.17.14/406、Windows1.17.14.0、Room34。公开构建省略本机默认部署配置；新安装须自行选择有效服务器，已有非空地址继续优先，未存地址的旧安装可能需要重选。7项公开资产与真实下载哈希已核实，Windows范围100%，便携484条目、full 482条目、1.17.13增量4条目验签重建通过。1.17.13功能验收保留为历史，旧七项目标保持complete，本轮没有设备操作或服务器部署。

正式Release、资产/下载和两端生产更新解析器终验已完成，见 [发布核对](PUBLIC_RELEASE_1.17.14.md)。当前没有剩余开发或发布任务；最终文档提交与main一致性以实际Git核对记录为准。旧七项目标不重新执行，后续仅响应新的用户任务。

## 历史准备：安全公开构建1.17.14启动

用户已在明确单一正式发布/100%Windows候选后要求继续，发布授权持续有效。预检发现1.17.13 APK DEX携带本机私有默认地址，未创建/上传Release；须移除而不能直接公开。公开模式跳过本地cloud.properties、保留已存服务器与现有选择/人机验证流程。按项目新安装包版本规则默认准备1.17.14/406及Windows1.17.14.0，Room34；可选版本偏好已发出，用户有新指令时优先采用。

正在完成Android公开构建及隐私复扫、服务器选择/已存配置回归、Windows新MSI/便携/签名更新包与实际包检查。原1.17.13成品及私有配置保留；保持已授权100%范围。正式发布前先核齐完整草稿资产，发布后核真实下载和更新识别，再同步公开文档/审计推送。当前公开Release仍v1.15.18，旧七项目标不重启。

下方候选与旧任务段是发布前历史记录，不作为重新等待旧发布范围的指令。

## 后续：1.17.13本地发布材料准备完成

旧七项目标与源码push均已完成，用户再要求继续但尚未指定新开发或公开Release范围。本轮准备了[发布候选](RELEASE_CANDIDATE_1.17.13.md)，材料与审核在Scratch/PaykiTodo/release-preparation-1.17.13。APK/MSI保持已验收原成品，另打484条目便携ZIP，核对旧公开APK同证书/包名及更新资产。

现有Windows签名清单为100%自动更新范围；实际公开发布为新步骤，等待用户明确。不以未回复或时间经过视为发布批准。未创建tag/Release、上传资产或改变线上灰度；公开Release仍v1.15.18，旧goal不重新启动。

## 2026-11-01 原七项目标已完成：1.17.13验收、交付与审计后推送

首次GitHub CI的无签名配置空路径错误已由61ae359修正；[实际CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/runs/41243078175)单测、Debug构建及空白检查全部通过。独立公开副本Debug配置成功，无配置Release仍明确拒绝；见[公开构建验收](VALIDATION_PUBLIC_BUILD_20261101.md)。未改变已验收产品行为或重建本地1.17.13成品。

原七项产品需求已实施，循环绝对提醒不再等待选择。按决策014，本轮沿最后单一推荐方案后用户继续执行全课Start>=absolute最近实例唯一归属，相等本次、并列输入顺序；相对每次，窗口外提前物化，中间补齐不跳过，有限范围无目标整批拒绝，不复制或自动重新分配。具体实现见[说明](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。

Android1.17.13/405、Windows1.17.13.0、Room34。270单测、Lint0错误/137警告/30信息、三种Android构建通过；64设备条目63通过、1旧跨端fixture条件跳过，本轮17新增全部通过。Windows62不同检查、LAN模拟HTTP实际DOM9/9和node--check通过。APK元数据/Release同证书/资产与源码、494交付文件及完整/增量更新验签重建已核，原user0 646/646和user10 148/148哈希相同、测试停止/user10主页/原暂停恢复，详见[1.17.13验收](VALIDATION_1.17.13.md)。

本轮产品、文档、验收、成品、数据保护、中文功能提交e014a23和独立目标归档f0902eb均已完成。推送前按旧远端83ff5cf基线全量审计68提交/1756blob/169已分类URL，零未解决命中与已审地址残留；main正常fast-forward至f0902ebe874fa185134d4755682080b08d12e632并实查本地/远端一致，原七项目标已完成。最终推送结果文档补记仍从同一旧基线完整复扫并随main推送。公开Release仍v1.15.18，没有tag/Release/上传APK/部署。

原未跟踪历史Prompt、恢复bundle、认证、私钥、旧成品与失败证据保持。仅user11合成测试，最终已停止应用/测试、回user10主页并恢复原暂停；真实手机和普通Windows资料未动。后续会话先核基线，仅按新的用户任务执行；不重新驱动已完成的本轮目标、不重复无新变化的成功套件、不重问已确认语法。

下方是历史轮次；其待定项、旧版本及下一步只描述当时状态，不作为当前任务清单。

## 历史阶段：2026-11-01 双端单次提醒复制缺陷已修复

Android1.17.12/404也已完成单次转循环、全部/后续/整课编辑及远期实例的提醒保护，258单测、Lint0错误、42项数据库检查通过，Debug及同证书Release交付，见[Android验收](VALIDATION_ANDROID_1.17.12.md)。Windows1.17.12.0原51项检查和484文件MSI/便携成品保持，Room34。该缺陷不依赖新规划语法选择，不能据此声称循环文本绝对提醒归属已确定。

下一步仍需要用户选择循环绝对提醒的一次归属或仅允许提前分钟；随后实施所选分支、审计新增提交并执行最终授权push。历史审计和已验证模块不重做，原目标未完成。

循环绝对提醒的双端具体入口、窗口外实例和编辑/补齐风险已只读核对，见[实施定位](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。两种分支仍待用户选择；本文中的建议不能取代用户确认。

## 历史阶段：2026-11-01 历史清理完成，等待循环绝对提醒归属并准备最终推送

60个未推送提交已保留阶段历史完成脱敏，接回main的归档HEAD为83e2a45；全部新快照/消息和127对变化blob通过复核，完整基线差异空白检查通过。最终469个实现/资源/工具/CI文件与已验收1.17.11相同，没有新APK，详情见[历史审计](PUBLIC_HISTORY_AUDIT_20261101.md)。原始bundle、私有证据及未跟踪历史Prompt保持。

循环课程的绝对提醒仍待最终答复：只提醒一次并归属该绝对时刻之后最近一次课程，或循环只允许提前分钟、绝对提醒另建单次事项。未答复不能自动选推荐项；当前定位报错保留原文。完成相关实现与验收后再审计新增提交、复核远端并按既有授权push main。原目标未完成，没有push/Release/部署，不重做已通过的1.17.8—1.17.11模块。

## 历史阶段：2026-11-01 更新阶段完成，继续原七项目标

1.17.11/403、Windows1.17.11.0、Room34已实现两端GitHub检查、手机持久下载和APK身份校验、Windows后台/灰度/实际文件增量及启动切换回退。最终验证与边界见[VALIDATION_1.17.11](VALIDATION_1.17.11.md)，成品在Deliverables/PaykiTodo/1.17.11。测试仅使用独立资料和模拟发布，没有真实手机升级或公开新Release。

接下来完成循环绝对提醒归属（本轮再次提供简短选择，等待答复），在恢复副本保护下清理全部未推送提交的内容与消息、复核最终文档，然后执行已授权源码推送。历史初筛及恢复材料保持私有。不得把当前树干净当作历史清理完成，不推送含原始私有历史的恢复引用。原goal保持未完成，用户恢复执行指令持续有效。

## 历史阶段：2026-11-01 临时规划主体已实施，原七项目标继续

Android1.17.10/402、Windows1.17.10.0、Room34；起点为a25aac6及前模型未提交规划代码。三端每日看板临时输入、本地/可选AI、可编辑预览、确认事务导入、父子与课程多时段、旧文档含归档只读、绝对提醒备份同步和无截止滚动已实施。254Android单测/Lint、12项设备检查、Windows90回归+1独立14阶段UIA和5段LAN模拟HTTP浏览器流程通过；成品含Debug、同证书签名Release、MSI和完整便携目录，见Deliverables/PaykiTodo/1.17.10及VALIDATION_1.17.10。原user0 646/646、user10 148/148哈希相同。

循环绝对时刻的一次归属仍待用户答复，当前报错保留原文，不复制到所有实例。更新器、全部未推送历史审计与最终授权push仍未做；原goal未完成。旧的“规划尚未实现”交接段落是历史。不要重复从头实现已验收解析器或重问已确认语法。

下一步：完成循环绝对提醒归属选择、实现双端GitHub更新与Windows真实后台/灰度/增量链路，随后清理全部未公开历史并执行已授权推送。整体任务不暂停、不标记完成。

## 历史阶段：2026-11-01 恢复原七项实施目标

用户明确要求按启动提示词和三份配套文档继续原任务，撤销此前仅为更换模型的暂停请求。接手核对HEAD a25aac6、Android1.17.9/401、Windows1.17.9.0、Room33；原未提交文档整理说明和历史Prompt保留。规划输入实现进行中，源码暂升1.17.10/402、Windows1.17.10.0、Room34用于新绝对提醒字段；尚无本轮构建验收成品，不能把新增代码算作完成。#备注顺序和跨午夜缺省已确认并记录决策012/013。更新机制、完整历史审计和最终授权推送仍待完成，整体goal未完成。

## 历史阶段：2026-11-01 用户交接指令

本轮只整理交接文档，用户明确要求交接完成后暂停自动目标以更换模型。整体工作未完成。下一模型获得恢复指令后从 [整体实施指南](MODEL_HANDOFF_20261101.md) 开始，不重做已验证的设置/字体/报告阶段；下方原七项范围继续有效。

## 历史阶段：进行中：2026-10-31 双端体验与公开发布

执行 [完整清单](EXPERIENCE_UPDATE_20261031.md)，保留全部7项需求及用户追加的循环/每周多时段规划语法要求。

1.17.9阶段完成公共报告prompt、前后日程/系列/签到上下文、日报168小时过期及读取/恢复/同步防复活，见 [验收](VALIDATION_1.17.9.md)。1.17.8设置、字体和帮助已完成。规划语法见 [决策](planning-input/p2a-session/DECISIONS.md)，#备注与分号顺序、跨午夜缺省仍待回复，整体规划流程尚未改造。

下一步实现每日看板统一输入/可编辑预览/确认入库，推进双端GitHub更新及桌面后台/灰度增量机制。最后核对文档，处理全部未推送历史中的私有信息与第三方参考残留，再执行已授权推送。整体目标未完成，不应标记complete。

## 历史阶段：Completed: 1.17.7 / 399 双端同步冲突恢复，2026-10-29

接续指定会话 `01a187ab-3592-7cf1-a762-9b389ebf4fa7`，从 `63fc037` 修复真实手机反复陷入整包冲突的问题。双方持续联网，但手机延后/自动错过与电脑明确完成/取消被整包冲突锁住。双端现保存已确认快照，自动合并独立改动和可判定的任务状态，继续保护真正冲突；无内容变化的 dirty 与诊断日志不再造成误报。

Android 1.17.7/399、Windows 1.17.7.0 已构建并完成针对性验证。真实 Release 手机已同签名升级，原始副本备份后完成合并恢复、上传、自动续传和重启核验。715 条任务保持；报告恢复时为85份，随后既有90天策略清理2份过期报告并自动生成1份日报，手机/云端84份一致，原报告在备份内保留。未改服务器代码或另一台实体电脑的本地副本。

237 Android单测、9项账号设备用例及1项跨端实时流程、71 Windows回归和1条14阶段UIA、MSI生命周期/用户数据保留检查通过。初次失败、复验与边界见 [VALIDATION_1.17.7.md](VALIDATION_1.17.7.md)。安装包和说明在 `Deliverables/PaykiTodo/1.17.7`；本地中文提交，不推送或发布。此轮无剩余开发/恢复步骤，其他电脑需使用新版安装包。下方为历史任务。

## 历史阶段：Completed: Android 1.17.6 / 398 后台备份快照一致性

从 `9e4ff78` 接续并发审查，已用真实 Room 测试复现并修复旧分组与新待办混合导出的问题。快照多表读取现在使用同一事务，兼容已有同步事务，自动备份压缩/文件写入仍在后台。

229 单测、Lint、31 项针对性模拟器检查及 1 项正式 Windows 兼容检查通过；Debug/签名 Release/AndroidTest 构建、签名与升级证书校验、正常冷启动均完成。新 Android APK 在 `Deliverables/PaykiTodo/1.17.6`，Windows 沿用 1.17.5；原 user 0/10 全部文件哈希一致。见 [VALIDATION_1.17.6.md](VALIDATION_1.17.6.md)。本地中文提交，不推送或公开发布。下方为历史轮次。

## 历史阶段：Completed: 1.17.5 / 397 双端体验与后台同步

2026-10-28 继续补验：正式 Windows EXE 已打开时，能自动接收手机完成操作、更新可见列表并持久化，联合本地测量 5092 毫秒；两端各新增 1 项定向检查通过。只改测试与记录，原应用版本和交付包不变，详见[实时窗口补验](VALIDATION_1.17.5_LIVE_SYNC.md)。

[2026-10-28 的 12 项反馈](REQUEST_2026-10-28.md)已完成，开始基线 `344bcdd`。双端图标、固定桌面字体、卡片和日历样式、快速清单时间、手机响应及跨端完成状态均已实施并验证。

Debug/签名 Release/MSI/便携成品及 18 图预览已交付至 `G:\Workspace\Deliverables\PaykiTodo\1.17.5`。229 Android 单测、28 项不同模拟器检查、67 项不同 Windows/桥接/UIA 检查及 MSI 生命周期通过；失败后的定向复测、数据保留和实际边界见 [VALIDATION_1.17.5.md](VALIDATION_1.17.5.md)。按项目规则作中文本地提交，不推送或公开发布。下方为历史轮次。

## 历史阶段：Completed: Windows 1.17.4.0 multi-computer session recovery

The Windows client previously reused `DeviceId` from the encrypted workspace during login. Copying `%LOCALAPPDATA%\PaykiTodo` to another computer therefore caused the second login to replace the first computer's session, even though the cloud service supports multiple devices. Windows now persists a separate installation identity, the server allocates a new device when a reused ID has a different machine name/platform, and authentication failures retain server error codes. Safe diagnostics record endpoint host, status, error code, token expiry timestamps and hashed device identity without credentials or token values.

Validation: 60 desktop tests (57 passed, 3 skipped) and 13 server tests pass. The Windows `1.17.4.0` self-contained MSI still needs to be rebuilt and installed separately; no production server deployment was performed in this source round.

## 历史阶段：Completed: 1.17.3 / 395 search access and Android calendar presentation

Manual CAPTCHA and second-account registration are complete. The dedicated 1.17.2 native Android/Windows workflow passed editor invitation, acceptance, RSVP, bidirectional existing-event edits and revocation. Inspection exposed revoked titles in search, dormant reminder rules in details and incorrect all-day search dates; 1.17.3 fixes these issues. The same identities passed viewer acceptance, correct details/search and automatic removal from already-open Android search after revocation.

Passed: 227 Android units, Lint 0 errors/133 warnings, rebuilt Debug/signed Release/signature checks, 57 Windows regressions, one 14-stage isolated UIA flow and MSI lifecycle with real 1.17.2 upgrade. Latest packages, verified copies and notes are in `Deliverables/PaykiTodo/1.17.3`. See [validation](VALIDATION_1.17.3.md) and [native collaboration](NATIVE_COLLABORATION_ACCEPTANCE_20261025.md).

Original Android user 0 has 646/646 unchanged files. User 10 retains its dedicated identity and three revoked shared cache rows, all reminders off. A's original 6 personal records/settings and collaboration-start snapshot/revision 24 are unchanged. Dedicated Windows host/client stopped; interactive `PaykiTodo - Pixel_8` mirror remains available on user 10, installed Debug 1.17.3/395. No pending human verification, push, public release or new deployment.

This authorized implementation and acceptance round is complete. OEM long-idle/lock-screen/vibration, full Windows physical-input acceptance and wider availability/subscription/meeting-resource services remain explicit boundaries in [the audit](CALENDAR_ACCEPTANCE_AUDIT_20261025.md); do not start expanding scope or rerunning successful suites solely on another continuation.

## 历史阶段：Previous: 1.17.2 shared-save fix and 1.17.1 audit

1.17.2 fixed unnecessary personal uploads from normal shared-event saves and preserved concurrent shared edits during personal downloads. Its initial single-account packaged create test and artifacts remain in [VALIDATION_1.17.2.md](VALIDATION_1.17.2.md). The original audit's two-account and packaged existing-event gaps were subsequently resolved by the native flow above. The emulator visibility issue was corrected by attaching scrcpy to the existing headless AVD without losing the registration form; the user then completed verification.

## 历史阶段：Completed: existing server shared-calendar rollout, 2026-10-25

The user's requested whole-task completion includes production deployment. The existing account server now runs the current cloud and calendar modules; four additive tables preserve the original accounts, devices, sessions and personal snapshots. Remote and local recovery copies exist. Linux 21-test suite, public dedicated-account calendar read/write/CAS/authentication checks and unchanged personal snapshot checks passed. See [rollout evidence and scope](PRODUCTION_CALENDAR_ROLLOUT_20261025.md). No new client build; Android remains 1.17.1/393, Windows 1.17.1.0. Native two-account/window/OEM workflows were not rerun in this rollout.

## 历史阶段：Completed locally: 1.17.1 / 393 participant controls and complete details

Both clients implement accepted-member participant selection, organizer/participant response details, current-account RSVP initialization/refresh and server-authorized PRIVATE projections. Ordinary cancel writes nothing; calendar changes reset selection, edits preserve existing response authority, new occurrences start unanswered and independent copies drop invitations. Confirmed response and pending/in-flight edits are both retained safely during sync conflicts. Source access and all-day notification dates are rechecked.

Debug/signed Release APK, self-contained Windows output, Chinese MSI and22 selected actual-control images exist. Passed222 Android units,12 data plus18 distinct UI tests,55 Windows regressions plus one 14-stage isolated UIA flow,21 server tests and MSI lifecycle/data-preservation checks. See [VALIDATION_1.17.1.md](VALIDATION_1.17.1.md). That local build round did not deploy production; the subsequent requested rollout is recorded above. Source and client artifacts remain frozen.

## 历史阶段：Completed locally: 1.17.0 / 392 calendar and account update

Implement the [calendar update contract](CALENDAR_UPDATE_CONTRACT.md) on Android and native Windows. Improve the calendar body, create/adjust workflows, multiple calendars, recurrence, all-day reminders, details/copy, event/display timezones and calendar collaboration. Keep current single strong-reminder delivery and queue; opening a reminder must resolve the complete current item detail.

The normal login surface must show a named server selection action without exposing its domain. Server management remains available in Settings and through the login server selector, including expired-login and changed-server recovery. Improve account action layout with compact grouped controls.

Update versioned data/backup compatibility together with both clients. Preserve existing user records, credentials and local-first operation. Use independent fixtures for schema, recurrence, reminders, timezone and collaboration verification; keep Android device results, isolated Windows results and real server results distinct.

Android `1.17.0 / versionCode 392`, Windows `1.17.0.0`, Room `33` are built and locally verified. New Debug/signed Release APK, self-contained EXE, Chinese MSI and actual component screenshots exist. Validation: 216 Android units, 25 distinct emulator tests, 51 Windows regressions plus one 14-stage isolated UIA flow, 21 server tests and MSI install/upgrade/repair/uninstall/data preservation. See [VALIDATION_1.17.0.md](VALIDATION_1.17.0.md) for exact scope and paths. Sharing still requires a server update; no push, public release or production deployment was made.

## 历史阶段：Completed: 1.16.26 / 391 日历复核与 Windows 安装包

按[REQUEST_2026-10-23_REAUDIT.md](REQUEST_2026-10-23_REAUDIT.md)重新审计原始反馈，补齐循环作用域/日期锚点、全天/跨日/多时段、日历颜色及窗口交互、Windows本机OCR和手机纪念询问；中文MSI已完成实际安装、升级、修复、启动、卸载及数据保留验证。

当前Android `1.16.26 / 391`、Windows `1.16.26.0`、Room32，Debug/签名Release/MSI及便携目录均已构建。193 Android单测、13设备用例、38 Windows回归及1条14阶段隔离控件流程通过。逐项证据和明确边界见[VALIDATION_1.16.26.md](VALIDATION_1.16.26.md)。本轮本地交付已完成，不推送发布；办公室实际环境、手机OEM与物理拖动全链路不被本机测试替代。下方为历史完成记录。

## 历史阶段：Completed: 1.16.25 / 390 两端体验反馈

按照 [REQUEST_2026-10-23.md](REQUEST_2026-10-23.md) 完成独立多日历、两步网格创建、字体自定义、倒数日保留规则与完成询问、重复待办折叠、设置重排、Android 名称纠正及相关两端逻辑。开始基线为 `6f3800f / 1.16.24 / 389`，下方是历史已完成轮次，不能替代本轮验收。

最终实现与验证见 [VALIDATION_1.16.25.md](VALIDATION_1.16.25.md)。新版Debug/签名Release/Windows成品均已构建，原模拟器业务数据保留。按项目规则本地中文提交，不推送或发布。

## 历史阶段：Completed: 1.16.24 / 389 字体、日历与真实客户端验收

需求见 [REQUEST_2026-10-22_TYPOGRAPHY.md](REQUEST_2026-10-22_TYPOGRAPHY.md)。从 6005a10 恢复未提交修改，按最终批注完成无衬线标题/正文/导航/用户名、手机抽屉加粗、Windows 搜索和账号顺序、一体清单图标、参考日历主体与蓝色交互。清刻本悦宋仅用于问候；补修 Windows 辅助功能完成/恢复持久化，以及提醒/日历深色对比度。

最终 Debug/签名 Release/Windows 已重建，Android 190 单测及 Lint、Windows 22 项回归与一条 14 阶段 UIA 流程、正式 EXE 启动与搜索检查通过。专门测试账号完成 Windows 创建、手机修改回传、Windows 完成到手机历史、两端到点提醒及手机完成回传。原始手机 557 个文件已恢复并逐项匹配，78 个额外运行文件保留，测试数据另存备份。证据、失败记录和准确范围见 [VALIDATION_1.16.24.md](VALIDATION_1.16.24.md)。

本轮要求的本地实现和验收已收尾；不推送或发布。真机 OEM 长待机/锁屏/振动、物理键鼠全链路和既有 Windows 功能差异仍是后续边界，不作为本轮已通过事项。

## 历史阶段：Completed: 2026-10-22 七项修复 / 1.16.23

- 上一完成轮次为 main / 8bbb136、1.16.22 / 387；恢复会话时已有未提交的 1.16.23 / 388 修改。当前 Room31 不变，需求见 [REQUEST_2026-10-22.md](REQUEST_2026-10-22.md)。
- 自动续传传输期间的新写入，核对遗漏的本机变化，阻止旧副本差异在后续自动轮询中绕过确认；独立副本完成状态传播、断网恢复和冲突副本保留已验证。
- 已登记近 10 分钟提醒恢复且不重复；过去的 DDL、日程与自定义提醒可保存，新增历史事项不补响。
- 三端/Wiki 内置授权中文字体，修复 Windows 分组显示、任务排版与重复账号入口；Android 中文名称、待办快捷方式和深链接已实现。
- 190 Android 单测、32 个设备用例分批复验、22 Windows 回归、1 条14阶段隔离控件流程、LAN 网页回归通过；Lint 0 errors/131 warnings。Debug/签名 Release/EXE 已构建并核对版本、哈希、签名；详见 [VALIDATION_1.16.23.md](VALIDATION_1.16.23.md)。
- 原 Pixel_8 数据恢复后 531/531 文件匹配，26 个新增运行文件保留；本次接续只读复核和文档收尾，不再重建安装包或改写用户数据。文档与代码按项目规则中文提交，不推送、发布或修改生产。
- 剩余边界：真机 OEM 长待机/锁屏/振动、厂商语音识别、Windows 完整真实账号验证与既有功能差异。没有替用户决定真实冲突数据取舍；手机较新而电脑仍旧时按 [同步恢复说明](SYNC_RECOVERY_1.16.23.md)检查。

## 历史阶段：Completed: 1.16.22 / 387 Windows 排版与不干扰测试 (2026-10-19)

- 从 main / 429f6da、1.16.21 / 386、Room31 开始；上一轮实际操作验收已提交。
- 统一原生 WPF 导航、字体层次、控件对齐、编辑流程、各页面与弹窗；保留原看板图片、问候字体和品牌色。公共弹窗和托盘必须使用 PaykiTodo 原图标。
- 默认测试不注入全局鼠标/按键、不修改剪贴板、不切换用户桌面。通过独立 Win32 桌面上的真实 WPF 控件自动化接口验证；物理输入验收仍单独明确 opt-in，不混称。
- 升级 Android 1.16.22 / 387 与 Windows 1.16.22.0，构建 Debug、签名 Release 和独立 EXE；验证保存/取消、导航草稿、清单、主题、提醒、同步安全回归和实际渲染。
- 不推送、发布、修改生产服务器；保持显式退出锁定、过期离线提醒、revision/dirty 与传输期间新写入安全规则。
- 已完成上述改动、Debug/签名 Release/Windows 构建和本地验证：19 Windows 回归（含成品启动）、1 项14阶段 UIA 流程、186 Android 单测、Lint、签名验证、原 Pixel_8 升级启动。完整证据、复现命令和边界见 [VALIDATION_1.16.22.md](VALIDATION_1.16.22.md)。
- 修复实际渲染发现的日期/时间文字裁切和窄窗口规划按钮越界，增加可视区域断言；账号、AI 和隐私弹窗也覆盖排版/取消检查。不是仅换配色，也不宣称手机所有功能已在 Windows 对齐。
- 默认不再接管用户系统输入；物理命中/键盘全链路需另行安排。保留已有 OEM、Windows 农历/OCR/多时段创建及账号验证 E2E 的明确缺口，不算作已通过。

## 历史阶段：Previous: 1.16.21 / 386 implementation and scoped interactive acceptance (2026-10-18)

The implementation round for [REQUEST_2026-10-17.md](REQUEST_2026-10-17.md) landed at main / 30c3176 from 856e714, Room 31. This is not a claim of complete cross-platform parity or all-scenario acceptance. Expired accounts retain offline editing/reminders and dirty data; explicit logout locks local access. Both clients provide encrypted credential memory and confirmed conflict recovery. Android adds persisted network sync jobs and notifications, cross-client checklists allow no title, settings use neutral surfaces, and native WPF fixes drafts, layout and calendar overlaps.

- Verified outputs: Debug and signed Release APKs at `app/build/outputs/apk/{debug,release}/PaykiTodo-1.16.21-*.apk`; Windows `desktop/dist/PaykiTodo.exe` / `1.16.21.0`.
- Passed: 186 Android unit tests, Lint, 23 emulator instrumentation tests, 18 Windows tests, 12 server tests and LAN browser regression. See [VALIDATION_1.16.21.md](VALIDATION_1.16.21.md) for actual paths, hashes and coverage.
- Next acceptance boundary: physical-device OEM long-idle/lock-screen behavior. No multi-day idle, physical vibration or vendor battery-policy evidence is claimed. Windows lunar/OCR/multi-slot creation remains incomplete; existing special fields are preserved. Desktop reminders require the program to run.
- No push, tag, publication or production server changes.

### User-requested actual operation audit

- Reused the user's Pixel_8 / emulator-5554. Actually tapped notification/account/settings/checklist/todo/history/calendar/planning/reminder controls. New formal login/account panels received real taps and keyboard input against a local server; actual Wi-Fi/data outages and request-time edits were tested.
- Re-ran 186 unit tests/Lint, original23 Android tests, 3 new Android acceptance tests, 18 Windows regressions and a 12-stage visible native OS pointer/keyboard flow. Visible Edge saved a titleless hierarchy into the actual phone LAN service, not mocked fetch routes.
- Scope, failures and remaining boundaries: [INTERACTIVE_VALIDATION_1.16.21.md](INTERACTIVE_VALIDATION_1.16.21.md). No fresh product APK/EXE; only AndroidTest rebuilt. Original emulator files restored with19/19 SHA-256 matches, network/temporary permissions restored and test server stopped.
- Remaining: OEM long-idle/lock-screen, timed redelivery, upgrade/time-change full E2E, Windows login/EXE entry full E2E and documented parity gaps. Android calendar modification save was not confirmed; creation/detail were. Do not describe these as passed.

## 历史阶段：v1.16.20 Completed Work (2026-09-23)

User request resumed from session `01a05c72-896d-7831-afd4-0a2dc093ccec`:

1. Restore phone Daily Board agenda geometry from the supplied screenshot: left date strip, readable event rows, tomorrow below; preserve artwork.
2. Close all LAN Web editors and discard drafts when the phone disconnects or access expires.
3. Replace raw fetch errors with useful Chinese connection guidance.
4. Replace the Windows HTML workspace with a native desktop UI, retaining independent local storage and account sync.

That completed round used source version `1.16.20 / 385`; the public GitHub Release remains `v1.15.18`.

- Phone agenda implementation matches the supplied reference geometry and the dedicated `DailyBoardReferenceTest` passed on `emulator-5554` / `Pixel_8`.
- LAN regression passed all 8 modal types, canceled promises, stale response, disconnect, and reconnect errors; network failures show Chinese guidance.
- Windows WPF workspace, local-first account lifecycle, reminder behavior, special-field preservation, AI-key stripping, native editor interaction, and real local-server integration all passed 12 tests.
- Android Debug/AndroidTest/Release assembly, 185 unit tests, Lint, JavaScript syntax, LAN browser regression, Windows tests, and self-contained publish passed.
- This round is complete locally. No push, tag, or GitHub Release was performed; existing public release remains `v1.15.18`.

### v1.16.20 Known Limits

- Windows does not yet reproduce every Android-only surface such as OEM lock-screen behavior, lunar recurrence editing/generation, OCR/image capture, all advanced settings, or multi-slot editor details.
- Windows reminders require the client to remain running; physical-device Android reminder and widget behavior still needs OEM-specific testing.
- AI calls were tested with controlled parsing and local test servers, not with a paid provider account.

## 历史阶段：Previous Active Focus

The `v1.16.19 / versionCode 384` round closes the remaining cross-client history restoration gap from the `v1.16.18` quick-checklist validation. LAN Web and the independent Windows client now expose restoration for ordinary historical todos and quick-checklist subtrees. This remains a local validation round and does not publish a GitHub Release.

## 历史阶段：v1.16.19 Checklist

- [x] Administrator timestamps are emitted as epoch values and formatted in the visitor's browser timezone.
- [x] The quick checklist parser handles a title, arbitrary dash-depth nesting, visible `--#` notes, validation, and Enter prefix continuation.
- [x] Daily Board and Todo expose the quick checklist entry and real-time preview on Android.
- [x] Confirmed input creates real hierarchical TodoItems, never a title row or note-only row.
- [x] Parent completion excludes canceled children, does not revive canceled ancestors, and updates ancestors recursively.
- [x] Complete, cancel, delete, history, backup, cloud snapshot, LAN Web, Windows, and Widget paths carry the checklist fields.
- [x] Room `30 -> 31` migration and exported schema are present.
- [x] Parsing diagnostics locate the source line on Android, LAN Web, and Windows.
- [x] Widget rows expose checklist completion state and progress without replaying old completed items.
- [x] Windows complete/cancel operations preserve terminal states and fresh local data creates a valid default group.
- [x] LAN Web and Windows expose historical todo restore/delete actions, including quick-checklist subtree restoration.
- [x] Turnstile token polling is lifecycle-bound and cleaned up when its WebView is released.
- [x] Focused parser/repository/migration/backup/server/Windows tests and emulator instrumentation pass.
- [x] Documentation, final build verification, sensitive-data scan, and focused commit are complete.

## 历史阶段：v1.16.19 Verification Evidence

1. `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest`, `node --check`, `dotnet test`, `dotnet build`, and the 12-test server suite passed during implementation.
2. Room migration metadata covers database `31`, and backup compatibility tests cover older archives without checklist fields.
3. `emulator-5554` / `Pixel_8` passed all 18 instrumentation tests, including a real Room `30 -> 31` migration.
4. Final verification after the `1.16.19 / 384` metadata update: 185 Android unit tests, 18 emulator instrumentation tests, Android Lint, Debug/AndroidTest/Release assembly, 12 server tests, JavaScript syntax, 5 Windows tests, and Windows self-contained publish all passed. Release APK v2 signature verification passed with the expected RSA-2048 signer.
5. The current Debug APK was installed on `emulator-5554` / Android 14, package `com.paykitodo.app` reported `versionName=1.16.19` and `versionCode=384`, and the app launched successfully. Physical-device OEM reminder/widget behavior remains outside this round.

## 历史阶段：Completed `v1.16.14` Scope

1. Allowed Cloudflare's required `about:blank` and `about:srcdoc` internal documents in the Android Turnstile WebView.
2. Added restrictive CSP entries for the Turnstile iframe, internal documents, and the Cloudflare Insights script injected by the private proxy.
3. Suppressed duplicate delivery of one Turnstile token and kept AndroidView callbacks connected to the current Compose state after recomposition.
4. Raised Android and Windows application metadata to `1.16.14 / versionCode 379`.

## 历史阶段：Validation For `v1.16.14`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed.
2. `:app:testDebugUnitTest` and `:app:lintDebug` passed; Debug and Release APKs were assembled.
3. Azure service restarted successfully; public `/healthz` returned `200`, and the public challenge CSP contains the required `about:blank` / `about:srcdoc` entries.
4. The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the Turnstile widget. A final successful token assertion remains pending on a physical device because Cloudflare challenges are intermittent in this emulator; the observed 401 is the documented Private Access Token probe.

## 历史阶段：Completed `v1.16.13` Scope

1. Added the existing Turnstile production hostname to the widget without replacing the owner's widget or removing its local development hosts.
2. Retrieved and validated the existing Secret without displaying or committing it, then stored it in Azure `/etc/paykitodo/cloud.env`.
3. Added server-side hostname allowlist validation in addition to `success=true` and `action=account_auth`.
4. Added an Android WebView response-field fallback for WebViews that render Cloudflare success but omit the parent callback.
5. Raised Android and Windows application metadata to `1.16.13 / versionCode 378`.

## 历史阶段：Validation For `v1.16.13`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed.
2. `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
3. Public `/healthz` returned `200`; `/v2/auth/challenge` returned a real Turnstile widget; a dummy registration token was rejected with `403 human_verification_failed`.
4. The final rebuilt APK was not installed on the emulator because `emulator-5554` disconnected; the earlier emulator session displayed Cloudflare `Success!` before this callback fallback was added.

## 历史阶段：Completed `v1.16.12` Scope

1. Normal reminder acknowledgement, completion, notification retention, and stale-alarm rejection no longer erase the dispatch ledger for the current trigger.
2. Reminder ledger reset remains limited to deliberate todo or event rescheduling, schedule rebuilding, item replacement, or deletion across both Android and LAN Desktop Web writes.
3. Android uses the notification full-screen intent as the only primary reminder surface and enables direct Activity / accessibility fallback only when the system mechanism is unavailable.
4. AI reports remain AI-first with a visible local-template fallback; provider model discovery and ordered fallback remain available.
5. No periodic synthetic prompt traffic was added because it would disguise automated behavior and attempt to evade provider safeguards.
6. Raised Android and Windows application metadata to `1.16.12 / versionCode 377`.

## 历史阶段：Validation For `v1.16.12`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
2. `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed; Lint reported 0 errors and 125 non-blocking warnings.
3. `emulator-5554` / `Pixel_8` installed and launched `PaykiTodo-1.16.12-debug.apk`; metadata reported `versionName=1.16.12` and `versionCode=377`, and the account page rendered without a fatal crash.
4. Debug SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
5. Release SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
6. The remaining physical-device checks are OEM full-screen reminder delivery, lock-screen behavior, notification shade rendering, and battery policy.
7. The signed release build is local-only in this round; no GitHub Release or push was performed.
8. Windows publish completed; the known `WindowsBase` 4.0/5.0 WebView2/WPF warning is non-fatal.

## 历史阶段：Completed `v1.16.10` Scope

1. Reminder polling now passes the exact trigger timestamp into `ReminderForegroundService`, so receiver and polling paths share one dispatch identity.
2. New reminder scheduling no longer creates the old `+2s` backup alarm; application startup cancels legacy backup PendingIntents.
3. Planning Desk mappings, nodes, note creation, status refresh, and snapshot import notify the widget and remote-sync coordinator after direct Room writes.
4. Turnstile WebView allows the Cloudflare challenge host and third-party cookies and blocks HTTP/render-process failures.
5. AI reports remain AI-first with an explicit local-template fallback; no synthetic periodic anti-detection traffic was added.
6. Raised Android and Windows application metadata to `1.16.10 / versionCode 375`.

## 历史阶段：Validation For `v1.16.10`

1. Server integration tests and Android unit tests passed before the version bump.
2. The deployed cloud source now exposes `/healthz` and `/v2/auth/challenge`; the production environment still needs the owner's Turnstile Site Key and Secret Key before a real widget can appear.
3. Run Android Lint, Debug/Release assembly, and emulator smoke tests after the final metadata update.

## 历史阶段：Completed `v1.16.9` Scope

1. Android login, registration, and password recovery can obtain a Turnstile token from the configured challenge page.
2. The cloud service verifies the token against Cloudflare before processing authentication requests; missing configuration keeps existing deployments compatible.
3. Partial Turnstile configuration fails closed instead of silently accepting unverified authentication requests.
4. Added safe public/private configuration templates and kept the Turnstile Secret Key out of Android and Git.
5. Raised Android and Windows application metadata to `1.16.9 / versionCode 374`.

## 历史阶段：Validation For `v1.16.9`

1. Passed the 7-test server integration suite, Android unit tests, Android Lint, and Debug/Release APK assembly.
2. On `emulator-5554` / `Pixel_8`, installed and launched the Debug APK; package metadata reported `1.16.9 / versionCode 374`, and the account UI was visible without a fatal crash.
3. The emulator did not have an authenticated test session, so a real scheduled reminder could not be triggered through the app. The service-start fallback remains covered by the production catch path and requires authenticated controlled runtime testing for end-to-end full-screen evidence.

## 历史阶段：Completed `v1.16.8` Scope

1. Empty authenticated devices now initialize local sync revision at `0`, so an existing cloud snapshot is downloaded.
2. Devices with local data continue to upload to an empty cloud account or enter explicit conflict protection when the cloud already has data.
3. Added regression tests for empty-device download, local-data conflict, and local-data upload initialization.
4. Raised Android and Windows application metadata to `1.16.8 / versionCode 373`.

## 历史阶段：Completed `v1.16.7` Scope

1. Removed all migration-package actions from the unauthenticated surface; export and import remain available after login in `设置 -> 数据与备份`.
2. Updated the Debug-to-Release migration instructions so both operations require an authenticated account session.
3. Raised Android and Windows application metadata to `1.16.7 / versionCode 372`.

## 历史阶段：Validation For `v1.16.7`

1. Passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verified on `emulator-5554` that the unauthenticated screen contains no migration explanation, export action, or import action; authenticated `设置 -> 数据与备份` remains the migration entry point.
3. Passed `python -m unittest server.test_paykitodo_cloud_server -v` with all 5 integration tests.
4. Passed PowerShell syntax checks for the ignored local release-build and administrator-credential helpers without rotating production credentials.
5. Passed `git diff --check` and verified `user/`, signing files, environment files, and server database patterns are ignored.

## 历史阶段：Completed `v1.16.5` Scope

1. Caught system-level failures when Android rejects a background foreground-service start and published a normal reminder notification fallback instead of crashing the process.
2. Preserved full-screen reminder intent behavior during the notification fallback and recorded the rejection/fallback result in the reminder chain logs.
3. Allowed a newly created calendar event that has already started to keep historical reminder offsets; the alarm scheduler continues to ignore past trigger times.
4. Added an explicit editor notice for retained historical reminders and kept future calendar events subject to future reminder validation.
5. Added the existing dashboard artwork and launcher icon to the account login/register/recovery screen with a scrollable narrow-screen layout.
6. Added `-Rotate` to the ignored administrator credential helper; the default mode reads credentials and rotation mode writes a new password and restarts `paykitodo-cloud.service`.
7. Raised Android and Windows application metadata to `1.16.5 / versionCode 370`.

## 历史阶段：Validation For `v1.16.5`

1. Run `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verify the login page on the Android emulator in light and dark themes, including migration buttons and scroll behavior.
3. Verify the reminder fallback through a controlled Android 12+ background-start denial or an injected unit-level start failure; physical OEM lock-screen and battery policies still require device testing.
4. Verify creating an already-started calendar event with a past reminder saves successfully and does not schedule the missed trigger.
5. Parse the administrator helper with PowerShell without running `-Rotate`; do not rotate the production credential during build verification.

## 历史阶段：Completed `v1.16.4` Scope

1. Moved Android login and registration network work to `Dispatchers.IO` so the authentication action does not block the UI thread.
2. Refreshed the ViewModel authentication signal immediately after successful login or registration, so the app opens the daily board without requiring a restart or a polling tick.
3. Installed the signed Release build on the Android emulator and verified login, relaunch persistence, daily board, drawer, todos, calendar, Planning Desk, settings, and appearance navigation.
4. Added `docs/current/ANDROID_RELEASE_UI_AUDIT_1.16.3.md` with UI-tree evidence scope and residual physical-device risks.
5. Restored the final build configuration to the HTTPS cloud endpoint; the local cleartext endpoint used for isolated emulator testing is not part of the final build.

## 历史阶段：Completed `v1.16.2` Scope

1. Added migration-package export/import actions to the unauthenticated login surface, so a user can preserve local data before uninstalling a Debug-signed build and restore it after installing a Release-signed build.
2. Stopped the phone LAN service explicitly on account logout and blocked secondary entry points, reminders, and widgets from exposing data without a valid session.
3. Made reminder acknowledgement/snooze remove stale notification and foreground-service artifacts, and kept long-title reminder actions readable on narrow screens.
4. Changed automatic backup output to one latest archive updated on data changes, with cleanup of legacy timestamped archives.
5. Raised Android and Windows application metadata to `1.16.2 / versionCode 367`.

## 历史阶段：Completed `v1.16.1` Scope

1. Unauthenticated `/admin` requests render a PaykiTodo-owned login page instead of returning a `WWW-Authenticate` challenge.
2. Added cookie-based administrator sessions with logout and preserved CSRF checks for state-changing actions.
3. Kept valid Basic Auth as a non-challenging compatibility path for scripts and tests.
4. Raised Android and Windows application metadata to `1.16.1 / versionCode 366`.

## 历史阶段：Completed `v1.16.0` Scope

1. Added the account registration, login, token refresh, device, versioned encrypted snapshot, conflict, and administrator APIs on loopback port `18766`.
2. Added Android account UI and encrypted local account credentials while preserving Room as the local authority and `18765` as the separate LAN browser path.
3. Added a writable, offline-capable Windows local client with DPAPI-protected state and core todo/event/planning operations.
4. Added integration tests and public deployment/usage templates without private domain or server credentials.
5. Deployed the cloud API and administrator page behind the private HTTPS reverse proxy; the origin stays loopback-only on `18766`, with the existing phone LAN `18765` path unchanged.
6. Verified public health, account registration/authentication, administrator access, administrator CSRF protection, account disable/enable, and device revocation.

## 历史阶段：Completed `v1.15.22` Scope

1. Split desktop todo and event creation buttons and made their visibility tab-specific.
2. Reflowed the desktop header so navigation stays left while creation and connection controls stay right without narrow-window overlap.
3. Matched the Desktop Web current-time line to the phone calendar's full-width red line and current-day marker convention.
4. Added minute-aligned indicator refresh, fixed mini-calendar selection geometry, centered week headers, and one-second selected-column feedback.

## 历史阶段：Completed `v1.15.21` Scope

1. Added signed Release and Debug build tooling using the same source with separate signing/build types.
2. Added `.paykibak` ZIP migration archives with checksums, legacy JSON import compatibility, and secret exclusion.
3. Added local-first AES-GCM remote snapshot upload scheduling and the server-side ciphertext relay.
4. Added a self-contained Windows WebView2 shell for the existing LAN Desktop Web console.
5. Fixed Android API 26 compatibility in the remote-sync URL path and verified Android/desktop builds.
6. Added `我知道了` to every todo full-screen reminder, including same-day due todos.
7. Added remote snapshot summary inspection, explicit replacement confirmation, and pre-restore safety backup.
8. Added Windows server snapshot mode with local decryption and read-only board/todo/calendar rendering.
9. Unified the user-facing reminder control: one `提醒` checkbox now means full-screen plus notification-bar delivery; legacy channel values are migration-only.
10. Made todo reminder actions responsive: event reminders show only `我知道了`, intermediate todo reminders show completion/acknowledgement, and the final todo reminder additionally shows fixed ten-minute snooze.
11. Reflowed the accessibility overlay actions into a primary row and an optional second row, with single-line button labels and compact padding for narrow phones.
12. Revalidated retained-notification clicks so completed, canceled, or missing items only clear stale notifications instead of opening previews.

## 历史阶段：Latest Public Release Baseline

1. `v1.15.18` remains the latest GitHub Release and public APK.
2. The `1.16.0` source and local APKs are not published until the user explicitly requests a release.

## 历史阶段：Next Validation

1. Install `1.16.4` Release on a physical device and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify the single reminder switch and the event/intermediate/final todo action layouts on a physical phone.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Test Android + Windows login, offline edits, upload/pull, conflict blocking, logout, and device revocation against a disposable account on the deployed service.
7. Add field-level merge or explicit conflict-resolution UI in a later round; the current encrypted snapshot contract intentionally stops before automatic merges.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
