# 1.17.13 本地发布候选

此候选已被安全公开构建 [v1.17.14](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.14) 替代。1.17.13 原成品保留；当前验收见 [1.17.14](VALIDATION_1.17.14.md)，下方仅保留正式发布前的历史准备事实。

原七项目标已完成，源码与最终CI基线为`70d892af2e8641cbb47bed757adc0329aa54bd07`。用户随后再次要求继续，但尚未指定新的功能或提交公开发布指令；本轮仅整理可供查看的发布候选。没有创建tag/Release、上传安装包、改变线上灰度或部署服务器。公开Release仍为v1.15.18。

## 已备材料

候选版本Android1.17.13/405、Windows1.17.13.0，Room34。使用已验收的APK、MSI和签名更新资产，没有重建APK或改变既有交付目录。新完整便携ZIP位于`Scratch/PaykiTodo/release-preparation-1.17.13/assets/`，484条目逐项与原便携目录和交付哈希相同，包括启动器和update-files.json。

| 名称 | 用途 | 字节 |
| --- | --- | ---: |
| PaykiTodo-1.17.13-release.apk | Android正式签名安装包 | 21084248 |
| PaykiTodo-1.17.13-windows-x64.msi | Windows每用户安装 | 106226796 |
| PaykiTodo-1.17.13-windows-x64.zip | 完整Windows便携目录，解压后运行启动器 | 130646520 |
| PaykiTodo-1.17.13-windows-full.zip | Windows应用内完整更新，482条目 | 134261957 |
| PaykiTodo-1.17.12-to-1.17.13-windows-delta.zip | 仅适用1.17.12基线的增量更新，4条目 | 50363802 |
| PaykiTodo-update-manifest.json | RSA-PSS签名更新清单 | 87838 |
| SHA256SUMS.txt | 六项安装/更新资产的可读SHA256清单 | 613 |

全部原有资产与交付SHA256.json匹配，新便携ZIP逐条目核对通过。资产路径、大小和SHA256在`Scratch/PaykiTodo/release-preparation-1.17.13/asset-manifest.json`；可读校验清单在`assets/SHA256SUMS.txt`。

公众发布说明草稿为同目录的`RELEASE_NOTES.md`，覆盖v1.15.18以来的独立Windows、临时规划、循环提醒、日历共享、同步、字体、报告与更新能力，注明实际验收边界；没有插入尚不存在的v1.17.13下载链接。文本审核结果为`release-notes-text-audit.json`。

## 签名与实际更新范围

已下载现有公开v1.15.18 APK并核对：两包签名有效、证书一致、包名均为com.paykitodo.app，版本码从360至405，满足覆盖升级的身份条件。没有在真实手机上执行该跨度的安装或数据迁移。证据为`signature-compatibility.json`、签名和badging日志；此前1.17.13产品验收见[验收记录](VALIDATION_1.17.13.md)。

现有签名更新清单的`rollout.percent`为**100**，seed为PaykiTodo-1.17.13，验签通过。公开上传这份清单后，所有具备当前更新器的Windows客户端都处于自动更新范围；这不是小范围灰度。若用户要求分阶段扩大，需要更改比例并重新签名，保留现有清单。原v1.15.18尚无本轮新更新器，不能宣称它会自动升级。

Android和Windows只检查稳定Release，draft/prerelease不会被其正常自动检查选中。清单只存资产名称，客户端根据稳定v1.17.13构造下载地址；应先准备完整资产后一次公开发布，避免清单先可见而ZIP尚缺失。其他Windows基线会使用完整更新回退，不套用1.17.12增量。

应用内完整ZIP省略PDB和update-files.json，后者由客户端校验更新后生成；因此另外准备完整便携ZIP，不能把两包等同。详细审查在`update-review.md`。

## 后续操作边界

候选建议为稳定v1.17.13，引用上述已验收源码并上传表列资产。实际创建公开Release和启用100%自动更新需要用户明确这一新增范围；原目标第7项是源码提交和push，本地准备不等于公开发布。用户若继续修改新功能，应转入其新需求，不发布本候选。

没有修改原用户资料或模拟器状态；既有成品、认证、签名秘密、失败证据和原未跟踪规划Prompt均保留。发布准备不重启已经完成的旧goal。
