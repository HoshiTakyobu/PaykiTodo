# PaykiTodo Windows 客户端

PaykiTodo Windows 客户端有两个明确分开的模式：

## 独立云客户端

这是正式桌面客户端，不依赖手机在线，也不使用手机的 `18765` 端口。

1. 运行 `PaykiTodo.exe`。
2. 选择“独立云客户端”。
3. 填写部署后的 HTTPS 云服务器地址、账号和密码。
4. 首次使用可注册账号；已有账号选择登录。
5. 客户端把完整的工作副本保存在当前 Windows 用户的本地应用数据目录，离线时仍可新增、修改、完成、取消和删除事项。
6. 网络恢复后自动同步。两台设备同时修改时不会静默覆盖，而是提示先处理冲突。

本地账号令牌和工作副本使用 Windows DPAPI（当前用户范围）保护。服务器只保存端到端加密的迁移包，不能读取任务标题、地点、备注或 AI 密钥。

## 手机 LAN 兼容模式

这是旧的“连接电脑”路径，不是云客户端：

```text
电脑浏览器 / WebView2 -> 手机:18765 -> 手机本地 Room 数据库
```

手机必须主动开启“连接电脑”，然后把手机显示的地址和访问密钥填入客户端。该模式直接修改手机数据，不建立 Windows 本地副本；服务器云同步若已登录，仍由手机自己的同步逻辑负责上传。

## 构建

在仓库根目录运行：

```powershell
tools\Build-PaykiTodoDesktop.ps1
```

默认输出 self-contained `win-x64` 到 `desktop\dist\PaykiTodo.exe`，因此无需安装 .NET Runtime；仍需安装 Microsoft Edge WebView2 Runtime。使用 `-FrameworkDependent` 可生成体积更小但依赖 .NET 运行时的版本。

## 部署边界

- PaykiTodo 云 API / 管理后台默认只监听服务器 `127.0.0.1:18766`。
- 公网只通过 Cloudflare/Nginx 的 HTTPS `443` 访问。
- 手机 LAN 服务仍是手机自己的局域网 `18765`。
- 真实域名、证书、服务器 IP、管理员密码和环境变量必须保存在仓库外。
