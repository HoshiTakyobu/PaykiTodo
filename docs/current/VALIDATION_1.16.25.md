# 1.16.25 验收记录

## 版本与结果

- Android `1.16.25 / versionCode 390`，Windows `1.16.25.0`，Room `32`，开始基线 `6f3800f`。
- 193 Android 单测；Lint 0 errors / 131 warnings；13个不同模拟器用例经针对性复验通过；26 Windows 回归（含实际 EXE）及1条14阶段隔离 UIA 流程通过。
- 第三方观察及运行状态仅保留在私有恢复副本中；公开记录仅保留 PaykiTodo 的需求、实现与验收。
- Windows 生成迁移包→Android 独立 Room 数据库导入、修改→导出→Windows 读取，核对日历归属、名称与已完成纪念状态。手机及桌面新数据字段进入现有整份快照链路。
- 新版 Debug 安装在既有 `emulator-5554 / Pixel_8`（Android14），冷启动与登录页渲染正常；当次外部人机验证显示加载失败，未进行真实账号登录验收，也未绕过账号门禁。应用与测试包最后均停止，模拟器保持运行。
- Windows 使用独立桌面和独立资料，未接管用户键鼠或剪贴板。未推送、发布、建标签或部署服务器。

## 实现范围

独立日历贯穿两端的创建、名称/颜色、日程归属、显隐、循环生成、周模板、迁移和备份。旧事项默认归常规日历；删除日历保留事项，孤立引用回退默认。彩色复选框有空心边框、实心选中态和对比色勾。Windows 时间网格按15分钟落点生成30分钟蓝色草稿，再次点击打开编辑器；取消不写入，显隐重绘不跳到顶部。

Windows 默认主字体改为 Microsoft YaHei UI，设置可选择已安装字体或 Sarasa；Android 默认系统无衬线，可选择内置字体或导入本机 TTF/OTF。预览完成后应用，取消不改动。字体文件不导出，问候字体及看板图片保持原样。

Windows 今日待办左色条8px，DDL 使用闹钟符号及粗体；值得期待/倒数日恢复色条。AUTO 默认保留目标本地日期过去7天内的项目（两端可调），KEEP 可长期显示已完成纪念，HIDE 隐藏；完成待办或日程结束后询问。保留纪念不会把事项改回未完成。未来同系列循环待办按手机逻辑折叠，右侧分组选择与按钮等高。

参考拾光设置的左分类/右内容、标题说明和细分隔线；手机为分类入口与单列详情，备份目录/导出/恢复分行，维护工具收起。Android 应用标签、欢迎及账号入口恢复 PaykiTodo。LAN 旧编辑路径保留新日历与纪念字段。

## 检查与证据

证据根目录：`G:/Workspace/Scratch/PaykiTodo/feedback-261023/`，截图及私有资料不入 Git。

| 检查 | 结果与范围 |
| --- | --- |
| Android 单元测试 | 193通过；包含7天边界、完成纪念、旧JSON默认值、备份及数据指纹、日历周模板归属 |
| Android Lint | 0错误、131警告 |
| Android 设备用例 | 首轮13项中12通过；字体取消用例修正后所在5项界面测试通过。共13个不同用例，非单次全绿。包括2迁移、2独立Room/跨端包、5新界面和4既有工作区控件 |
| Windows 回归 | 最终26/26通过，包含成品启动、已有本地服务同步/冲突、日历/纪念数据回归、原生新交互和持久化 |
| Windows UIA | 1条14阶段流程通过；独立桌面无全局输入 |
| Windows 网格输入 | 生产Canvas路由事件验证第一次输入仅建草稿；指定15分钟seed后点击真实草稿按钮并确认/取消、重载数据。属于控件验证，不冒称物理鼠标端到端 |
| 跨端包往返 | `windows-feedback/calendar-desktop.paykibak` 经手机导入/修改，返回 `calendar-android.paykibak`；最终Windows测试核验 |
| LAN | JavaScript语法与无界面Edge连接回归通过；未新增生产账号/服务器写入 |
| 构建 | Debug、AndroidTest、签名Release、自包含Windows构建成功；Release v2签名验证通过，1位签名者 |
| 安装/启动 | emulator-5554显示390/1.16.25，aapt应用标签为PaykiTodo；MainActivity冷启动正常 |

重点截图：`windows-feedback/windows-calendar-ownership.png`、`windows-backup-settings.png`、`windows-recurring-expanded.png`、`windows-countdown-choice.png`、`android-backup.png`、`android-final-startup.png`。Windows 性能改动采用渲染数据缓存、局部时钟刷新与滚动保留；性能结论限于实际测量。

## 验证中发现并修复

- 早期Windows新复选框XAML缺少x命名空间，造成原生窗口测试失败；补齐后通过。
- 日历网格初始化和旧视图卸载的ScrollChanged覆盖已保存位置；限制为当前已加载视图的有效滚动变化，300px滚动后切换显隐保持原位。
- 旧LAN编辑器不携带新增字段、周模板删除日历后的引用残留；更新保存链路并保留未知模板字段。
- Android旧“只显示未来倒数日”断言与新需求不符，更新为近7日；编译期间修正新字体类型和误插入的草稿参数。
- 新测试曾缺构造参数/导入和未安装的返回键测试依赖；按现有依赖调整。字体返回测试没有等待正确取消状态，新增明确取消入口并通过实际点击验证。Windows新日历输入用自动化名称定位，避免匹配ComboBox内部TextBox。
- LAN脚本初次缺Playwright运行环境；在本项目Scratch安装测试依赖后通过。未改产品依赖。
- 文档收尾脚本的循环变量覆盖目标路径，曾把PROJECT_STATUS内容写到生成的Windows EXE；发现后将内容恢复到正确文档，修正脚本并重新发布EXE。成品签名版本、哈希和独立桌面成品回归重新核验。源码、用户资料及APK未受影响。

## 数据保护与边界

测试前备份：`G:/Workspace/Backups/PaykiTodo/emulator-before-1.16.25-20261023-174814/private-data.tar`。原635个文件无缺失，604个字节一致；变化涉及数据库迁移、启动保护/安装记录、每日引言及登录页WebView运行缓存，额外运行文件保留。原数据库各业务表按旧字段逐行比较全部相同，Room31升级到32。没有用测试内容覆盖原业务数据，因此保留正常迁移结果，不回滚用户数据库。详细比较在 `emulator-data-verification.json`。

本轮跨端一致性范围为上述日历归属、循环补齐/折叠、编辑字段保留、纪念规则、备份和快照。没有把控件测试当成所有真实账号场景或全平台功能一致证明；既有Windows高级循环编辑作用域、农历/OCR/多时段能力差异仍见功能台账。物理手机OEM锁屏、长待机、振动、小组件、每种第三方字体和系统文件选择器全链路未在本轮穷举。自定义字体文件仅本机保存，缺失时回退系统字体。新日历数据应在两端均升级本版后使用，旧客户端不能保留其未知字段。

## 本轮成品

Windows分发需保留整个 `desktop/dist/`，不能只复制EXE。公开Release仍为v1.15.18。

| 文件 | SHA-256 |
| --- | --- |
| `app/build/outputs/apk/debug/PaykiTodo-1.16.25-debug.apk` | `112358624A87553C6D65C063039D69F79AD1E70DAEF8A8FEAF5519D1A44AAC4A` |
| `app/build/outputs/apk/release/PaykiTodo-1.16.25-release.apk` | `4385567D5E671516E0DD6E2ADACCA7D2FD444F322F0A12F0FF741B6E6F1EF169` |
| `desktop/dist/PaykiTodo.exe` | `D0FFC7FDB45E713C2D9722062807B4AA58CD39E779349FB186D458FEF74FF9CE` |
| `desktop/dist/PaykiTodo.dll` | `CFE4A99A23AF801F823788E568D9148C84C3BF3E5E3E88848A4A7349C054229A` |
