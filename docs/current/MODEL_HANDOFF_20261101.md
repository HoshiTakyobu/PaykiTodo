# PaykiTodo 整轮实施指南与模型交接

本指南以已验收的双端1.17.12为基线。原七项目标尚未完成：只剩循环规划文本中绝对提醒的明确规则、所选分支的实现/验收，以及最终源码推送。规划主体、设置/字体/帮助、报告、更新器和未公开历史清理已经完成，不重新开发。

## 恢复与基线

先执行AGENTS.md的恢复检查，再读PROJECT_STATUS、CURRENT_TASK、SESSION_HANDOFF顶部；配套[语法契约](planning-input/INPUT_CONTRACT_20261101.md)、[源码地图](IMPLEMENTATION_MAP_20261101.md)及[启动提示词](START_NEXT_MODEL_20261101.txt)。最新用户明确回复优先于文档中的建议。

- Android1.17.12 / versionCode404，Windows1.17.12.0，Room34。最近Android功能提交2e83d17、目标归档3e3b5c2；本次文档更新之后的HEAD以实际git为准。
- 工程G:\Workspace\Project\PaykiTodo；Android源码命名空间com.example.todoalarm，安装包名com.paykitodo.app。MainActivity为com.example.todoalarm.ui.MainActivity，不用安装包名拼出Activity类名。
- 工作树原有未跟踪docs/goals/PaykiTodo_规划台实现Prompt.md，与保留的原始备份哈希一致；不得混入新提交或用它覆盖用户已确认语法。
- 最近远端main实查83ff5cfc9b66f071dc2e58ce90f3247cd3fb7c69，公开Release仍v1.15.18。未执行push、tag、Release、上传或部署；最终推送前重新核对。
- 用户为换模型提出的暂停已明确撤销。继续原目标，不再次请求恢复许可，不创建新goal替代，也不把旧工具状态当作停止执行的指令。

## 七项需求的实际状态

| 要求 | 已完成并验收 | 剩余工作 |
| --- | --- | --- |
| 1. 每日看板统一规划 | 1.17.10：Android/Windows/LAN临时输入、本地/可选AI、可编辑预览、确认事务导入、父子待办和课程多时段；旧文档只读，新输入不保存文档 | 确认并实施循环文本的绝对提醒规则 |
| 2. Windows设置布局 | 1.17.8：同底色分类、明确选中、宽屏靠左、窄屏适配 | 保持已完成行为 |
| 3. AI报告与七天日报 | 1.17.9：共同prompt、相关记录上下文、生成满168小时过期、恢复/同步防复活 | 保持已完成行为 |
| 4. 中文字体和字号 | 1.17.8：中文/英文别名、12—22px真实预览、完成才保存、跨端字段保留 | 保持已完成行为 |
| 5. GitHub与平滑更新 | 1.17.11：两端稳定版本检查；手机后台下载/状态恢复/身份核对/系统安装入口；Windows签名后台/灰度/增量/独立启动器/失败回退 | 真实未来公开Release不存在，不能声称线上升级已实测；不重写更新器 |
| 6. Windows使用说明 | 设置/F1离线说明及公开安装/使用文档已同步 | 后续实际行为改变才更新嵌入资源 |
| 7. 文档、历史审计和push | README及阶段文档已更新；原60未推送提交完成隔离脱敏，后续已复扫到3e3b5c2的65提交/1710blob/167已分类URL，零未解决命中 | 新增提交继续复扫，全部实现完成后执行既有授权main推送 |

额外的1.17.12双端修复保护已有实例的单次绝对提醒，避免生成/系列修改复制或误关闭。Android新ID按系列日期锚点匹配，保留仍有效的远期实例，待办修改失败事务回滚。这项修复不负责选择新文本的归属规则。

## 唯一待确认的产品规则

用户已确认标题必填、其他字段可省略，时间点为待办DDL、>时间段为日程，今天/当前年简写及空格/+，省略提醒=(5,0)、()关闭，分号后#到行尾，反向跨午夜要求补结束日期，层级/首尾引号、循环方括号、多时段课程和可省略范围。不要重问这些。

尚未明确选择循环绝对提醒：

1. 只提醒一次，归属绝对时刻之后最近一次课程；恰等于开始时归属该实例，相对分钟继续每次提醒。
2. 循环只允许提前分钟；绝对提醒由用户另写单次事项，不自动拆出隐含待办。

只收到“继续”不代表选择了任一项。当前两端规划候选定位报错、保留原文；Android仓库直接新建无归属循环草稿也拒绝，不复制到全部实例，Windows普通保存接口没有同等额外拒绝。已存在实例的绝对提醒仍正常保留。建议边界和入口详见[分支实施定位](planning-input/RECURRING_ABSOLUTE_IMPLEMENTATION_20261101.md)。

## 剩余实施顺序

1. 收到明确选择后更新语法契约/决策，只实现所选分支；变化前按AGENTS升相关端版本。优先复用Room34字段。
2. 若选一次归属，在整门课所有时段中唯一分配，处理初始窗口外目标、有限范围无目标及并列开始时刻；保留已过去时刻且不补响。取消/改期/系列编辑/补齐不得复制或自动重新分配单次提醒。建议不得冒充用户确认。
3. 若选提前分钟，统一本地/AI/LAN诊断和帮助，明确用户自行写单次事项；非法批次事务拒绝，保留原文，不自动拆任务。
4. 运行针对所选分支的解析/真实数据库/跨端/必要界面检查，保留失败证据；有新实现才补构建及安装相关验收，已通过的旧套件不重复。
5. 同轮更新当前状态、合同、公开说明和验收，创建中文功能提交及独立目标归档。逐提交复扫全部新历史，核对忽略路径和真实远端。
6. 所有原要求完成后只push审计过的main。不要push --all、恢复引用或强改公开历史；创建Release、上传安装包、服务器部署与源码push分别处理授权。

## 数据与环境保护

- Android/Windows各自本地保存数据；手机LAN兼容服务与正式Windows账号同步是不同路径。事项长期保留，旧文档/备份不因移除入口而删除。
- 草稿、识别和预览不写业务库，确认事务才导入；账号/草稿代际保护、真实同步冲突与恢复副本保持。
- 原用户数据、认证、私钥、未提交源码及失败证据保留。更新签名私钥只在忽略的user/update-signing目录，不打印或提交。
- 复用emulator-5554/Pixel_8，无窗口，最终user10主页、应用停止，qemu PID20332恢复暂停。先核对真实PID/AVD/线程状态；暂停时ADB仍可能显示device，不盲目重启或清空。
- 仅user11做合成测试，安装会更新共享APK；先备份原user0/user10并比哈希，最终恢复主页与暂停。不操作真实手机或普通Windows资料，不使用全局键鼠/剪贴板验收。
- 过程输出放Scratch/PaykiTodo，恢复副本放Backups/PaykiTodo，成品放Deliverables/PaykiTodo；不删除旧副本，不创建目录联接。

## 成品与证据

Android最新Debug及签名Release在app/build/outputs/apk/debug/PaykiTodo-1.17.12-debug.apk与app/build/outputs/apk/release/PaykiTodo-1.17.12-release.apk，副本在Deliverables/PaykiTodo/1.17.12。Release与1.17.11交付证书一致；Debug/Release不可互相覆盖。

Windows最新MSI为desktop/installer-output/PaykiTodo-1.17.12-windows-x64.msi；实际payload为desktop/obj/installer/1.17.12-773fce09cffd481888a0003a9a2ecb1d/payload。使用完整Windows-portable目录，不用旧desktop/dist或单个EXE代替。交付清单现490文件，添加Android时原487文件哈希不变。

验收：[1.17.8](VALIDATION_1.17.8.md)、[1.17.9](VALIDATION_1.17.9.md)、[1.17.10](VALIDATION_1.17.10.md)、[1.17.11](VALIDATION_1.17.11.md)、[Windows1.17.12](VALIDATION_1.17.12.md)、[Android1.17.12](VALIDATION_ANDROID_1.17.12.md)。Android258单测、Lint0错误、42数据库检查通过；暖启动正常，冷启动等待超时如实记录。原user0 646/646与user10 148/148文件哈希一致，不能将模拟器结果扩展为OEM/真实手机验收。

历史清理范围和恢复说明见[审计](PUBLIC_HISTORY_AUDIT_20261101.md)。私有策略、逐blob报告和恢复bundle位于项目Scratch/Backups中；清理只覆盖远端main后的未公开提交，不表示撤回已有公开历史。旧1.17.9交接方案保留在Git历史与Backups/PaykiTodo/handoff-docs-20261101-3e3b5c2，不能再当当前待办清单。
