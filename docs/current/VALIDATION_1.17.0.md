# 1.17.0 日历与账号更新验收

日期：2026-10-25。Android `1.17.0 / versionCode 392`，Windows `1.17.0.0`，Room `33`。本轮为本地构建和验证，没有推送、发布或部署生产服务器。

## 功能与证据

| 功能 | 本轮行为 | 直接验证与范围 |
| --- | --- | --- |
| 日历主体 | 保留手机连续翻页、三日/周/月/列表与桌面小月历、重叠布局；新增主/辅助显示时区 | 手机6项既有交互回归及真实控件截图；Windows隔离UIA日历导航、布局与时区控件 |
| 创建与调整 | 默认日程时长、全天/定时独立草稿；表单与拖动共用时区/循环作用域规则 | 手机全天切换往返保持原时间/提醒；Windows草稿、取消、完成与时区拖动回归 |
| 多日历 | 描述、排序、归属、显隐与颜色管理；个人与共享日历区分 | 手机真实CalendarManager显隐测试；Room/备份往返、Windows控件及数据回归。隐藏日历不会关闭个人提醒 |
| 循环 | 间隔、无截止、最后一个星期几、无效日期跳过；当前/之后/全部保留改期例外与跳过记录 | 216项Android单测中的日期/复制/协调器用例；11项Room契约包含无截止补齐、改期身份、分支、DST异常事务回滚与农历年末 |
| 全天与提醒 | 排他结束日期；提前天数与任意钟点、事件时区；复制保持当前实例提醒 | 手机09:17/提前1天控件测试；纽约跨DST全天提醒数学与归档/Room往返；Windows对应模型和控件 |
| 详情、复制与通知 | 保留单强提醒窗口及队列；读取当前事项详情；复制当前实例为独立非循环草稿 | 两端复制数据回归与Windows详情控件；Android提醒界面增加前台、来源/权限变化重验。没有新增通知卡片系统 |
| 时区与协作 | 事件时区与显示时区分离；共享权限、邀请、参与回应、撤权、冲突处理 | Android时区/共享编解码、Room权限测试；21项服务端测试；Windows实际本地服务端两账号/多副本测试。未宣称已在生产或手机到电脑真实账号全链路验证 |
| 登录服务器与账号排版 | 普通登录页无域名，选择器和设置中管理服务器；手机底部操作紧凑同排 | 手机选择/取消/完成与账号按钮同行断言；Windows实际登录/选择器/账号浅深截图 |

## 执行结果

- Android：`:app:assembleDebug`、`:app:assembleRelease`、`:app:assembleDebugAndroidTest`、`:app:testDebugUnitTest`、`:app:lintDebug`通过。216项单测，0失败；Lint 0 errors / 133 warnings。签名Release通过apksigner验证。
- Android设备：`emulator-5554`（Pixel_8），安装本轮Debug与测试APK；11项数据契约及14项不同UI用例通过。UI图来自真实生产Compose控件，展示资料为内存合成数据；没有向真实账号写入这些示例。修复测试截帧滞后后，仅重新执行编辑器1项并核对重复/时区图，不重复计数；临时关闭的Wi-Fi/数据网络已经恢复启用。
- Windows：最新安装包实际安装目录中的EXE通过51项回归和1条14阶段隔离UIA流程，包含真实本地协作服务器测试。MSI旧`1.16.26`到新版本升级、修复、payload哈希、每用户安装、入口、启动、卸载与用户数据不变均通过。不是51条物理输入E2E。
- 服务端：`python -m unittest discover -s server -p 'test*.py' -v`，21项通过，测试数据库与账号独立于生产。
- 代码及提交检查：`git diff --check`通过；签名、账号与私有配置路径仍被Git忽略。当前源码与产品文档使用本产品行为描述。

## 安装产物

- Debug：`app/build/outputs/apk/debug/PaykiTodo-1.17.0-debug.apk`，SHA-256 `4F25DC81982D3D6C157E9B83B29D12B1DF2D29C753D469FE19D2A10C40D079FE`。
- 签名Release：`app/build/outputs/apk/release/PaykiTodo-1.17.0-release.apk`，SHA-256 `F5821C5366ECF49FDD3342CF64F674FE4A72CECD01E5EA4B3F27A9CD63FA1963`。
- Windows中文MSI：`desktop/installer-output/PaykiTodo-1.17.0-windows-x64.msi`，SHA-256 `B303F84BF304A48D59A4FDD0330CE0DBAB4387CF10733E3ECEA34A80928C6676`。
- Windows便携入口：`desktop/dist/PaykiTodo.exe`；便携使用需保留整个目录。EXE与程序集文件版本为`1.17.0.0`。EXE SHA-256 `6E6407F84BFC85C628E7B0ADABA8055EF8FABDDC915E13DCC042F466AD2D53E2`；主DLL SHA-256 `E9F4BF3C10D184B38B968472886C8D8100C3B42697909030BF3A872201BE6301`，与MSI payload一致。
- UI预览：`G:\Workspace\Deliverables\PaykiTodo\1.17.0\界面预览.html`；正式结果截图保存在同目录，来自本轮实际Android控件与隔离Windows窗口。

## 数据保护与证据位置

- 手机验收前备份：`G:\Workspace\Backups\PaykiTodo\emulator-before-1.17.0-20261025-004919`，642文件。验收后备份：`emulator-after-1.17.0-20261025-010835`，646文件。原文件无缺失；15张业务表按原列比较，原记录无修改或遗漏，账号与配置文件哈希未变。原待办表为空，不能把这次原库比较替代带任务的迁移契约测试。
- 数据库从32迁移到33；另有profileInstalled与安全启动标记正常变化，4个新增文件为隔离迁移测试数据库及锁文件。没有为恢复旧哈希而覆盖已升级的数据库。
- 手机数据与UI日志、完整截图、数据库比较报告：`G:\Workspace\Scratch\PaykiTodo\calendar-1.17.0`。
- Windows最终安装结果：`windows-installer-verified/install-d8ea0f20774442e0b7420ebd562a9aa1/result.json`；对应`installed-app`内保存TRX、步骤与截图。

## 实际边界

共享日历需要服务器同步更新到提供`/v3`接口的版本；本轮没有部署生产，旧服务器会提示功能尚不可用。个人加密快照保持独立兼容，共享内容由服务端按成员权限管理。手机OEM锁屏/振动/长待机、Windows用户物理键鼠全链路和生产网络协作未由本机验证代替。桌面到点提醒仍要求程序运行。
