# Windows 更新资产与启动协议

1.17.11引入GitHub稳定发布检查、后台下载、灰度、文件增量重建和独立启动器。当前稳定[v1.17.14](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.14)已公开，Windows清单范围100%、增量基线1.17.13；七项公开资产与实际下载核对通过。

完整便携ZIP包含启动器及update-files.json；应用内完整更新ZIP不包含该基线元数据和调试符号。1.17.14实际分别为484和482条目，已验签和核对；不能把windows-full.zip当作完整便携安装包。

## 维护者生成资产

先通过 tools/build/Build-WindowsInstaller.ps1 或 tools/Build-PaykiTodoDesktop.ps1 构建完整含启动器的版本目录。使用 PowerShell 7运行：

```powershell
./tools/build/New-WindowsUpdateRelease.ps1 -PayloadDirectory <1.17.14版本目录> -OutputDirectory <新的输出目录> -Version 1.17.14 -RolloutPercent 100 -RolloutSeed PaykiTodo-1.17.14 -BaselineDirectory <1.17.13旧完整目录> -BaselineVersion 1.17.13
# 100代表所有具备更新器的旧版本客户端均在自动检查范围内；调整比例必须重新签名。
```

输出为完整zip、可选增量zip和 PaykiTodo-update-manifest.json。只打包发布程序；本机 client-config.json、调试符号、数据库和认证材料不进入资产。签名清单包含所有目标文件的相对路径、大小和SHA-256、压缩包大小/哈希、版本、灰度比例/种子，以及增量所需基线文件清单哈希。update-files.json写入本地目标目录，为下一次增量提供基线；它自身不进入文件清单。

默认签名私钥在忽略目录 user/update-signing/private-key.pem。公钥随客户端和启动器发布在 update-public-key.txt；两者必须匹配。私钥应保存恢复副本并仅交给授权发布环境；不能提交、输出或上传至Release。工具只生成本地资产，不上传或创建标签。发布时将上述资产附加到对应 vX.Y.Z稳定Release；灰度调整需重新签名清单。

## 客户端约束

仅接收指定项目GitHub API返回的稳定版本和相同标签的HTTPS资产地址，数字比较版本，过滤draft/prerelease。签名使用RSA-PSS/SHA-256，客户端内置公钥。签名覆盖原始UTF-8清单bytes的base64；不重新序列化后验签。拒绝路径越界、链接、重名文件、无效版本/大小/哈希及清单外的压缩包条目。

本机更新目录按基础安装目录的哈希隔离，随机安装标识持久化；账号明文不参与灰度。未命中自动灰度仍可查看更新并手动参与。文件准备在独立staging目录；成功才原子移动到versions/<版本>并设置pending。取消或下载失败不改变活动版本；失败候选和下载文件保留，当前没有自动清理旧缓存。

增量只下载变化文件zip，经验证的未变化文件从当前版本复制；基线不存在/内容变化/增量损坏则下载完整zip。完整目标逐文件校验后可启动。首次安装1.17.11是更新协议起点，之前版本没有此更新器，需先使用完整安装包。

## 切换与回退

MSI快捷方式和便携启动器保持在基础安装目录。启动器选择高于基础版本的pending/active版本，重新验签和验证文件，写入一次性启动nonce。主窗口Loaded在任何联网同步前确认nonce，成功后pending变active。45秒未确认或新进程退出时结束该候选进程并回退；中断启动留下的attempt也会在下一次启动回退。失败版本不自动反复尝试，后续新版或完整MSI可恢复。

PAYKI_UPDATE_SEED携带基础目录，防止每次升级创建新的缓存身份；PAYKI_UPDATE_NONCE用于确认。本地更新目录不保存用户事项，程序始终使用原用户资料目录。MSI修复/卸载只处理安装文件；缓存保留，但基础版本更新后不会选择比基础版本旧的缓存。

协议回归在 DesktopUpdaterTests。实际安装器、启动器进程、Android系统安装权限及APK身份检查的验收边界须以当次VALIDATION文档为准。
