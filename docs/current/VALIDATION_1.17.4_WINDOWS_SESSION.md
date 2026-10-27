# Windows 1.17.4.0 多电脑登录修复验证

日期：2026-10-27

## 修复范围

- 登录设备身份从加密账号工作区中独立出来，使用当前 Windows 用户下的 DPAPI 保护 `installation-identity.bin`。复制旧 `cloud-workspace.bin` 到另一台电脑时，登录会得到新的设备身份；同一台电脑重启或重新登录仍保持稳定。
- 服务端收到已存在的设备 ID，但设备名或平台不同时，会分配新的设备 ID并保留原设备会话。不同设备 ID 原本就支持并行登录。
- 认证错误保留服务端 `error` 码；刷新令牌失效、访问令牌失效和设备撤销显示不同的提示。诊断日志只记录服务器主机、HTTP 状态、错误码、过期时间和设备 ID 哈希，不记录账号密码或令牌。

## 自动化结果

- `dotnet test desktop/PaykiTodo.Desktop.Tests/PaykiTodo.Desktop.Tests.csproj --configuration Release --no-restore`：57 通过，3 跳过，0 失败。
- `python -m unittest server.test_paykitodo_cloud_server`：13 通过，0 失败。
- 新增回归覆盖：单个安装身份稳定、独立配置目录使用不同身份、复制旧工作区不复用旧设备 ID、服务端复用 ID 但更换设备名时保留两台活动设备。
- MSI 构建：`desktop/installer-output/PaykiTodo-1.17.4-windows-x64.msi`，80,090,176 字节，SHA-256 `29830769257C8CC7B9F5C1C160BA17F5DE2187FA42DFE6EEE65E8D0B4ACB80E2`；便携目录入口 `desktop/dist/PaykiTodo.exe` 文件版本 `1.17.4.0`，SHA-256 `04f56a9d2b86d1d785284a78551976a73f9ee8d31048cdfbbb78e6c2a61026f8`。
- 使用 `msiexec /a ... /qn` 提取 MSI 返回退出码 0；提取目录中的 `LocalApp\Programs\PaykiTodo\PaykiTodo.exe` 文件版本为 `1.17.4.0`。提取验证目录保留在 `G:\Workspace\Scratch\PaykiTodo\installer-audit-261027-1174-extract`。

## 日志与边界

当前工作区未发现另一台电脑的 PaykiTodo 专用历史日志；已有 WebView2 文件属于浏览器运行时缓存，不能说明云端会话原因。修复后桌面端日志位于 `%LOCALAPPDATA%\PaykiTodo\diagnostics.log`。本轮未连接用户另一台电脑，也未执行生产服务器部署；MSI 已生成，但另一台电脑的实际安装和登录仍需用户侧验证。旧版 `1.17.3` 安装包没有删除。
