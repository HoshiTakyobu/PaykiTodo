# 循环绝对提醒：已实施规则与验收入口

## 当前1.17.14发布准备

当前源码Android1.17.14/406、Windows1.17.14.0、Room34，安全公开包正在准备。Android公开构建不读取本机cloud.properties，新安装须在登录页选择有效服务器。已存非空remoteSyncServerUrl继续优先；此前只依赖旧编译默认地址、未存地址的安装可能需要重新选择。Android公开构建、270单测和Lint0错误已通过；Windows新包/更新资产与正式发布终验待补记，范围沿用已授权100%、增量基线1.17.13。公开Release仍v1.15.18；1.17.13功能验收为历史，旧七项目标保持complete，没有本轮设备操作或服务器部署。

## 1.17.13历史实施状态

Android1.17.13/405、Windows1.17.13.0、Room34已实现全课唯一归属。本轮按最后单一推荐方案后用户继续执行，决定来源见决策014，不伪写数字选项回复。旧两分支定位保留在Git历史，不能继续作为等待用户选择的执行指令。

## 当前规则

- 对每个不同绝对时刻，在整门课所有输入时段的合法实例中选择Start>=absolute的最近一个，相等归本次，同一最早开始时刻按输入时段顺序。
- 重复时刻去重，同一拥有者合并；相对分钟针对每次，绝对只针对拥有者。触发重合不重复派发，过去时刻保留不补响。
- 查询完整循环和生效范围，不受初始生成窗口约束；窗口外目标提前物化，后续补齐不跳过中间课程。
- 有限范围无目标则定位报错、保留原文、整批拒绝，不造范围外实例或隐含待办。
- 取消、单次改期、时段后续或整课共同字段修改不自动重新分配或复制。1.17.12实例提醒保护继续生效，显式关闭按所选范围执行。
- 沿用Room34的reminderAbsoluteTimesCsv事项字段、备份与同步合同；循环模板不存单次绝对时刻。

## Android实现

data/PlanningAbsoluteAssignment.kt的assignPlanningAbsoluteReminders统一分配slotIndex、occurrenceStartAt与该拥有者的absoluteReminderTimes。先按周期跳转，再复用calendarRecurrenceDates匹配，覆盖DAILY/WEEKLY/MONTHLY_DAY/YEARLY_DATE，月末/闰日沿用CLAMP；查询没有将100年窗口当截止。

data/TransientPlanningInput.kt候选允许循环绝对提醒并校验合法目标。TodoRepository.importTransientPlanning在整门课分配后按时段生成，不把绝对字段复制到每次实例；createCalendarEventFromDraftTransaction的additionalDates提前物化远期拥有者并重算触发时间。ensureRecurringInstancesAhead从max(today, templateStart)检查缺口，排除existing/skips，不从最新远期实例推进。

ui/TransientPlanningDialog.kt展示归属，编辑后重算。普通直接创建无归属循环草稿仍提示从规划预览确认所属实例，不能绕过唯一归属流程。

## Windows实现

TransientPlanningCandidate.AssignAbsoluteReminders通过DesktopSnapshotDocument.NextPlanningOccurrence在全课时段中选择唯一目标。DesktopRecurrence.CreateRecurringOccurrence复用实际生成匹配器；DesktopTransientPlanning确认导入分开保存相对规则和单次绝对字段，窗口外提前生成。混合内部NONE候选直接用本次initial，避免series=null选中已有同刻单次事项。

NativeWindow.TransientPlanning.cs提供可编辑预览与实时归属说明。现有系列/整课保存、补齐和提醒重算沿用1.17.12保护，确认仍在本地事务中执行。

## LAN实现

sync/TransientPlanningWeb.kt输出absoluteAssignments，每项包含start、slotIndex、reminders。DesktopSyncCoordinator.previewTransientPlanning对编辑候选复用原session冻结now，检查账号generation/token/有效期并重校验；不新建session或写库。

desktop-web/transient-planning.js展示归属，时间/提醒/范围修改后200ms重校验；pending/invalid禁用确认，请求前及响应后检查revision/epoch/connectionGeneration，丢弃过期回调。派生字段不作为可信输入，最终导入重新计算。

## 新增与受影响验收

1. 算法：跨时段、相等、同刻输入顺序、重复/多个绝对时刻、有限范围无目标、月末/闰日、间隔与远期窗口。
2. 数据：真实拥有者字段与触发时间、相对每次、远期实例与中间补齐、取消/改单次/系列共同字段、过去不补响、备份往返、事务和重复确认。
3. 界面：真实Compose/WPF编辑后重算、无目标错误及禁用确认、取消不写库；LAN JSON合同与真实DOM模拟HTTP流程分别记录。

Android单测PlanningAbsoluteAssignmentTest；设备RecurringPlanningAbsoluteRepositoryTest、RecurringPlanningAbsoluteUiTest、RecurringPlanningAbsoluteWebContractTest，以及受影响的旧循环、规划和日历合同。WindowsTransientPlanningTests、循环作用域/提醒回归、实际WPF预览和packaged检查。

本轮Android270单测、Lint0错误/137警告/30信息和三种构建通过；64设备条目63通过、1旧跨端fixture条件跳过，本轮17新增设备检查全部通过。Windows62项不同检查通过（60针对+2包）；LAN模拟HTTP实际DOM9/9及node--check通过，未冒称真实LAN网络。成品、更新资产、Release同证书和原user0/user10逐文件哈希已核实，模拟器原暂停恢复。精确统计、失败/复验和边界见[1.17.13验收](../VALIDATION_1.17.13.md)，私有过程目录Scratch/PaykiTodo/planning-absolute-1.17.13。

## 完成状态

本轮实施、验收、成品、设备保护、文档、功能提交e014a23及独立目标归档f0902eb已完成。推送前按旧远端83ff5cf基线全量审计68提交/1756blob/169已分类URL，零未解决命中与已审地址残留；main正常fast-forward至f0902ebe874fa185134d4755682080b08d12e632，实查本地/远端一致，原七项目标已完成。最终推送结果文档补记仍从同一旧基线完整复扫并随main推送。公开Release仍v1.15.18，没有tag/Release/上传APK/部署；后续会话核基线后仅按新任务继续。
