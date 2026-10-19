# 1.16.22 / 387 Windows 排版与不干扰验收

日期：2026-10-19。开始基线 main / 429f6da；Room31 不变。本轮 Android 只更新版本元数据，原账号/提醒/同步实现未重写。没有推送、发布、生产服务器改动或清除用户资料。

## 改动与安全边界

- Windows 统一微软雅黑 UI/Segoe UI、字重层次、导航 glyph/选中标记、输入控件、浅深色中性背景与字段布局。保留原品牌色、看板背景和问候字体。
- 原 ICO 取高分辨率帧；窗口、确认、提醒、账号、AI、隐私使用品牌图标，托盘不再从 testhost/宿主进程取图标。普通弹窗居中 owner，提醒标题/时钟/操作居中；编辑操作固定在滚动表单外。
- 窄看板自动单列，规划台操作固定底部。画面复核发现日期/时间文字被重复 Padding 裁切，即使保存测试通过仍无法阅读；移除重复 PART_ContentHost Margin，并增加文本视口高度断言。规划台窄窗口按钮边界也加入断言。退出后标题改为账号登录。
- 默认独立 Win32 desktop 启动整个测试进程，不 SwitchDesktop、不注入系统输入、不读写剪贴板；测试检查自身 desktop 与输入 desktop 不同。物理输入模式仍单独 opt-in，本轮未运行。
- `--data-directory` 隔离业务数据、凭据、服务器偏好和验证浏览器资料。默认用户目录不变；这不是登录绕过。显式退出仍锁定本地资料，过期仍允许离线，云端请求中的新写入仍保留 dirty。

## 实际测试：不是只启动或看日志

最终复现命令（仓库根目录）：

```powershell
.\tools\Build-PaykiTodoDesktop.ps1
.\tools\tests\Invoke-DesktopAcceptance.ps1 -PackagedExe .\desktop\dist\PaykiTodo.exe -Evidence G:\Workspace\Scratch\PaykiTodo\ui-1.16.22\final-4
```

19 项回归通过：包括18项已有数据/同步/渲染回归，以及1项新增成品 EXE 启动检查。覆盖加密记忆独立于归档、上传/下载中写入、冲突恢复安全副本、过期通知去重、退出锁定、重叠日程、循环删除、打卡/隐藏字段、保存事务、账号设备/密码 API、AI/快照和清单等。成品用例启动真正 `desktop/dist/PaykiTodo.exe`，验证1.16.22.0、窗口名、WM_GETICON、独立资料仍存在及正常退出0；它是启动 smoke，不是又一套完整 E2E。

另1项 UIA 工作流通过，共14阶段（不是14个独立测试）：

| 阶段 | 操作与断言 |
| --- | --- |
| W01 | 原生待办编辑、完成，检查内存及加密磁盘副本 |
| W02 | 输入待办后取消，不得持久化 |
| W03 | 无标题清单的父子任务、说明与注释，验证真实数据 |
| W04 | Enter 续写使用组件 PreviewKeyDown，再取消；不是系统键盘 E2E |
| W05 | 日历创建；UIA 修改日期和10:15/11:45，检查保存值及文字不裁切 |
| W06 | 规划输入、切页、继续草稿、完成保存 |
| W07 | 通过主题下拉 Selection 切换深色 |
| W08 | 深色遍历看板/待办/日历/规划/倒数/报告/设置 |
| W09 | 合成账号过期，真实桌面计时器到期弹提醒，完成后落盘 |
| W10 | 点击退出及确认，本地工作区锁定，磁盘保留任务 |
| W11 | 受控本地云端发生冲突，确认保留本机上传，检查云快照内容 |
| W12 | 确认云端恢复，再用“恢复操作前副本”找回本机更改 |
| W13 | AI 配置/新增服务/可编辑模型控件，嵌套取消不保存草稿 |
| W14 | 退出后登录标题、记忆选项、隐私弹窗打开关闭；不打开外部网页 |

UIA 操作的是生产 WPF 控件及事件处理，不是后台直接调用业务方法冒充点击。账号过期和云冲突由测试夹具设置，不能称为真实公网账号 E2E。保存、取消、导航、磁盘状态、真实计时与对照断言是主要证据；图片只辅助检查排版。

局部失败与修正：最初尝试线程级 SetThreadDesktop 被 COM 窗口占用，改为进程创建时绑定；修正仅属于测试的 WPF peer/API 使用和多 Application 同进程限制。可视审查后来发现日期裁切和规划底栏越界，修复后 review-4 / final-2 / final-3 / final-4 重跑。旧失败证据保留，不用失败运行声称通过。最终 TRX 和步骤：

- `G:/Workspace/Scratch/PaykiTodo/ui-1.16.22/final-4/windows-regressions.trx`
- 同目录 `windows-isolated-uia.trx`、`windows-uia-steps.txt`、`packaged-exe-result.txt`。
- 少量 PNG 覆盖看板浅深色、日历、窄规划、设置、编辑、清单、AI、登录、隐私、提醒与确认；没有持续录屏。最终约15张、总计不到1MB磁盘空间，不是持续截图占用内存。图片是 WPF 内容渲染，不包含完整系统标题栏；图标另有对象/WM_GETICON 断言，不混称物理截图。

## Android 构建与原模拟机

```powershell
$env:JAVA_HOME='G:\Android\Android Studio\jbr'
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- 首次构建成功，后续同命令增量复核成功；35个测试类共186项单测，0失败。Lint：0 errors / 128 warnings，不是零警告。签名 Release `apksigner verify --verbose`：v2 true / 1 signer。
- 使用用户原来的 `emulator-5554` / `Pixel_8` / Android14，没有另开模拟机。先停止应用并备份私有目录，再 `adb install -r` Debug 和 `am start -W` MainActivity；Status ok，包信息387/1.16.22，uiautomator确认应用包、登录及账号入口。未清数据、未输入真实密码、未制作新手机业务夹具。
- 备份：`G:/Workspace/Backups/PaykiTodo/emulator-before-1.16.22-20261019-0956/private-data.tar`，1,227,776 bytes / 53 entries，tar可列目录。备份不提交；升级启动不需要回滚，模拟器保持运行在账号页。
- 本轮未重新跑完整 Android instrumentation、LAN 浏览器或服务器套件。先前逐项手机实际操作/26项仪器测试与其失败边界仍见 [INTERACTIVE_VALIDATION_1.16.21.md](INTERACTIVE_VALIDATION_1.16.21.md)，不能按本轮新测试计数。

## 本地新制品

| 制品（相对仓库根目录） | SHA-256 |
| --- | --- |
| `app/build/outputs/apk/debug/PaykiTodo-1.16.22-debug.apk` | `EBFF103973055E5E8B419C0AB47B6604649D419FF7C7BF208EE617F78869BF79` |
| `app/build/outputs/apk/release/PaykiTodo-1.16.22-release.apk` | `247B4CF564876BC0EDDA8FDF33AA1DE1841BB42593009F93D601BE1BC0AD4ECE` |
| `desktop/dist/PaykiTodo.exe` / 1.16.22.0 | `7464FFFD69D91C8AC9A45CF97D4BE41EA96F687215296F1DC18D7647D99E8070` |
| `desktop/dist/PaykiTodo.dll` / 主程序集 | `A07C4E308F968E4826CDB7C6327B704663040D6D905937887FCDEB05DB899BF7` |

Windows 是无需安装 .NET 的 self-contained win-x64 目录，不是可单独拎走的单文件 EXE；必须保留完整 dist，账号验证仍需要 WebView2 Runtime。EXE是 apphost，UI变化主要在DLL，只对EXE校验不足以确认代码版本。私有配置、签名和截图/日志均不提交。

## 未证明的范围

- UIA 不覆盖真实物理鼠标命中、全局快捷键、剪贴板、系统焦点争用、全部DPI/多屏布局。未据此宣称全方位物理 E2E。
- Windows 真正公网登录/Turnstile、付费AI服务及完整全平台功能对等未验证；农历循环创建、OCR、多时段创建仍有明确差距。桌面提醒需进程运行；睡眠/退出不保证。
- Android 本轮是版本升级和启动 smoke，不冒充重新完成全部账号/提醒/清单验收。真实OEM多日待机、锁屏/震动/厂商电池策略仍需真机；此前模拟器全屏接管/定时再次提醒等未通过项未被改写为通过。
