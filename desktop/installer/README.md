# Windows 安装包

运行 `PaykiTodo-<版本>-windows-x64.msi`，按向导安装，然后从桌面或开始菜单打开 PaykiTodo。

- 支持 Windows 10 1809及以上版本、Windows 11 x64，默认安装到当前用户的 `%LOCALAPPDATA%\Programs\PaykiTodo`，无需管理员权限。已包含 .NET 运行环境。
- 首次登录需要网络和账号验证组件 Microsoft Edge WebView2。若系统缺少组件，应用会提供“安装登录组件”入口，使用随包附带的微软官方安装器联网安装。
- 办公室电脑登录同一账号后，通过账号同步获取手机数据；不需要手机局域网服务。已有本地数据时先核对云端差异再恢复。
- 新版 MSI 可直接升级旧安装。Windows“设置 → 应用 → 已安装的应用”可卸载；升级和卸载均保留 `%LOCALAPPDATA%\PaykiTodo` 用户数据。安装程序不携带开发机的账号、密码或数据库。
- 安装文件在本地构建，尚未签署 Windows 商业代码签名。管理员策略禁止未签名软件时需由办公室 IT 放行。

构建：安装 .NET 9 SDK、WiX 4.0.6 与 `WixToolset.UI.wixext/4.0.6`，在项目根目录运行：

```powershell
pwsh -File tools/build/Build-WindowsInstaller.ps1 -WixPath <wix.exe路径>
```

可用 `-ServerConfig <本地client-config.json>` 指定用户的服务器默认地址；只接收 `serverUrl` 字段。此配置及成品不进入 Git。脚本从全新发布目录收集清单，不读取用户资料目录。制品输出到 `desktop/installer-output/`。

实现依据：[WiX Package](https://docs.firegiant.com/wix/schema/wxs/package/)、[WixUI](https://docs.firegiant.com/wix/schema/ui/wixui/)、[微软 WebView2 分发说明](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution)。
