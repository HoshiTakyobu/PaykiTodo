# v1.17.16 正式发布核对

已公开稳定 [v1.17.16](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.16)，Release ID `434260717`；GitHub latest、draft=false、prerelease=false 已实查。发布时间为 `2026-11-04T18:03:56Z`（UTC）。Android 1.17.16 / versionCode 408、Windows 1.17.16.0、Room 34。

## 源码与授权

本轮用户要求接收上一 agent 的任务、更新文档、提交 GitHub 并提供手机和电脑安装包，取代实施轮的暂不提交限制。接收分支 ui/1.17.16-layout-refinement，从 9452d19 增量完成中文功能提交 `10cae66` 与独立目标归档 `a29b942`，main 正常快进推送，无强制推送或历史重写。发布标签指向 `a29b94258dadf5c0b29f9896e6576755a3d88c74`；[对应 CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/runs/41882369044) 成功。

推送前扫描包括前次未推送的 1.17.15 提交，共 3 个提交、707 个不同内容；已审私有地址、凭据与不应入库的参考资料无未解决命中。工作树审计排除原无关 `docs/goals/PaykiTodo_规划台实现Prompt.md`，该文件保持本地未跟踪。最终发布文档单独提交，不移动发布标签。

## 安装与更新资产

复用前一 agent 已验收的 1.17.16 APK / MSI；本轮没有重建，因此不新增应用版本。重新核对两 APK 的包名、版本、公开占位配置、签名与共享资源；正式证书 SHA-256 为 `34e6094bfd8e14d675fcfed2f06fc121500349f9b45d4d0b84f9810399970610`，与旧正式版相同。原交付 492 文件及 MSI 清单与 487 文件最终 payload 完全匹配。

| 公开资产 | 字节 |
| --- | ---: |
| `PaykiTodo-1.17.16-release.apk` | 21164254 |
| `PaykiTodo-1.17.16-windows-x64.msi` | 106402173 |
| `PaykiTodo-1.17.16-windows-x64.zip` | 130854990 |
| `PaykiTodo-1.17.16-windows-full.zip` | 134503681 |
| `PaykiTodo-1.17.14-to-1.17.16-windows-delta.zip` | 50605526 |
| `PaykiTodo-update-manifest.json` | 88334 |
| `SHA256SUMS.txt` | 613 |

新完整便携 ZIP 包含 487 个文件；应用内 full 为 485 个文件；1.17.14 → 1.17.16 delta 为 7 个变化文件。RSA-PSS/SHA-256 验签、完整 ZIP 每项字节与基于真实 1.17.14 目录的增量重建均通过。Windows 更新范围 100%，其他基线走 full。原 APK / MSI 字节保持不变，未使用过时 desktop/dist。

公开内容扫描覆盖所有 APK / ZIP 成员与 MSI 原始元数据，去重检查 1184 个内容；MSI 文件清单与便携载荷同时核对。7 个原始模式命中均为已核实的 .NET 区域表前缀或压缩字节解码假阳性，没有真实密钥、凭据、私有地址或参考资料残留。扫描证据只在 Scratch，未提交敏感上下文。

先创建完整草稿、上传七项并核对大小 / GitHub digest / 发布说明，再公开。公开后从真实 GitHub Release 下载全部 443619571 字节，七项 SHA-256 与本地再次匹配。两个大 ZIP 的 urllib TLS / 读取超时后，改用 GitHub CLI 成功下载；失败的部分下载和错误记录保留。

## 验收及边界

既有 [布局验收](VALIDATION_LAYOUT_1.17.16.md) 的 276 Android 单测、15 项设备检查、Windows 142 通过 / 3 个外部夹具跳过保持；本轮重新核对保留测试记录，不称为重跑。公开源码 CI 另外执行本次提交的单测与 Debug 构建。

发布后 Android 调用当前编译的生产 GithubRelease.parse；Windows 调用交付 DLL 的 ReadRelease、Verify、InRollout、CheckAsync 和 PrepareAsync。两者均使用未经改写的真实发布 JSON：1.17.14 可发现正式 1.17.16，本版不误报更新；Windows 1.17.14 首选 delta，1.17.12 / 1.15.18 首选 full，生产验签及 100% 范围成立。更新选择用离线 HTTP 提供器返回真实 API / 清单，在 ZIP 请求前停止；实际网络下载另行完成，不等于安装升级或启动切换验证。

发布前第一版模拟夹具只改 draft=false，保留 GitHub 草稿的 untagged URL，导致两端解析器正确拒绝。修正模拟夹具的预期正式 URL 后通过，未修改产品；正式发布后重新用真实响应原字节验证。CI 普通列表曾短暂未显示新运行，通过 head_sha 查询找到了对应运行，没有改动工作流。

本轮没有启动模拟器、操作实体手机、登录真实账号、部署服务器，未重复 MSI 安装 / 卸载或多显示器 DPI 验收。公开包仍需首次选择有效账号服务器；已有非空选择继续优先。

## 交付

`G:\Workspace\Deliverables\PaykiTodo\1.17.16-public` 共 497 文件，SHA256.json 覆盖其余 496 并逐项校验；包括 Debug、七项公开资产、完整便携目录与使用说明。原 1.17.16 交付和 1.17.14 基线文件保持。证据目录为 `Scratch/PaykiTodo/release-publication-1.17.16`，含安装包复核、源码与资产扫描、草稿 / 公开状态、实际下载、两端生产解析器与最终交付清单。
