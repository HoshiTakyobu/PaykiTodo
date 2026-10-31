# 公开安装包构建

公开发行包不预置维护者的账号服务器地址或人机验证配置。新安装用户从登录页选择服务提供者给出的服务器；有效的已保存地址继续优先使用。如果旧安装只使用了旧包内置默认值、没有保存地址，升级后需要重新选择原服务器。选择服务器不删除本地事项。

## Android

签名配置保存在忽略的 `user/keystore.properties`，签名文件保存在 `user/release/`。根目录旧路径只作为兼容回退；真实密码、私钥和部署配置不得进入源码或公开资产。

在仓库根目录运行：

```powershell
./tools/Build-PaykiTodoRelease.ps1 -PublicBuild
```

`-PublicBuild` 对 Debug 和 Release 都传入 `-PpaykiPublicBuild=true`，完全跳过本地 `user/cloud.properties`。生成配置使用 `https://paykitodo.invalid` 和空 Site Key；人机验证从用户选择的服务器提供的页面加载。直接使用 Gradle 时，同样明确传入该属性：

```powershell
./gradlew.bat -PpaykiPublicBuild=true :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --console=plain
```

默认本机构建继续读取本地配置。`-SkipDebugBuild` 可保留已经存在的 Debug 输出，因此发布检查应只引用实际以公开模式构建并独立核对过的文件。

## Windows

公开构建不传 `ServerConfig`。确认 MSI 与完整便携目录不含 `client-config.json` 或用户资料。应用内更新工具排除本机配置、调试符号和用户数据；完整便携 ZIP 包含启动器和 `update-files.json`，用途不同于应用内完整更新 ZIP。

签名更新资产按照 [Windows 更新协议](WINDOWS_UPDATE_PROTOCOL.md) 生成。清单中的版本、标签、灰度比例、完整包及增量基线必须与本次资产一致；签名私钥只保存在忽略目录。

## 发布核对

先核对包名、版本、Android 签名与既有公开包兼容性、Windows 文件清单和更新签名，再扫描 APK 的解压内容及 Windows 全部文件，确保没有本机地址、认证或私钥。核对完整包与增量重建后的每个文件，生成 SHA256 清单。

先上传完整草稿并核齐资产，再公开为稳定 Release。发布后下载实际资产并核对 SHA256，检查两端当前更新解析器能识别该版本。保存本地验收、模拟验收与真实公开下载的各自证据，不把下载校验描述成真实手机安装或所有客户端自动升级均已完成。
