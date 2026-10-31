# PaykiTodo 整轮实施指南与模型交接

当前源码及已验收成品Android1.17.13/405、Windows1.17.13.0、Room34。原七项产品需求均已实施，本轮终验、成品核对和设备保护已完成，当前收尾文档、提交、全量审计和已授权源码推送。循环绝对提醒按决策014已实现，不重新等待旧两分支选择，也不重做已完成模块。整体目标在实际push并核实远端前仍未完成。

## 恢复与基线

先执行AGENTS.md恢复检查，再读PROJECT_STATUS、CURRENT_TASK、SESSION_HANDOFF顶部；配套[语法契约](planning-input/INPUT_CONTRACT_20261101.md)、[源码地图](IMPLEMENTATION_MAP_20261101.md)及[启动提示词](START_NEXT_MODEL_20261101.txt)。最新用户指令优先于历史交接状态。

- 本轮恢复HEAD为d765f14；最终功能、归档与推送提交以实际git为准，不沿用旧版本摘要。
- 工程G:\Workspace\Project\PaykiTodo；Android源码命名空间com.example.todoalarm，安装包名com.paykitodo.app。MainActivity为com.example.todoalarm.ui.MainActivity。
- 原未跟踪docs/goals/PaykiTodo_规划台实现Prompt.md须保留，不混入提交，不用它覆盖已确认语法。
- 最近远端main实查83ff5cfc9b66f071dc2e58ce90f3247cd3fb7c69，公开Release仍v1.15.18。当前尚未push、tag、Release、上传或部署；推送前重新核对。
- 用户已恢复原目标，本轮按最后单一推荐方案后用户继续执行决策014，不伪写数字选项回复。不新建goal，不把旧blocked状态当作停止指令。

## 七项需求的实际状态

| 要求 | 已实施行为 | 收尾或验证边界 |
| --- | --- | --- |
| 1. 每日看板统一规划 | 1.17.10三端临时输入、本地/可选AI、编辑预览、确认事务、父子待办与同课多时段；旧文档只读，新输入不保存文档。1.17.13全课绝对提醒唯一归属 | 本轮Android/LAN/Windows相关验收已完成，保留条件跳过边界 |
| 2. Windows设置布局 | 1.17.8同底色分类、明确选中、宽屏靠左、窄屏适配 | 既有验收保持 |
| 3. AI报告与七天日报 | 1.17.9共同prompt、前后关联上下文、生成满168小时过期、恢复/同步防复活 | 既有验收保持 |
| 4. 中文字体和字号 | 1.17.8中文/英文别名、12—22px真实预览、完成才保存、跨端字段保留 | 既有验收保持 |
| 5. GitHub与平滑更新 | 1.17.11双端稳定版本检查；手机下载恢复/身份核对/系统安装入口；Windows签名后台/灰度/增量/独立启动器/失败回退 | 模拟发布与本地验收不代表真实未来公开Release升级已实测 |
| 6. Windows使用说明 | 设置/F1离线说明、公开安装/使用文档；1.17.13已同步新提醒规则 | 当前MSI已包含新版说明 |
| 7. 文档、历史审计和push | 原60未公开提交完成隔离脱敏；后续复扫至d765f14，66提交/1715blob/167已分类URL，零未解决命中 | 新增功能、归档和文档提交继续全量复扫；执行已授权main推送并核实远端 |

1.17.12双端实例保护保持：生成不复制单次绝对提醒，系列/整课修改保留对应实例时刻，显式关闭仍生效；新ID按系列日期锚点匹配，保留有效远期实例，失败事务回滚。

## 已落地的循环绝对提醒规则

标题必填，其他字段可省略；时间点为待办DDL，>时间段为日程；今天/当前年简写及空格/+，省略提醒=(5,0)、()关闭，分号后#直到行尾，反向跨午夜要求补结束日期，层级/首尾引号、循环方括号、多时段课程与可选范围均已确定。

决策014如实记录：本轮按最后单一推荐方案后用户继续执行，绝对时刻只提醒一次。对每个不同绝对时刻，在整门课所有时段的合法实例中选Start>=absolute的最近实例；相等归本次，并列按输入时段顺序。相对分钟仍针对每次课程。重复绝对时刻去重，同一拥有者合并，过去时刻保留不补响。

窗口外拥有者提前物化，滚动补齐不跳过中间课程。有限范围无目标则定位报错、保留原文、拒绝整批导入。取消、单次改期或系列共同字段修改不自动重新分配或复制。沿用Room34事项字段，循环模板不存单次绝对提醒。三端预览显示归属，修改后重算；LAN复用原session冻结日期及账号代际，重校验不写库。

具体实现与验收入口见[循环绝对提醒实施说明](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。Android普通直接新建含绝对字段的无归属循环草稿仍提示从规划预览确认所属实例；不能把这个保护误写成规划不支持绝对提醒。

## 剩余执行顺序

1. 本轮测试、成品和设备保护均已结束，直接使用VALIDATION_1.17.13及原始证据；没有新改动或失败不重复运行成功套件。
2. 同步最终文档，中文功能提交后单独归档目标；原未跟踪Prompt排除。复扫全部新增提交及忽略路径，核对真实远端。
3. 只push审计过的main，不force、不push --all或恢复引用。实查远端HEAD相等和工作树后才标原goal complete。源码push不等同创建Release、上传安装包或部署。

## 数据与环境保护

Android/Windows各自本地保存数据；手机LAN兼容服务和正式Windows账号同步是不同路径。草稿/识别/预览不写业务库，确认事务才导入。旧文档、事项、恢复副本、认证、私钥、未提交源码和失败证据保留，不删除或搬迁用户资料。

复用无窗口emulator-5554/Pixel_8、qemu PID20332，最终已停止user11应用/测试、回user10主页并恢复原暂停。原user0 646/646与user10 148/148逐文件SHA256一致，零增删改；user11 after218文件保留备份。前后副本在Backups/PaykiTodo/planning-absolute-1.17.13，暂停时不要因ADB等待重启/清空。Windows用独立桌面/资料，不占全局输入，不动真实手机。

过程输出Scratch/PaykiTodo，备份Backups/PaykiTodo，成品Deliverables/PaykiTodo。更新签名私钥只在忽略的user/update-signing目录，不能打印或提交。

## 成品与证据

当前构建路径：app/build/outputs/apk/debug/PaykiTodo-1.17.13-debug.apk、app/build/outputs/apk/release/PaykiTodo-1.17.13-release.apk、desktop/installer-output/PaykiTodo-1.17.13-windows-x64.msi。真实Windows payload为desktop/obj/installer/1.17.13-15bbf1bf1431488690872e90c2ae6327/payload，不用旧desktop/dist或单个EXE代替。

本轮终验记录见[1.17.13](VALIDATION_1.17.13.md)，证据在Scratch/PaykiTodo/planning-absolute-1.17.13。Android270单测零失败/跳过、Lint0错误/137警告/30信息、三种构建通过；64设备条目中63通过、1旧跨端fixture条件跳过，本轮17新增设备检查全部通过。Windows62不同检查通过（60针对+2包）；LAN模拟HTTP实际DOM9/9及node--check通过，不能冒称真实LAN网络验收。

Deliverables/PaykiTodo/1.17.13共有494文件，SHA256.json覆盖其余493；便携484文件、完整更新ZIP482条目、增量4条目已验签与重建核对。两APK、MSI、说明和更新资产均为新目录成品，旧1.17.12交付保留。

既有阶段：[1.17.8](VALIDATION_1.17.8.md)、[1.17.9](VALIDATION_1.17.9.md)、[1.17.10](VALIDATION_1.17.10.md)、[1.17.11](VALIDATION_1.17.11.md)、[Windows1.17.12](VALIDATION_1.17.12.md)、[Android1.17.12](VALIDATION_ANDROID_1.17.12.md)。没有新变化不重复运行旧成功套件，不将模拟器结果扩展为OEM/真实手机验收。

历史清理范围见[审计](PUBLIC_HISTORY_AUDIT_20261101.md)。私有逐blob策略和恢复bundle保留，只处理远端main后的未公开历史。旧交接方案保留于Git历史与Backups，仅作历史参考，不重问其过期问题。
