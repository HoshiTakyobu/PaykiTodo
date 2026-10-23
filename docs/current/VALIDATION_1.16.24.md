# 1.16.24 / 389 验收记录

完成日期：2026-10-23。上一完成基线为 `main / 6005a10`、Android `1.16.23 / 388`；本轮接续未提交的字体改版和五张批注图，最终 Android `1.16.24 / 389`、Windows `1.16.24.0`、Room `31`。公开 GitHub Release 仍为 `v1.15.18`，本轮只做本地提交，没有推送、发布或修改服务器部署。

需求：[REQUEST_2026-10-22_TYPOGRAPHY.md](REQUEST_2026-10-22_TYPOGRAPHY.md)。恢复来源包含 `01a1676f-703e-7f40-bfbc-3166970a4b68`；仓库、制品和当前测试结果优先于中断会话中的完成声明。

## 最终行为

- Android 与原生 Windows 的标题、导航、用户名和正文使用更纱 UI 无衬线，清刻本悦宋保留在问候文字；手机抽屉统一 Semibold，正文增至 17/15sp。保留原看板素材和图标。Windows 品牌采用 Century Gothic/Century，问候为蓝色。
- Windows 搜索框有图标、输入提示、Enter 提示和 Ctrl+F 入口；设置首块呈现账号图标与用户名，去除重复同步状态，外观后移。快速清单使用完整一体图标。
- Windows 日历具备左侧小月历、选中周连续背景、今日圆圈、固定星期日期栏、GMT 时区、对齐整点的网格和当前时间标签。当前时间线在过去列为红色虚线，今日左侧有红点，今天及之后为实线；定时刷新，保留日/周/月/列表、分组过滤、创建预览和重叠分栏。
- 日历主按钮采用可读的蓝底白字，深色模式不使用浅蓝底白字；键盘焦点与勾选边框跟随日历蓝色，避免交互后回到绿色。
- Windows 待办复选框以前仅监听 Click，UIA Toggle 只改变勾选而没有保存。现在监听 Checked/Unchecked，完成、恢复均写入本地并参与同步；日历分组过滤同样响应状态变化。复选框具有包含任务标题的辅助功能名称。
- Android 提醒页修复透明渐变、系统栏及按钮文字对比度。

## 自动回归和构建

本轮最终证据根目录：`G:/Workspace/Scratch/PaykiTodo/typography-1.16.24/resume-20261023/`。

| 项目 | 实际结果 | 证据 |
| --- | --- | --- |
| Android 单元测试 | 190 通过，0 失败/错误/跳过 | `app/build/test-results/testDebugUnitTest/`、`android-build-final.txt` |
| Android Lint | 0 errors，131 warnings，28 information | `app/build/reports/lint-results-debug.xml` |
| Android 构建 | Debug、签名 Release 成功；既有版本从上一完成轮次 1.16.23 升到 1.16.24 | `android-build-final.txt` |
| Windows 回归 | 22 通过，包含真实控件切换完成后重载本地存储的断言、字体解析、日历轴/时区、成品启动等 | `final-colors/windows-regressions.trx` |
| Windows UIA 流程 | 1 条流程、14 阶段通过；不是 14 个独立 E2E | `final-colors/windows-isolated-uia.trx`、`windows-uia-steps.txt` |
| Windows 制品 | self-contained win-x64 发布成功；正式 EXE 的独立 profile 启动、图标、正常退出通过 | `windows-publish-colors-final.txt`、`final-colors/packaged-exe-result.txt` |
| Release 签名 | apksigner verify 通过，APK v2、1 个 signer | SDK build-tools 34.0.0 |
| Android 实机运行面 | 复用 `emulator-5554 / Pixel_8`，原地安装最终 Debug，核对 1.16.24 / 389 | 下述实际操作记录 |

复现入口：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --console=plain
.\tools\Build-PaykiTodoDesktop.ps1
.\tools\tests\Invoke-DesktopAcceptance.ps1 -PackagedExe .\desktop\dist\PaykiTodo.exe -Evidence G:\Workspace\Scratch\PaykiTodo\typography-1.16.24\resume-20261023\final-colors
```

Windows 自动测试在独立 Win32 桌面运行，未接管全局鼠标、键盘或剪贴板。正文、抽屉、账号、编辑器、浅深色日历、窄窗口和提醒均有渲染截图；最终实际 EXE 也核对了日历、账号顺序、搜索结果与清空提示。

## 真实账号与客户端操作

实际客户端使用专门测试账号及 `G:/Workspace/Scratch/PaykiTodo/real-user-audit-20261022/windows-profile`，没有改写用户的正式 Windows profile。2026-10-22 的手机人机验证由用户手动完成，之后正常登录；本次沿用登录状态，不把人机验证记为自动通过。

1. 上次中断前通过正式 Windows 编辑器创建 `Resume acceptance 20261023`。手机出现本机/云端均有更新的保护提示；中断会话已在另存测试数据后选择测试云端副本，手机收到任务。本次没有替用户选择个人数据冲突。
2. 本次在手机真实编辑器给任务标题增加 ` - Android verified` 并点完成。Windows 自动拉取版本 14，显示修改后的标题。
3. 初次 UIA 勾选没有保存，定位为 Click/Toggle 事件差异；修复并重建后，通过正式 EXE 复选框完成任务，手机自动出现相同标题的“已完成”历史。另实际切换恢复，再次完成，验证两个方向的持久写入。
4. 在正式 Windows 编辑器创建 `Resume timed reminder 20261023`，设置 DDL 为 2026-10-23 15:27、提醒偏移 0 分钟。手机自动收到任务，显示 07:27；Windows 为 Asia/Shanghai，模拟器为 UTC，两者是同一个时间点。
5. 到点后 Windows 出现 `PaykiTodo 提醒` 窗口，手机通知栏出现相同任务通知。点击手机通知进入实际 ReminderActivity，核对标题、备注、时间、完成/知道了/延后按钮与深色对比度。
6. Windows 点“我知道了”，手机点“我已完成”；Windows 自动拉取版本 18，任务进入已完成历史。最终配色修改只涉及渲染，随后重建并重复完整 Windows 回归、UIA 和正式 EXE 搜索检查，已完成任务跨重启保留。
7. 最终 Debug 安装后核对 Android 浅深色设置和抽屉；最后停止应用并恢复原私有数据。

代表性证据：

- `real-user-audit-20261022/resume-android-edit-received.png`
- `resume-20261023/android-completion-received.png`
- `real-user-audit-20261022/resume-timed-reminder-created.png`、`resume-timed-reminder-fired.png`
- `resume-20261023/android-timed-reminder-notification.png`、`android-final-reminder-opened.png`
- `real-user-audit-20261022/resume-reminder-completion-received.png`
- `real-user-audit-20261022/final-packaged-calendar.png`、`final-packaged-search.png`、`final-packaged-settings.png`
- `resume-20261023/final-colors/windows-calendar-current-{light,dark}.png`

这些是指定流程的实际操作证据，不等于全部 Android/Windows 功能或厂商行为验收。

## 数据恢复、初次失败和边界

- 原手机备份：`G:/Workspace/Backups/PaykiTodo/emulator-before-real-user-audit-20261022-174553/`。恢复前另存本轮最终测试数据到 `G:/Workspace/Backups/PaykiTodo/emulator-after-1.16.24-20261023-153057/`。
- 恢复后逐文件核对 **557/557** 原始 SHA-256 匹配，78 个额外运行/测试文件保留，没有删除文件。记录：`resume-20261023/emulator-restoration.json`。模拟器保持运行，应用及测试包停止，避免恢复后马上改写校验状态。
- 早期字体字节测试遇到异步监听启动竞态；等待端口就绪后再连接，并保留超时。最终 190 单测通过，先前失败日志保留在 `typography-1.16.24`。
- 恢复工具初次以 `adb shell` 传二进制流报 EOF/Illegal seek，未当作恢复成功；改用 `adb exec-in` 后完整恢复并校验。原备份与恢复前测试备份均保留。
- 独立进程 UIA 主题下拉辅助脚本曾遇到弹出项失效/不支持的 SelectionItem 模式；该次没有算通过。浅深色切换与最终渲染由隔离控件流程验证，最终正式 EXE 的导航、搜索、重启持久状态另行通过。
- 未使用的思源宋体试验文件及许可证共三项已从源码资源目录移到 `Scratch/PaykiTodo/typography-1.16.24/superseded-font-assets/`，完整哈希核对；清单在 `Notes/workspace/paykitodo-1.16.24-font-moves-20261023.json`，没有删除。这些资源未进入最终 APK，也不提交到源码。
- 本轮没有重跑全部 Android instrumentation 或 LAN 浏览器套件，不把 1.16.23 的 32 个设备测试算成本轮结果。真机 OEM 长待机、锁屏自动全屏、振动、厂商语音和小组件仍需真机验收；本次手机全屏证据来自点击到点通知。Windows 农历/OCR/多时段编辑等原有功能差异仍在，提醒需要客户端运行。

## 最终制品

| 制品 | 路径 | SHA-256 |
| --- | --- | --- |
| Debug APK | `app/build/outputs/apk/debug/PaykiTodo-1.16.24-debug.apk` | `9ABD9DEE69535B6194E3E21649F9D27475286C0BC0CFFD0AC109929F6CDF6921` |
| 签名 Release APK | `app/build/outputs/apk/release/PaykiTodo-1.16.24-release.apk` | `CE61F3FF24DC4743763806C09CEAF793671957C5FC9C758B408A13788EB3C9DA` |
| Windows EXE | `desktop/dist/PaykiTodo.exe` | `E41C698CE44717EFA7EB62F51A571B1FE007B077A69F19430E0616E21838A6E0` |
| Windows 主程序集 | `desktop/dist/PaykiTodo.dll` | `60464C773B2D3F0F2FCAF57E5015361FA6E145DC9ACC0770CBA1D88E124DF95F` |

均在本次接续中重建。Windows 必须保留完整 `desktop/dist`；EXE apphost 的哈希不能单独识别界面代码。私有地址、测试凭据、签名材料、备份和含测试账号的截图只留本机，不进入 Git。
