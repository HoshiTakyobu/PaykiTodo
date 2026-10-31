# v1.17.14 正式发布核对

2026-11-01 06:10:38（Asia/Shanghai）已按既有授权公开稳定 [v1.17.14](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.14)，Release ID 为 430213317。GitHub latest、draft=false、prerelease=false 均实查一致。Android 1.17.14/406、Windows 1.17.14.0、Room34；旧七项开发目标及本次发布均完成。

## 源码与资产

源码先正常快进推送 main，再创建完整草稿、上传全部资产并核对，之后一次公开。发布标签指向 `a1ea844f61c7d51f520c7a60ff58b433f98e2719`；对应 [CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/runs/41246534777) 的公开模式单测、Debug 构建及空白检查通过。发布说明与审核原文相同。

| 公开资产 | 字节 |
| --- | ---: |
| PaykiTodo-1.17.14-release.apk | 21084288 |
| PaykiTodo-1.17.14-windows-x64.msi | 106194028 |
| PaykiTodo-1.17.14-windows-x64.zip | 130213983 |
| PaykiTodo-1.17.14-windows-full.zip | 134261962 |
| PaykiTodo-1.17.13-to-1.17.14-windows-delta.zip | 50363807 |
| PaykiTodo-update-manifest.json | 87838 |
| SHA256SUMS.txt | 613 |

7 项上传状态、大小及 GitHub 提供的 SHA256 摘要全部与本地资产一致。随后从真实公开 browser_download_url 下载全部 442206519 字节，每项 SHA256 再次匹配。正式 APK 与公开 v1.15.18 证书相同；Windows 便携为 484 条目，完整更新为 482 条目，1.17.13 增量为 4 条目，签名及实际字节重建验收见 [1.17.14](VALIDATION_1.17.14.md)。

公开包不预置有效账号服务器。新安装或没有保存地址的旧安装需要选择服务提供者给出的原服务器；已保存的非空地址继续优先。说明见 [公开构建](PUBLIC_RELEASE_BUILD.md)。

## 实际生产更新解析器

两端直接使用相同的真实 GitHub Release JSON 字节，没有改写资产字段。Android 调用本轮编译的生产 GithubRelease.parse，确认旧 1.17.13 版本参数可识别 1.17.14 的正式 APK 地址、大小和 digest；1.17.14 参数不提示更新。

Windows 调用实际交付 PaykiTodo.dll 的 ReadRelease、Verify、InRollout、CheckAsync 和 PrepareAsync，接受公开 MSI、完整便携、full、delta 和签名清单。生产验签通过，范围 100%；1.17.13 参数可发现更新，1.17.14 参数不提示更新。读取真实 1.17.13 基线后，首个压缩包请求选择该增量；1.17.12/1.15.18 参数选择 full。所用签名清单与实际公开下载文件大小、SHA256 及 API digest 一致。

此生产逻辑检查用离线 HTTP 提供器供应真实发布响应，在 ZIP 请求前停止，ZIP 传输 0 字节；没有启动产品、切换更新或操作设备。它与前述真实公开下载及既有更新重建各自记录，不能声称所有实体客户端都已升级。没有更新器的旧安装仍需先使用完整安装包。

Windows 首轮辅助脚本误用本地便携文件名 windows-portable.zip 作断言，实际公开命名为 windows-x64.zip。生产解析器接受真实名称，修正辅助脚本后通过；失败证据保留，没有修改产品或公开资产。

## 交付与证据

新交付目录 `Deliverables/PaykiTodo/1.17.14` 共 497 文件，SHA256.json 覆盖其他 496 文件并逐项复核。包含两 APK、新 MSI、完整便携目录/ZIP、签名更新、说明和公开校验清单。旧 1.17.13 的 494 文件与构建前清单完全相同。真实手机、原暂停模拟器与普通 Windows 用户资料保持，没有服务器部署。

证据在 `Scratch/PaykiTodo/release-publication-1.17.14`：release-draft/public-verification.json、ci-source-verification.json、public-download-verification.json、final-delivery-verification.json，以及 post-publication/production-public-release-result.json 和两端详细解析结果。历史审计入口见 [审计记录](PUBLIC_HISTORY_AUDIT_20261101.md)。最终文档提交和 main 一致性由实际 Git 与私有推送核对记录确认，标签保持发布源码提交。

本次没有剩余开发、资产上传或发布步骤。后续先核实际仓库和当前交接，再按新的用户任务执行；不重复本版本发布或旧功能验收。
