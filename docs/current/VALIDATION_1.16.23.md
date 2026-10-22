# 1.16.23 / 388 验证与交付记录

2026-10-22 完成 [七项请求](REQUEST_2026-10-22.md)的本地实现和验收。起点为 `main / 8bbb136`、`1.16.22 / 387`；当前 Android 为 `1.16.23 / 388`，Windows 为 `1.16.23.0`，Room 保持 `31`。公开 GitHub Release 仍为 `v1.15.18`。

本轮先在会话 `01a1626c-654a-77d0-b797-d364204bcc42` 中实现、构建及测试；会话中断后核对原始结果、源码、制品和恢复记录，再完成文档与本地提交。此次收尾没有重新构建产品安装包，也没有把历史测试当作新运行。

## 行为与验证范围

| 请求 | 实现与实际验证 |
| --- | --- |
| 到点提醒可靠性 | Android 启动、系统恢复路径重新登记之前已排定、近 10 分钟内未送达的提醒，保留派发去重；先持久化触发身份，再调用可能立即送达的 AlarmManager。设备测试验证近期提醒恢复一次、过期账号离线后台提醒、历史记录不补响。 |
| 自动同步与旧数据 | 本机修改、完成状态和上传中的新写入会继续上传；内容指纹发现遗漏的 dirty 标记。没有旧版核对记录且副本不同时持续等待确认，后续轮询不能绕过冲突。Android 真实本地 HTTPS 服务验证断网恢复、延迟请求、重新登录与双方恢复副本；Windows 回归验证独立副本接收完成状态和重复轮询保护。 |
| Windows 字体、排版与入口 | 内置 Sarasa UI SC Regular/Semibold，紧凑任务布局，移除设置之外底部重复的账号入口；真实 WPF 画面及控件检查通过，保留看板素材和问候字体。 |
| 分组对象字符串 | 分组选项绑定名称，实际待办页显示“全部分组”，不再显示 `DesktopGroup { ... }`。 |
| 过去的 DDL 与提醒 | Android 编辑器、规划导入和 LAN 写入允许保存过去的待办、日程与自定义提醒；仍校验日程结束不早于开始。Windows 只送达提前登记的提醒，新建或下载的历史记录不补发。单测、设备与 Windows 回归覆盖相关保存/调度行为。 |
| 三端字体 | Android Compose、原生 Windows、LAN Web 和 Wiki 使用内置字体；网页回归显式检查两种字重加载，Windows 检查资源解析。SIL OFL 许可随资源及 Windows 分发目录保留。 |
| 中文启动与任务入口 | 桌面名称“派期待办”，快捷方式“我的待办”“添加待办”，`paykitodo://tasks` 和 `paykitodo://tasks/new` 保留账号门禁。设备测试验证隐式深链接进入新任务编辑器；新增入口不直接写入任务。厂商语音识别尚无真机证据。 |

手机仍显示较新数据、电脑副本过旧时，操作说明见 [SYNC_RECOVERY_1.16.23.md](SYNC_RECOVERY_1.16.23.md)。本轮使用合成测试账号验证同步逻辑，没有替用户选择或覆盖真实账号的冲突副本。

## 测试结果

| 范围 | 结果 | 原始证据 |
| --- | --- | --- |
| Android JVM | 190 项，0 失败、0 错误、0 跳过，共 36 个类 | `app/build/test-results/testDebugUnitTest/TEST-*.xml` |
| Android Lint | 0 errors，131 warnings | `app/build/reports/lint-results-debug.txt` |
| Android 设备 | 32 个不同用例的最后一次结果全部通过；分批复验，非单次全绿 | 下述 instrumentation 日志 |
| Windows 回归 | 最终 22 项全部通过，含真实成品启动 | `final-4/windows-regressions.trx` |
| Windows 原生控件 | 1 条流程、14 个阶段全部通过 | `final-4/windows-isolated-uia.trx`、`windows-uia-steps.txt` |
| LAN Web | 无标题清单、8 类弹窗、取消、过期响应、断连与重连错误通过；两种字体加载断言通过 | `web-final.txt`、`tools/tests/desktop-web-connection.cjs` |
| 产品制品 | Debug、签名 Release、Windows 自包含目录已生成；Release APK v2 签名有效，1 个签名者 | 下述版本、哈希及制品路径 |

本轮设备/桌面/网页证据目录：`G:/Workspace/Scratch/PaykiTodo/acceptance-1.16.23/`。原始日志、个人数据、测试账号、截图与数据库不进入 Git。

Android 使用现有 `emulator-5554 / Pixel_8`、Android 14。初跑 `android-instrumentation.txt` 为 30/32；深链接生命周期跟踪和断网后的测试探针分别失败。保留深链接原始 URI，修正临时探针重试后复验；中间一轮又触发本地测试服务注册限流，仅重启该测试服务。`android-account-reminder-final-2.txt` 为 10/11，最后失败来自把编辑页标题“新增任务”误写成“新增待办”的测试断言；修正后 `android-deep-link-final.txt` 为 1/1。逐项取最后一次结果得到 32/32，失败日志仍保留。

Windows 最后一次运行使用：

```powershell
.\tools\tests\Invoke-DesktopAcceptance.ps1 -PackagedExe .\desktop\dist\PaykiTodo.exe -Evidence G:\Workspace\Scratch\PaykiTodo\acceptance-1.16.23\final-4
```

测试进程位于独立 Win32 桌面，不切换用户桌面，不注入全局鼠标/键盘，不使用剪贴板。14 阶段覆盖创建/取消、无标题清单、日期时间保存及裁切、规划草稿跨导航、主题与七个页面、AI 弹窗取消、冲突确认/恢复副本、过期离线提醒和退出锁定。UIA 控件流程、局部键盘事件调用和成品启动各有不同覆盖范围，不等同于物理输入及账号人机验证的完整 E2E。

最终修正 Windows 结果文字及 LAN 测试页面中写死的旧版本号，使其读取真实成品/源码版本；随后 Windows `final-4` 和 `web-final.txt` 均通过。

## 制品

| 制品 | 路径 | 字节 |
| --- | --- | ---: |
| Debug APK | `app/build/outputs/apk/debug/PaykiTodo-1.16.23-debug.apk` | 42113832 |
| 签名 Release APK | `app/build/outputs/apk/release/PaykiTodo-1.16.23-release.apk` | 20762154 |
| Windows 启动程序 | `desktop/dist/PaykiTodo.exe` | 196096 |
| Windows 主程序集 | `desktop/dist/PaykiTodo.dll` | 30583296 |

SHA-256：

```text
Debug   08706ADD2BB439FDCA2AE30A99A75C8C9911DAA704E4BF8A864EF70EC3EBFC5D
Release BED86B8E0898EAC07AF404F572F9D252EADF2D6A2930896BE83A461A0AE1E806
EXE     B0215B297697AB8E2A52FAE92B12F93EF7FB0B7BBD4EDBDB90DEED6B3912DC92
DLL     83462BCC0FACE842DAB62FAED7B8510F7E6A7D7F99871C9FD7EC8033BDF832B0
```

收尾时重新计算以上四个哈希，均与构建记录 `artifact-hashes.json` 一致；Release 元数据和模拟器已安装包均为 `com.paykitodo.app / 1.16.23 / 388`，应用标签为“派期待办”。Windows 文件版本为 `1.16.23.0`。Windows 必须分发完整 `desktop/dist/`，包含 DLL、运行依赖与字体许可，单个 EXE 不能代表全部程序内容。

## 数据恢复与环境

- 设备测试前备份：`G:/Workspace/Backups/PaykiTodo/emulator-before-1.16.23-20261022-151201/private-data.tar`；SHA-256 为 `3CEF8FA59F14474736517B87881BD6FB38D3C269143E91E35D6595532135CFB3`。
- 前一会话已恢复原文件。一次早期比较中 WebView HTTP 缓存有差异；应用和测试包停止后的比较为原 531 个文件全部匹配，包含数据库和账号设置。2026-10-22 17:28（Asia/Shanghai）再次只读核对仍为 531/531、0 缺失、0 不匹配，见 `resumed-data-verification.json`。
- 测试新增的 26 个运行文件继续保留，未借数据恢复静默清理。此次接续没有再次恢复、清除或改写模拟器业务数据。模拟器仍运行，应用和测试包在最终比较时未运行；设备保留本轮 Debug 包。
- Windows 使用 Scratch 中的独立 profile，正式用户资料不参与合成测试。临时测试服务已停止，ADB 转发列表为空。

## 剩余边界

- 尚未证明真实 OEM 长待机、锁屏全屏、重启后厂商电池策略、物理振动和桌面小组件表现，也不能保证系统停止进程后 15 秒内同步。
- 同步仍为整份加密快照，冲突需明确选择；没有逐字段合并，也没有替真实用户决定哪一份数据正确。正常运行的联网客户端约每 15 秒核对云端，“已核对云端版本”不表示其他设备一定已上传。
- Windows 提醒要求进程运行；农历/OCR/多时段创建、完整真实账号人机验证和付费 AI 服务仍有既有验证缺口。
- 未推送、打标签、发布 GitHub Release 或修改生产服务器。
