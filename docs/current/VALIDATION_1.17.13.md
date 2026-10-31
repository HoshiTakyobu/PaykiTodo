# 1.17.13 循环规划绝对提醒验收

2026-11-01从`d765f14`继续原七项目标。Android为`1.17.13 / versionCode 405`，Windows为`1.17.13.0`，Room保持34。本阶段完成循环规划绝对提醒归属及三端预览，设置/字体/帮助、报告期限、客户端更新和历史清理沿用此前验收。本文记录本地产品验收；源码推送结果另见[历史审计](PUBLIC_HISTORY_AUDIT_20261101.md)。公开安装包Release仍为`v1.15.18`。

## 行为与边界

绝对时刻只提醒一次，归属整门课程所有时段中开始时刻大于或等于该时刻的最近实例；相等归本次，并列按输入时段顺序。多个绝对时刻分别归属、去重，同一拥有者合并；相对分钟在每次课程继续提醒。已过去的提醒保留而不补响。

目标超出近期生成窗口时提前物化拥有者。Android补齐从当前日期窗口检查缺口，避免远期拥有者让中间课程永久缺失；循环模板不保存单次绝对提醒。有限范围内没有之后课程时定位报错、保留输入，整批拒绝。既有取消、单次修改与系列共同字段修改不会自动重新分配或复制绝对提醒。

Android、Windows和手机LAN网页均显示绝对时刻及所属实例，预览编辑后重算。LAN复用原session的识别时间和账号/连接代次，编辑校验不写库，派生归属字段由后端重算；过期、取消、乱序响应和重复确认受到保护。普通编辑直接新建带绝对提醒的循环草稿仍要求从规划预览确认实例，不能绕过归属规则。

## 验收结果

原始证据在`Scratch/PaykiTodo/planning-absolute-1.17.13`。重跑和截图重采不重复计数。

| 范围 | 实际结果 |
| --- | --- |
| Android单元测试 | 270通过，0失败/错误/跳过；含12项新增分配算法检查 |
| Android Lint | 0错误、137警告、30信息；数量与1.17.12相同 |
| Android构建 | Debug、AndroidTest及同证书签名Release成功 |
| Android模拟器 | 64条目中63通过、1项条件跳过；53数据、5Compose、5Web通过，本轮新增12数据/2Compose/3Web均通过 |
| Windows针对检查 | 60个不同检查最终通过，含13项新增数据回归及实际WPF编辑/取消/重复确认流程 |
| Windows实际包 | 另2/2启动与更新nonce检查通过；本轮合计62个不同检查 |
| LAN网页 | 9/9首轮通过，实际DOM和生产脚本、模拟HTTP；Node语法检查通过 |
| Windows成品 | MSI成功；实际payload 484文件逐项与files.json相同，无本机client-config.json |
| Android成品 | 包名com.paykitodo.app、1.17.13/405核对；Release v2签名有效且与1.17.12同证书；两APK内LAN脚本、AI prompt、Wiki与源码字节一致 |
| 更新资产 | 本地完整ZIP 482条目、相对1.17.12增量ZIP 4条目；RSA-PSS独立验签及逐项重建与成品一致 |
| 原用户数据 | user0 646/646、user10 148/148逐文件SHA256相同，零增删改 |
| 最终交付 | 新目录494文件；SHA256.json核对其余493文件，不覆盖旧交付 |

Android条件跳过为既有`CalendarRepositoryTest.actualWindowsArchiveCanBeEditedAndExportedForWindows`，要求外部Windows生成的`calendar-desktop.paykibak`。本轮未提供该旧样本，Runner虽输出`OK (64 tests)`，状态码仍明确为1项assumption跳过，不能报告64项全部通过。本轮新增归属/备份/编辑回归不依赖该样本。

算法与数据库检查覆盖全课最近实例、相等及同刻并列、去重、纯绝对与混合提醒、有限无目标的整批回滚、无截止及远期拥有者/中间补齐、每2周与每10000周、月末/闰日、取消/系列修改、账号切换和重复确认。Compose验证编辑后的所属文字、取消无写入、确认接收修改后的候选及防重复导入。LAN合同验证JSON归属、冻结年份、编辑重算和伪造派生字段不能绕过校验。

LAN浏览器9项覆盖初次归属、编辑时禁用及之后重算、无目标保留草稿、乱序响应、取消后响应、取消前debounce、连接代次变化、连接终止和重复确认。使用实际`index.html`、`app.css`、`transient-planning.js`及生产`api/openModal/closeModal`，HTTP和连接外壳为模拟；与Android合同分别记录，不能描述为真实LAN网络端到端。归属文字首次位于截图滚动下方，另重采3张可见截图，不累计检查数。

## 首次失败及复验

- Android首轮编译因`${startLabel}`字符串边界写错而失败，修正后单测/Lint/Debug/AndroidTest构建成功；最后再打包Debug/AndroidTest/Release，使最终LAN资产一致。失败与两次成功日志均保留。
- Windows初次PowerShell 5启动器无法解析UTF-8脚本，改用pwsh。首轮59项为58通过/1失败：WPF测试在前一刷新布局完成前查找每日看板输入按钮；明确导航并更新布局后定向1项通过。审查补充内部混合循环/NONE候选防御并新增1项，相关28项定向复验通过，最终不同检查数60。
- Android验收汇总脚本最初只检查成功状态码并假定64项成功，触发断言后补查assumption状态；修正为63通过/1跳过。未修改产品或重跑成功套件。

## 设备与资料保护

只使用既有headless `emulator-5554 / Pixel_8`专用user11，安装当前Debug和AndroidTest，未操作真实手机或真实账号。测试前确认qemu PID20332身份并恢复原暂停；原始user0/user10/user11前后备份保存在`Backups/PaykiTodo/planning-absolute-1.17.13/before`与`after`，专用user11由214变为218文件，完整保留。

完成后停止user11应用与测试，返回user10主页。原user0/user10哈希完全相同，核对AVD进程身份后恢复PID20332原Suspend，353线程为Wait/Suspended。浏览器使用独立headless Edge上下文并已关闭；WPF使用隔离Win32桌面与独立资料，不占用户输入桌面。

## 证据入口

- `android-build.log`、`android-fixed-build.log`、`android-final-package.log`：初次编译失败及最终构建。
- `android-device.log`、`android-evidence-summary.json`、`verify-android-evidence.py`：64条目实际状态、270单测/Lint计数、原用户哈希。
- `windows-result-summary.json`与关联TRX：60不同检查、首次失败、定向复验；`windows-packaged.log`和`packaged/`另2项实际包检查。
- `windows-preview-retry/windows-transient-absolute-preview.png`：真实WPF归属预览。
- `lan-result-summary.json`、`lan-browser-attempt-1/result.json`、`lan-browser-validation.cjs`：9项真实DOM/模拟HTTP流程。
- `lan-browser-owner-captures/initial-owner.png`、`edited-owner.png`、`invalid-range.png`：可见归属与错误截图。
- `artifact-result.json`及关联验签/重建报告：484文件payload、APK身份/签名/内置资产、本地更新包和493文件交付哈希。

## 本地成品

完整交付目录为`G:\Workspace\Deliverables\PaykiTodo\1.17.13`，包括两APK、MSI、`Windows-portable/`完整目录、安装说明、完整/增量/签名更新资产和SHA256清单。便携版必须保留整个目录。

| 成品 | 字节 | SHA256 |
| --- | ---: | --- |
| PaykiTodo-1.17.13-debug.apk | 40862619 | BD15F8B970AC6D309373D78CD2F517CD133847BDFB07FB2A3DA7C916D5F8F097 |
| PaykiTodo-1.17.13-release.apk | 21084248 | AAD5BFD8BE3F2C564CA07491B493794DFE58EC61BB0C078FC62A74CE02F0B25F |
| PaykiTodo-1.17.13-windows-x64.msi | 106226796 | 80B874F10003E6E57DF348AD10363651AC438C754FFCA848AA26096C61F55CB5 |
| 完整Windows更新ZIP | 134261957 | DC89E02910B656C0D14230E12EC923A058C769A00D258D39B08C2E8511699E2F |
| 1.17.12至1.17.13增量ZIP | 50363802 | 528E77C4A9C6A9ECE8722A4D86584ECD56236155121BE8C3C818435B2D543BA7 |

## 实际限制

本轮未重复已成功的MSI升级/修复/卸载生命周期，安装器逻辑未改；未将模拟更新资产称为未来公开Release验证。真实LAN网络、手机手动系统安装、OEM长后台/锁屏/振动和所有时区的夏令时行为没有新增实测。没有上传安装包、创建tag/Release或部署服务器。

1.17.8设置/字体/帮助、1.17.9报告共同规则与168小时期限、1.17.10临时规划主体、1.17.11更新链路、1.17.12实例提醒保护及未公开历史清理保持，分别见同版本验收与历史审计记录。剩余仅为新增提交/文档的完整审计和已授权main源码推送；推送实查后以当前状态和历史审计顶部为准。
