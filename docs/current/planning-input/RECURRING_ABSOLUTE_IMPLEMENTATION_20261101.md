# 循环绝对提醒：实施入口与待确认分支

本次仅定位实现，未改变产品行为。基线Android1.17.11/403、Windows1.17.11.0、Room34；历史审计说明提交为b9bbaac。用户尚未选择下列两种语义，不能将任何一项写成已确认或提前撤销现有校验。

后续Windows1.17.12已独立修复现有实例绝对提醒在补齐/系列修改时被复制或误关的问题，见[验收](../VALIDATION_1.17.12.md)。这项保护不负责分配新循环文本中的绝对时刻，两种语法分支仍待选择；以下定位基线是修复前状态。

## 当前行为

Android `TransientPlanningCandidate.validate` 与Windows同名候选的`Validate`拒绝循环日程混用绝对提醒，保留原文及定位诊断。普通单次事项的绝对提醒、混合相对提醒和无DDL绝对提醒已实现；已有Room34字段`reminderAbsoluteTimesCsv`贯穿事项、备份与同步。LAN复用Android解析与导入。

## 选择一：归属最近一次课程，只提醒一次

必须在整门课程所有时段的实例中选择一个目标。不能逐时段独立选择，也不能把同一绝对时刻复制到所有循环实例。

主要改动入口：

- Android `data/TransientPlanningInput.kt`候选校验；`TodoRepository.importTransientPlanning`的EVENT导入分支；`calendarRecurrenceDates`及`buildItemFromTemplate`用于查询/生成。当前导入通过每个slot生成事项后统一复制`absolutes`，这一处理在移除校验前必须改为按目标实例分配。
- Android `RecurringTaskTemplate`当前没有绝对提醒字段；应优先利用目标事项已有绝对提醒字段，防止滚动补齐将绝对时刻当成重复规则。
- Windows `TransientPlanningInput.cs`候选校验；`DesktopTransientPlanning.cs`导入；`DesktopSnapshotDocument.SaveItemCore`持久化；`DesktopRecurrence.cs`的`ShiftEdit`、`SyncRecurringTemplate`、`EnsureRecurringInstances`、`SaveRecurringItem`及`SaveSeriesContent`。
- Windows补齐先查系列中已占用的实例日期；目标在当前生成窗口之外时，须提前物化正确实例并保留系列锚点，不能因为仅扫描当前窗口而丢掉远期提醒。

建议实施边界，仍需与用户所选语义一致：提醒恰等于开始时归属该实例；同一绝对时刻多个时段同时开始时，以输入时段顺序确定唯一目标；有截止范围且没有之后实例时定位报错，不编造范围外事项。已过去绝对提醒保留且不补响。单次改期保留绝对时刻，取消目标后不擅自重新分配。仅修改标题/地点等共用字段时，不把一个实例的绝对时刻扩散到其他实例。

关键验收：

1. 周一08:00及周三14:00课程，周二12:00绝对提醒只在周三实例保存，所有周一及后续周三实例没有该时刻；多个绝对提醒分别选择目标并去重。
2. 绝对提醒位于初始14天/桌面62天窗口之外，查询仍找到正确实例；后续滚动补齐不会复制或重复生成它。有限范围无目标、相同开始时刻、CLAMP月末/闰日和每2周保持正确。
3. 相对提醒继续针对每次课程，绝对提醒仅针对拥有者；取消/单次改期/时段后续/整门课共同字段修改、账号切换事务回滚、重复确认及跨端备份同步不增加提醒次数。

现有测试扩展入口：Android `TransientPlanningParserTest`、设备`TransientPlanningRepositoryTest`和`TransientPlanningWebContractTest`；Windows `TransientPlanningTests`与循环/提醒回归。先验证真实数据和触发时间，再进行必要界面、构建与安装验收。

## 选择二：循环只用提前分钟，绝对提醒另建单次事项

保留现有循环绝对校验，将内部“归属尚未确定”诊断改为明确的产品规则；说明如何另建单次事项。不得自动拆出隐含待办，也不得丢掉输入中的绝对时刻。预览修正及取消继续不写库。

新增验收应证明循环相对提醒可用、循环绝对提醒有清晰定位、用户明确输入的单次绝对事项只保存一次，以及混合非法批次没有部分导入。更新本地/AI合同、LAN诊断与帮助，移除待确认措辞。

## 收尾顺序

收到用户选择后再执行所选分支。任何可见规则或行为变化先按项目要求升版本；优先保持Room34，只有必要的新持久化合同才迁移。完成相关验收和文档，审计新增提交与远端main后执行已授权推送。没有新变化时不重跑1.17.11全部成功套件。
