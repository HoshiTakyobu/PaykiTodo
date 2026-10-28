# 1.17.5 双端体验与后台同步验收

2026-10-28；开始基线 `344bcdd`。Android `1.17.5 / versionCode 397`，Windows `1.17.5.0`，Room `33`。本轮完成[原会话的 12 项反馈](REQUEST_2026-10-28.md)，只作本地交付和中文提交。

后续“继续”补充了[已打开正式 Windows 窗口的自动同步验收](VALIDATION_1.17.5_LIVE_SYNC.md)：两端各新增 1 项检查通过，累计不同模拟器测试 29 项、Windows/跨端/UIA 检查 68 项；应用及本页安装包哈希保持不变。下列数量记录初轮执行范围。

## 需求与实际结果

| 项目 | 实现与验证 |
| --- | --- |
| 1 双端图标 | 保留原标志并放大，白色圆角底；Windows 多尺寸 ICO、Android 自适应前景及应用内图标已更新。安装成品启动检查通过。 |
| 2 品牌字体 | Book Antiqua 粗斜体转固定矢量轮廓；更换正文字体不改变品牌。未将本机字体文件放入仓库。 |
| 3 导航字体 | HYQiHei 60S 常规转固定轮廓，保留辅助功能名称；随主题变色。 |
| 4 正文字号与渲染 | 待办标题 19、DDL 16、详情 14；仅使用字体实际存在的字重，单字重字体不模拟加粗。Microsoft YaHei UI、HYQiHei 60S、FZXiaoBiaoSong-B05S 实际解析与截图通过。 |
| 5 卡片标记 | 圆形勾选、绘制的彩色闹钟、彩色分组标签、循环/完成状态和侧色条。多个分组标签可同时显示。 |
| 6 清单时间 | 两端支持 `- 做作业, #16:00` 和中文逗号；写入创建当天本地时区 DDL 与到点提醒。无时间的旧格式指纹不变，非法时间提示原始行号，层级/备注保留，已过时刻不补响。解析、数据库及保存预览检查通过。 |
| 7 手机入口 | 使用与 Windows 同结构的纸张轮廓和三行勾选；真实 Compose 入口打开编辑器、解析和确认通过。 |
| 8 导入展示 | Windows 直接展示待办卡片，不增加清单分组标题或容器；保留来源、父子关系、缩进与子任务折叠。 |
| 9/11 视觉一致性 | 看板、待办、规划台、倒数日、设置、编辑器与详情采用一致卡片和间距；保留原看板背景及问候字体。隔离桌面截图与控件操作通过，日历 900/1100/1450 宽度均检查。 |
| 10 日历 | 默认浅底、彩字、侧色条，保留经典白字选项。`desktopCalendarEventStyle` 在 Windows 保存、Android 备份读取和写入时保留。 |
| 12 响应与同步 | 手机提醒 IPC、备份与同步独立在后台执行；自动备份合并连续请求并由应用级单写者处理。运行中的双端每 3 秒检查版本，Android 无变化时使用小响应，15 秒内容审计保留遗漏标记检测；手动同步始终审计。保留离线、并发写入与冲突确认保护。 |

## 验证结果

- Android：229 项单元测试通过；Lint 0 错误 / 134 警告 / 30 提示；Debug、签名 Release 和 AndroidTest 构建通过。Release APK 的 v2 签名校验通过，包名与版本为 `com.paykitodo.app / 1.17.5 / 397`。最终 LAN JavaScript 通过 `node --check`。
- 模拟器共 28 项不同测试通过：原有数据/界面 21 项、账户同步 6 项、清单入口与时间预览 1 项。同步套件失败后的两项定向复测通过，不重复计数；不是声称一次完整执行全部 28 项。最新 Debug 正常启动至 MainActivity。
- 真实 ViewModel 完成操作：上传网络被人为挂起时，本地完成状态已写入；主线程调用 35 毫秒（阈值 1000 毫秒），5 次主线程心跳通过；释放网络后云端任务完成。该值是本机测试的一次测量，不是所有手机延迟上限。
- 独立 Windows 客户端通过实际登录、下载、解密和本地保存链路接收上述 Android 快照；目标事项已完成，另一个事项保持未完成，重开本地存储仍一致，Dirty=false。仅使用本机测试账号与服务器。
- Windows：65 项回归通过（含实际安装 EXE），另 1 项隔离 UIA 流程通过。回归批次中的 Android 桥接因无账号参数跳过，随后按专用参数独立执行 1 项并通过。合计 67 项不同检查，无未解决失败。
- 最终 MSI：安装 1.17.4 → 升级 1.17.5 → 修复 → 483 个负载文件哈希 → 实际 EXE 启动/关闭 → 卸载通过；安装路径、快捷方式与每用户登记正确，用户数据指纹保持一致。
- 交付便携目录的全部 483 个文件与 MSI 负载清单一致；APK/MSI/EXE 版本与下表哈希一致。18 张实际控件图片组成离线预览。

## 失败记录与修正范围

初次编译发现 WPF 类型别名、已不存在的时间属性以及测试反射重载选择错误，已修复。字体测试最初使用文件名式名称而跳过；改用 WPF 实际家族名后完成真实检查。

旧同步测试引用已更名的按钮，并假定后台 dirty 标记会同步完成；另有安全副本恢复自动上传与手动确认争用版本。更新测试标签、等待真实变更代次/新版本并使人工确认场景先进入稳定冲突；没有放宽产品的版本冲突检查。本机 HTTPS 测试服务启用 HTTP/1.1，消除测试响应流提前关闭。最后两项失败定向重跑通过。新增清单截图测试初次引用未安装的 Espresso 依赖，改为现有 Compose 键盘控制 API 后通过。

首次 MSI 验收发现测试向日常 `diagnostics.log` 追加日志。账户存储文件未因此改写。现将测试日志写入专用目录，`--data-directory` 启动的产品日志也跟随指定目录，重建 MSI 后完整复验通过。第一次日志追加保留，不回写或删改历史日志。WiX/JDK 初始路径错误已按本机真实位置更正，不影响最终产物。

## 数据与环境

- 原模拟器 user 0：646/646 文件哈希完全相同，无新增/缺失。
- 既有测试 user 10：148 文件中 143 个字节级一致，无新增/缺失；数据库全部 17 张表逐行一致。变化为启动时间、同步成功/会话到期时间、2 个加密偏好条目、运行 Profile 标记及 SQLite 共享内存；其余偏好条目保留。没有宣称所有凭据文件字节不变。
- 备份：`G:\Workspace\Backups\PaykiTodo\before-feedback-20261028-161008` 与 `after-feedback-20261028-170432`；保留早期失败备份文件。旧交付目录未删除。
- 新建专用 user 11 `PaykiTodo-1.17.5-test` 用于本机合成账户同步。结束时测试应用停止，模拟器回到 user 10 主屏；`emulator-5554 / Pixel_8` 保留运行，使用 `-no-window -no-audio -no-snapshot-save`，没有用户可见镜像。
- 本地测试服务 19443/19444 已停止；Windows 测试进程与测试安装均退出/卸载。未操作生产服务器、推送或发布 GitHub Release。
- 证据根：`G:\Workspace\Scratch\PaykiTodo\feedback-20261028`。主要记录为 `android-ui-tests.txt`、`android-sync-tests-v2.txt`、`android-sync-tests-v3-targeted.txt`、`android-sync-steps.txt`、`android-windows-bridge/`、`installer-tests-v3/`、`preservation-result.json`、`apk-signature.txt`。

## 最终交付

`G:\Workspace\Deliverables\PaykiTodo\1.17.5` 包含签名 APK、Debug APK、MSI、完整 `Windows-portable`、`界面预览.html`、18 张截图、`交付说明.md` 和 `SHA256SUMS.txt`。

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| PaykiTodo-1.17.5-release.apk | 21034734 | `7260031D0107B8646F9381B482F94A9123FE741B00E9D54474A8EC5CF956DE35` |
| PaykiTodo-1.17.5-debug.apk | 42680537 | `758FB0FCED3EC5641CA3248F3BCB9FF97C53818676E10E6F17D4D40EEEB3E515` |
| PaykiTodo-1.17.5-windows-x64.msi | 80172116 | `AFD9B5E6E97CA339C0A0E2923A68AC86CE39878EF68CABD1D89DAABD699F49C5` |
| Windows-portable/PaykiTodo.exe | 218112 | `D2CA8662B2870453571140BA965FF64CCC3CFBB6F84F31AA9E848B32339E913E` |

源码构建路径：`app/build/outputs/apk/debug/PaykiTodo-1.17.5-debug.apk`、`app/build/outputs/apk/release/PaykiTodo-1.17.5-release.apk`、`desktop/installer-output/PaykiTodo-1.17.5-windows-x64.msi`、`desktop/dist/PaykiTodo.exe`（保留整个目录）。

## 边界

未用本次本地测量替代 OEM 手机长待机、锁屏、振动、生产网络延迟或完整物理键鼠操作验证。3 秒是运行进程的检查间隔，不承诺后台被系统限制时仍实时到达。公开 GitHub Release 保持 `v1.15.18`。
