# PaykiTodo Windows 客户端

Windows 客户端以原生 WPF 提供每日看板、待办、日历、规划、倒数日、AI 报告、提醒与设置。它有独立本地副本，通过账号服务器与 Android 同步；手机无需持续在线。WebView2 用于账号人机验证、报告全文与离线帮助；报告列表摘要使用轻量原生排版。

用户操作见 [Windows 使用说明](../docs/WINDOWS_USER_GUIDE.md)，客户端“设置 → 常规 → 使用说明”或 F1 可离线打开同一份文档。安装方法见 [MSI 说明](installer/README.md)。

## 当前版本

源码为 1.17.16.0。日程编辑采用主题占位、融入式输入与单行起止日期时间，时区统一进入设置，保留已有事项时区。12 色按两行六列显示，分组与日历配有色标。每日看板条目同高，详情随内容收缩，报告列表直接呈现 Markdown 摘要；设置采用顶部横向分类，默认更纱黑体 UI。桌面周视图始终七天，最小宽度随字号计算，紧凑工具栏与日期同排，列表使用左侧日期轨。既有临时规划、提醒、账号同步、OCR、备份与更新行为保持。

每日看板提供本地／可选 AI 临时规划，预览确认后导入，不保存新文档；支持课程多时段。循环课程的相对分钟提醒每次生效，绝对时刻只归属整门课中开始时刻不早于该提醒的最近实例，修改预览后实时重算。窗口外拥有者提前生成，补齐和系列编辑保留各实例自己的单次绝对提醒。设置内容靠左，分类导航采用同底色，字体支持中文名称与12—22 px字号预览。日报与手机共用提示规则，生成满168小时过期。支持GitHub稳定版本检查、签名后台/灰度/增量更新与启动回退。原有日历、OCR、账号同步和备份能力保持。

最新公开版本为[v1.17.16](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.16)，提供[MSI](https://github.com/HoshiTakyobu/PaykiTodo/releases/download/v1.17.16/PaykiTodo-1.17.16-windows-x64.msi)和[完整便携ZIP](https://github.com/HoshiTakyobu/PaykiTodo/releases/download/v1.17.16/PaykiTodo-1.17.16-windows-x64.zip)。Windows签名更新范围100%，增量基线1.17.14，其他基线使用full回退；完整便携487条目、应用内full 485条目，用途不同。公开包不提供可用预置服务器，首次登录自行选择，已有选择继续优先。

## 开发与构建

需要 Windows 10 1809+/Windows 11 x64 和 .NET 9 SDK。主工程是 PaykiTodo.Desktop/，测试工程是 PaykiTodo.Desktop.Tests/。

在仓库根目录执行：

```powershell
dotnet build desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj
.\tools\Build-PaykiTodoDesktop.ps1
```

第二条脚本生成完整的 self-contained win-x64 目录到 desktop/dist/，入口为 PaykiTodo.exe。便携分发必须保留整个目录。可用 -FrameworkDependent 构建依赖已安装 .NET Runtime 的版本。

本地构建脚本可以从忽略的用户配置读取服务器默认值。公开分发前须检查生成的 client-config.json 和所有配置，不得携带私有部署地址、账号或凭据。MSI 公共构建可以不传 -ServerConfig，由用户在首次登录选择服务器。

```powershell
pwsh -File tools/build/Build-WindowsInstaller.ps1 -WixPath <wix.exe路径>
```

MSI 需要 WiX 4.0.6 和 WixToolset.UI.wixext/4.0.6，输出到 desktop/installer-output/。脚本使用全新 payload 目录并输出文件哈希清单。

## 数据与界面约定

- 默认用户目录为 %LOCALAPPDATA%/PaykiTodo。测试与独立工作区使用 --data-directory 指向专用目录，隔离数据、凭据、服务器选择和验证浏览器资料。
- 普通编辑在“完成”前只保留草稿，关闭或取消不提交。同步中的独立修改可以合并，真实冲突保留明确恢复选择及安全副本。
- 手机的“连接电脑”是独立局域网浏览器入口，直接操作手机数据；它不是 Windows 账号同步的替代路径。
- 提醒依赖桌面程序保持运行。Android 通知、锁屏、权限和小组件由手机端管理。
- 主字体和字号影响正文、编辑器和常规控件；品牌、导航图形和问候字样沿用固定设计。字体资源遵守各自许可。
- 内置说明直接嵌入 docs/WINDOWS_USER_GUIDE.md；变更用户流程时更新该文件并重新构建。

## 验证

```powershell
.\tools\tests\Invoke-DesktopAcceptance.ps1 -Evidence <专用证据目录>
.\tools\tests\Invoke-DesktopAcceptance.ps1 -PackagedExe <完整便携目录中的PaykiTodo.exe> -Evidence <专用证据目录>
```

默认验证在独立 Win32 桌面中使用 UI Automation，不接管日常鼠标、键盘或剪贴板。直接 dotnet test 会跳过需要隔离的界面用例。实物输入模式必须另行安排。

MSI 生命周期验证使用 tools/tests/Test-WindowsInstaller.ps1。它拒绝覆盖已有用户安装，在专用目录验证升级、修复、已安装 EXE、卸载及用户数据保留。测试结果区分数据回归、控件流程、打包 EXE 启动与安装生命周期，不将某一项通过作为所有用户场景都正常的保证。

真实用户数据、签名材料、服务地址和令牌不能进入源码、公开日志或测试截图。具体轮次证据见 docs/current/ 下对应验收记录。
