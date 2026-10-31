# 1.17.14 公开安装包验收

Android `1.17.14 / versionCode 406`、Windows `1.17.14.0`，Room 保持 `34`。本轮将 Android 公开构建与本机部署配置隔离，按新版本重建双端安装包和 Windows 签名更新资产。1.17.13 的规划、提醒与双端体验功能验收继续见 [既有记录](VALIDATION_1.17.13.md)。

## Android

使用 `-PpaykiPublicBuild=true` 完成单元测试、Lint、Debug、AndroidTest 与签名 Release 构建，日志最终为 `BUILD SUCCESSFUL in 4m 26s`。48 份单测结果共 270 项，失败、错误及跳过均为 0；Lint 为 0 错误、137 警告、30 信息。输出时间与本次构建窗口一致。

实际 Debug 和 Release BuildConfig 均为 1.17.14/406，使用占位默认地址和空 Site Key。只读复核确认已保存的非空服务器选择继续优先；选择、取消、持久化和从所选服务器加载人机验证的数据流保持。新安装或未保存地址的旧安装需要自行选择有效服务器，不能承诺继承此前只存在于旧包中的默认地址。

APK 包名均为 `com.paykitodo.app`，最低 API 26、目标 API 34。正式 APK 独立验签有效，与实际下载的公开 v1.15.18 APK 证书一致。正式证书 SHA256 为 `34e6094bfd8e14d675fcfed2f06fc121500349f9b45d4d0b84f9810399970610`。两 APK 内共用报告、规划及网页资源与源码逐字节相同。

## Windows

新 GUID 输出目录为 `desktop/obj/installer/1.17.14-0697f0cea1af41289cdd3fc027ff4168/payload`。MSI 清单、484 文件便携目录、484 条目完整便携 ZIP 逐项匹配；实际程序版本为 1.17.14.0。构建未传 ServerConfig，公开包无 client-config.json、私有文件类型或链接。WebView2 安装器的 Microsoft 签名有效。

两个实际程序包测试在独立 Win32 桌面通过，0 跳过：打开带品牌的窗口并使用独立数据；更新候选在联网之前确认启动 nonce。未启用物理输入验收。

RSA-PSS/SHA256 更新清单验签通过，范围 100%、seed 为 PaykiTodo-1.17.14。完整更新 ZIP 为 482 条目，1.17.13 至 1.17.14 增量为 4 条目；每个 ZIP 条目内容核对，以及使用真实 1.17.13 基线字节重建目标的 482 文件均通过。生成前后 update-files.json 字节相同。其他基线使用完整更新回退。

## 隐私与资料保护

对 7 项拟公开安装、更新及校验资产扫描 APK/ZIP 解压内容和 MSI 原始元数据，SHA 去重后核对 1171 个内容。已审私有地址、实际凭据/私钥、具有文本含义的禁止引用残留均为 0。6 条初始模式命中全部独立分类：5 条为 Microsoft .NET 语言区域表的前缀碰撞，1 条为压缩 MSI 字节丢弃无效编码后产生的虚假单词，实际连续 UTF8/UTF16 文本中不存在。详细上下文只保存在项目外的私有证据目录。

旧 1.17.13 交付目录 494 文件与构建前清单逐字节一致，旧安装包和本机配置保留。本轮没有操作真实手机或模拟器，也没有修改普通 Windows 用户资料。源代码、Release 文本与公开资产分别审计；本地资产核对不等同已公开发布。

## 成品与证据

| 成品 | 字节 | SHA256 |
| --- | ---: | --- |
| Android Release | 21084288 | 5e7ab59acd2ef330645cc1de222266d0a3004b3a121fad5b14a34ee2878f9b73 |
| Android Debug | 40857983 | 04f08058ccb814c2c43b0433c928dac3f1191f9cc0e4e118fbba515b8d998a3d |
| Windows MSI | 106194028 | ef7ade4ec18f36d0a51e9850ea8fd5c471c6b9ca2c65bc9cb48dab1e1ab8f716 |
| Windows 完整便携 ZIP | 130213983 | 60395d033282e0f292381533b62df3e167098fd3faa047776aabafe2a933f710 |

安装包输出为 `app/build/outputs/apk/release/PaykiTodo-1.17.14-release.apk`、对应 Debug 及 `desktop/installer-output/PaykiTodo-1.17.14-windows-x64.msi`。完整交付在 `Deliverables/PaykiTodo/1.17.14`。公开便携资产命名 windows-x64.zip，本地同内容文件命名 windows-portable.zip。

证据在 `Scratch/PaykiTodo/release-publication-1.17.14`：android-artifact-verification.json、android-config-review.json/md、artifacts-windows.json、packaged/windows-regressions.trx、publish-preflight-result.json 及资产清单。

本轮没有重跑旧完整功能/设备套件；不把构建、静态配置复核或下载校验扩展成真实手机覆盖安装、人工验证码、OEM 后台/锁屏行为、所有电脑升级或 MSI 完整生命周期的新验收。实际公开发布、真实下载和更新识别结果单独补记。
