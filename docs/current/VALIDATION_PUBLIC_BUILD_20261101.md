# 公开源码无签名配置构建检查

1.17.13功能与目标归档推送至f0902eb后，首次[GitHub Android CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/runs/41242742792)在Gradle配置阶段失败，尚未执行单元测试或APK编译。原因是没有本地签名配置时storeFile为空，`rootProject.file("")`提前抛出异常。源码空白检查已通过，故障不在已验收的规划数据逻辑。

修正：只在storeFile非空时解析候选密钥库路径。公开检出可配置Debug任务；请求Release仍必须提供完整有效的本地签名配置，继续明确拒绝缺失配置，不能静默使用Debug签名。

本地验证使用Git归档创建的独立公开源码副本，覆盖修正后的Gradle脚本，确认副本没有user/认证/密钥库。没有移动或读取真实签名秘密内容。

- `:app:assembleDebug --dry-run`成功，5秒；这是配置与任务图检查，不计作新增APK构建。
- `:app:assembleRelease --dry-run`按预期失败，明确提示Release signing is not configured；不再是空路径异常。
- `git diff --check`通过。

证据：`Scratch/PaykiTodo/planning-absolute-1.17.13/github-ci-failed.log`、`ci-public-debug-config.log`、`ci-public-release-config.log`及独立`ci-public-checkout/`。后续修复提交的GitHub实际单测和Debug构建结果将在此补记。

这是公开构建配置修正，未改变用户行为、版本或已验收成品，也未重建交付APK。Android仍1.17.13/405、Windows1.17.13.0、Room34；本地成品及签名见[1.17.13验收](VALIDATION_1.17.13.md)。源码推送结果见[审计记录](PUBLIC_HISTORY_AUDIT_20261101.md)。
