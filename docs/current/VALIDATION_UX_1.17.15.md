# 1.17.15 十项体验修复验收

状态：10 项体验修复的本地实现、验收与交付完成。本文记录本轮实际结果，不把前轮交接文档、历史测试或未执行的子项当作完成证据。

起点 main / 50c217e，Android 1.17.14/406、Windows 1.17.14.0；本轮 Android 1.17.15/407、Windows 1.17.15.0，Room 34。最新提交以实际 git log 为准，原未跟踪规划 Prompt 保留且排除本轮提交。公开发布仍 v1.17.14；没有 push/Release/服务器部署/真实设备安装。

验收契约：[A1–A10](UX_ACCEPTANCE_20261103.md)；补充范围按主文档2.1推荐默认值推进，未冒称逐项确认。统一证据目录 `G:\Workspace\Scratch\PaykiTodo\ux-implementation-20261103`。

## 数据保护与最小复现

Windows基线在独立Win32桌面、隔离profile运行实际WPF源码，1条原生控件流程通过，保留原始截图与TRX于baseline。字体诊断另独立进程，基线记录于font-baseline/font-rendering.json，方正小标宋命中FZXiaoBiaoSong-B05S、StyleSimulations.None；测试宿主96DPI、Awareness1不能当作真实EXE的DPI声明。

首次诊断脚本遇到反射方法重名和NaN/Infinity JSON序列化问题，修正后采集成功；这些属于诊断代码失败。产品复现包括三小时日程单行/地点缺失、字符箭头、原始色值、周列表范围缺陷。新WPF流程发现最大化隐藏恢复时ShowActivated=false条件限制，现修为先正常显示后恢复窗口状态。

Android 使用全新独立 headless AVD PaykiUx（emulator-5580），资料在本轮 Scratch/avd，仅安装本轮包与合成测试。最终 API 34、Asia/Shanghai、飞行模式，800×1600 像素/density 400，即 320dp 宽。旧 Pixel_8、真实手机、真实账号和正常 Windows 用户资料未操作。新建 AVD 最初皮肤配置错误已修，没有复制旧 AVD 数据。最终停止应用及 Instrumentation、关闭新模拟器，AVD 实体与全部证据保留，见 android/emulator-stop.log 与 devices-after-stop.log。

## R1–R10 实现与结果

下表记录当前源码实现；平台验收结果另列，源码入口存在不计为通过用例。单测、组件操作、独立桌面 UIA、实际 WebView 排版和真实包启动分别记录，重跑不重复计数。

| 需求 | 实现入口 | 当前行为 |
| --- | --- | --- |
| R1 关闭到托盘 | NativeWindow.cs、DesktopReminders.cs | 主窗口普通关闭隐藏，托盘恢复同一窗口；显式退出及会话结束消息走真实关闭，取消会话结束后普通关闭仍隐藏，提醒与同步生命周期保留。 |
| R2 选色色块 | NativeColorChoice.cs、NativeEditors.cs；CalendarEventEditorDialog.kt | Windows 待办与日程收起值及展开项都显示色块，保留自定义原值；手机日程保留色块，手机待办继续由第一个选中主分组取色。选择属于编辑草稿，取消不写库。 |
| R3 日程三行 | NativeWindow.Calendar.cs | 标题按真实字体度量最多三行，时间与非空地点按空间递减；极短事件使用矢量紧凑指示，完整字段留在提示与详情。 |
| R4 双样式 | NativeWindow.CalendarPreferences.cs、NativeWindow.Calendar.cs | 现有 MODERN/CLASSIC 设置增加同一日程预览，完成保存、取消放弃；desktopCalendarEventStyle 跨端字段保持。 |
| R5 Markdown 报告 | NativeContentReader.cs、OfflineContentReader.kt、content-viewer | 两端共用本地解析与清洗资源，单份报告打开阅读器；原文复制与搜索/备份字段保持，过期与账号变化撤下正文。 |
| R6 HTML 帮助 | NativeWindow.Help.cs、WikiActivity.kt、Build-ContentGuides.cjs | Windows 从内嵌维护源排版 HTML；手机由维护源生成本地 HTML，目录适配窄屏，保留离线资源。 |
| R7 字体与裁切 | app.manifest、NativeUi.cs、NativeWindow.Calendar.cs、viewer.css | 正式 EXE 明确声明 PerMonitorV2；保留用户字体及真实字重，移除下拉选中项伪粗体，过期日程文字不随整卡降低透明度，按字体行高安排正文。 |
| R8 周列表 | NativeWindow.Calendar.cs | 按所选周起止边界筛选重叠事件，列表上一周/下一周始终推进七天，包含跨周延续段。 |
| R9 矢量箭头 | NativeUi.cs | 通用 ComboBox 使用矢量 Path，尺寸随控件字号调整，保留选中模板、可编辑输入和原有键盘交互。 |
| R10 今日安排 | BoardArrangement.kt/.cs、DailyBoardSnapshot.kt、DashboardChrome.kt、NativeWindow.Pages.cs | 双端应用内看板合并待办与日程，按开始/自身 DDL 稳定排序；全天、逾期、未定时间与明天预览保留，旧消费者字段继续提供。 |

## 失败定位与修正

真实 Android WebView 一度停留在“正在打开…”。device-diagnostics.log 确认本地入口和 Marked/DOMPurify 均已加载，但原生 WebMessage 的 event.origin 为空且 source 为 null，被原浏览器来源检查拒绝。共用脚本现允许由受限本地宿主发送的此类消息，继续拒绝普通跨源页面消息和带 Window 来源的空 origin 消息；共享浏览器检查与原生宿主复验分别记录，不能用前者替代后者。

手机帮助首轮检查过早读取静态正文，此时目录脚本尚未初始化；等待目录就绪后 320dp 检查通过，没有据此修改布局。日程色块首轮未选中是测试进入嵌套横滚区后没有滚到实际控件，指定外层纵向滚动后通过。Windows 早期重跑包含测试布局、应用关闭时序、模态标题和回退控件未更新布局的问题，失败日志保留；最终新流程结果单独记录。

正式包复查发现旧 1.17.14 和首轮 1.17.15 EXE 都仍是 SystemAware：项目的 ApplicationHighDpiMode 属性没有落实到 WPF 实际进程，manifest 缺少 DPI 声明。补充正式 manifest 后重新生成最终 MSI，实际 EXE 的上下文变为 PerMonitorV2；首轮包保留在 windows-first-build，不作为交付。会话结束验收还确认 WPF 的 Application.SessionEnding 由隐藏消息窗接收，单发主窗消息不会触发它；主窗现处理 WM_QUERYENDSESSION/WM_ENDSESSION，隔离消息验证取消后恢复普通隐藏、确认后退出，不执行真实系统关机。

Windows 跨端检查首轮误把 WEEKLY 报告也按日报的 168 小时期限清理，修正测试预期为保留其 rawMarkdown 后通过，没有改动产品保留策略。Android 实际 WikiActivity 返回后的首轮生命周期断言过早观察到 STARTED，改为等待异步销毁后通过。所有这些重跑按最终测试名去重，不累加通过数。

旧 wpf-extended2 的三份字体截图与原看板截图 SHA256 完全相同，不能作为三字体各自渲染覆盖证据。最终字体检查使用独立运行的矩阵与真实字形诊断，按实际执行范围报告。

## 实际测试结果与证据

| 范围 | 最终结果 | 证据（相对本轮 Scratch） |
| --- | --- | --- |
| Android 单测 | 273，0 failure/error/skipped | android/build-verified.log；app/build/test-results/testDebugUnitTest |
| Android Lint/构建 | 0 错误、138 警告、30 信息；公开模式 Debug/AndroidTest/签名 Release 构建成功 | android/build-verified.log、build-final-assets.log、lint-final.log |
| Android 设备 | 12 个不同检查通过；11 项核心检查 27.524s，实际 WikiActivity/Back/onDestroy 另 1 项 2.162s | android/device-final-shanghai.log、device-help-activity-final.log、verification-final.md |
| Windows 数据、控件与实际包 | 按测试名去重 141 项：139 通过、0 失败、2 跳过，139 已包括字体矩阵、14 阶段 UIA 和包检查 | windows-evidence-summary.json；windows-full、windows-uia3、windows-packaged-final、windows-final-resources 下的 TRX |
| 共享源资源 Chromium | 7 项通过，0 页面错误、0 外部请求；属于共享资源验证，另核包内字节 | shared-reader-verified/shared-reader-results.json |
| 最终打包资源 | 两 APK 各 10 项 viewer/wiki 与源码字节相同；Windows 8 项 reader 资源及指南共 9 项一致 | android/apk-resource-verification.json；windows-final-resources/packaged-content-resources.json |
| 成品交付 | 490 文件；SHA256.json 覆盖其余 489，便携目录 484 文件逐文件匹配最终 payload | delivery-verification.json |

Windows 两个条件跳过为 AndroidSnapshotCrossClientTests.AndroidCompletionDownloadsIntoIndependentWindowsWorkspace 和 LiveAndroidSyncTests.OpenPackagedWindowRefreshesAfterAndroidCompletionWithoutManualSync；本轮没有提供同期 localhost 跨端网络 fixture。原先因尚未构建而跳过的实际包检查已由最终包检查覆盖。保留了 8 个既有 xUnit 警告及新 WFO0003 manifest 建议警告；没有为消除建议而撤掉已由实际进程证明生效的 DPI 声明。

Android 的 12 项包括 7 项阅读器/帮助、4 项 Compose 行为和 1 项迁移合同。Compose 使用真实产品组件和合成回调，初始化失败及渲染器退出为注入；没有冒充完整业务持久化、真实账号网络切换或 OEM 故障。实际 MainActivity 冷启动 Status ok，TotalTime 2285ms，crash buffer 中 FATAL EXCEPTION 为 0；本次耗时不作为性能承诺。

### A1–A10 按实际范围核对

原验收设计保留为契约，下表说明本轮执行范围，未把所有子项统一判为通过。

| 契约 | 已验证范围 | 仍未验证的边界 |
| --- | --- | --- |
| A1 托盘 | 真托盘菜单/双击回调、10 次隐藏恢复、最大化/最小化、后台短延迟提醒与详情、显式退出释放计时器/图标；实际 EXE 会话取消/确认及 nonce | 物理 Alt+F4/系统菜单输入、真实关机注销、隐藏期间生产同步与未来公开更新 |
| A2 颜色 | Windows 两种编辑器收起/展开、草稿取消/完成、自定义色与持久化；手机日程色块草稿与时间保持 | 手机待办沿用主分组取色；所有颜色/主题/物理键盘组合未遍历 |
| A3 标题 | 干净三小时事件实际三行并显示时间地点；短事件、字号、重叠分列与多窗口宽度控件检查 | 全部字体/时长/地点/跨天组合未做完整笛卡尔积 |
| A4 样式 | 现代/经典实际预览、取消/完成、保存字段；Android 两模式实际导出由 Windows 消费并重导出，原值保持 | 未做真实账号服务器往返或第二台物理电脑同步 |
| A5 报告 | 两端实际 WebView GFM、安全样例、原文、明暗/大字、过期/身份撤下及失败回退；共享资源空/未闭合/数万字与横滚 | 宿主故障为注入；所有旧报告和生产账号切换未穷尽 |
| A6 帮助 | 两端实际 HTML/目录，手机实际 WikiActivity 返回释放；维护源逐章静态核对、最终包内资源一致 | 全部非本轮功能未逐章执行真实账号操作 |
| A7 字体 | 两真实 EXE DPI 上下文前后对照、两字体 12/14/18/22 度量、14 字号下三种渲染模式、文字对比与完整行高 | 125%/150%/200% 物理缩放与跨监视器移动未测 |
| A8 周列表 | 所选周上下界、跨周延续、整周翻页、跨年/时区与隐藏过滤的控件/数据检查 | 所有夏令时和真实共享撤权组合未穷尽 |
| A9 箭头 | 真实 WPF 矢量 Path 与通用选择模板、颜色/字体/模型控件及字号检查 | 物理 Alt+下/Enter/Esc/Tab 与所有 DPI 组合未逐键执行 |
| A10 混排 | 双端数据排序、实际组件顺序与类型回调、父子来源、无 DDL/逾期/明天、边界与原时间保持 | 生产异步同步、真实权限撤销和午夜长期后台运行未测 |

## 字体结论

现有字体能命中真实字形，不能从一张截图归因于字体损坏。已定位选中下拉项强加Semibold和过去日程父层65%透明度：前者绕过单字重字体保护，后者削弱小字对比度。修为选中边框与实底/文字分别处理，不全局强制ClearType。标题及时间轴文字按真实字体度量取行高，极短时段放不下一整行时使用矢量紧凑指示与完整提示/详情，避免切掉半行笔画。

正式 EXE 对照在同一独立 Win32 桌面进行：1.17.14 WindowDpi=96、Awareness=1、PerMonitorV2=false；最终 1.17.15 WindowDpi=96、Awareness=2、PerMonitorV2=true，见 windows-packaged-final/baseline-exe-dpi.json 与 packaged-dpi.json。可以证明声明及进程上下文已修复，不能由 96DPI 直接证明其他物理缩放下的视觉效果。

独立字体矩阵验证 Microsoft YaHei UI 与 FZXiaoBiaoSong-B05S 的 12/14/18/22 字号，并在 14 字号比较 Auto/ClearType/Grayscale；12 组记录均 StyleSimulations.None，testhost 仍为 96DPI/Awareness1，不能与正式 EXE 的上下文混同。默认 14 字号的真实三行标题 LineHeight=19、ActualHeight=MaxHeight=57，指标和原始截图在 windows-final-resources/ux-calendar-title-metrics.json 与 ux-calendar-three-lines.png。不全局强制 ClearType；125%/150%/200% 与跨显示器视觉验收仍缺少环境。

## 帮助内容映射

维护源为 docs/WINDOWS_USER_GUIDE.md 与 docs/ANDROID_USER_GUIDE.md。Windows打包原文并用同一本地引擎排版；手机HTML由tools/build/Build-ContentGuides.cjs生成。静态源码映射与真实操作核对分别保留，不把目录或版本更新当作逐章验收。

逐章源码审核见 documentation-review.md，覆盖独立 Windows 账号客户端与手机 LAN 两条路径、所选周列表、统一安排、临时规划与当前设置导航、各入口循环默认范围、托盘退出与更新、报告/字体/资源。最后修正手机待办并无独立颜色入口的说明：第一个选中主分组决定颜色，日程提供独立色块。生成器 --check 与脚本语法检查通过后资源冻结，两 APK 的最终 HTML 与源码一致。Windows 实际帮助和手机实际 WikiActivity 均通过 HTML/目录检查；其余章节明确为静态核对，不声称真实账号下全部照说明运行。

## 构建与交付

| 成品 | 实际路径（相对仓库） | SHA-256 |
| --- | --- | --- |
| Android Debug，42,950,227 字节 | app/build/outputs/apk/debug/PaykiTodo-1.17.15-debug.apk | 97B993898029F8441D74080F795C5DF8C48CC18A0846E78435B8743A0987023F |
| Android 签名 Release，21,123,658 字节 | app/build/outputs/apk/release/PaykiTodo-1.17.15-release.apk | 2814979C95F2457A9FED40EE011E4D6927601813A55DDE79410F75035F5E2C42 |
| Windows MSI | desktop/installer-output/PaykiTodo-1.17.15-windows-x64.msi | 65721327BEBE752593FFE1441AB22FA9C381207259AE05C1171E85F61B1CCE1B |

两 APK 均为 com.paykitodo.app、1.17.15/407，公开构建不嵌入本机默认私有服务器。Release 验签有效，与 1.17.14 证书一致，证书 SHA-256 为 `34e6094bfd8e14d675fcfed2f06fc121500349f9b45d4d0b84f9810399970610`。Debug 与 Release 签名仍不同，没有安装到真实手机。

最终 Windows 完整 payload 为 `desktop/obj/installer/1.17.15-bdd224c8b65b4a47aa9243752c34e968/payload`，484 文件；入口 PaykiTodo.exe 与 PaykiTodo.Launcher.exe 需要完整目录。交付 `G:\Workspace\Deliverables\PaykiTodo\1.17.15` 共 490 文件，包含两 APK、MSI、说明及完整 Windows-portable；SHA256.json 覆盖其余 489，便携目录与最终 payload 逐文件 SHA256 一致。旧包、首轮失败包与所有证据保留；未重复 MSI 实机安装/升级/卸载生命周期，没有公开发布或部署。

本轮必项已完成。剩余边界为缺少物理多 DPI/跨屏环境、真实账号与生产跨端同步、第二台物理电脑、真实手机 OEM 通知/后台/触觉、完整键盘输入组合和未来公开更新；它们没有被计作本轮通过。
