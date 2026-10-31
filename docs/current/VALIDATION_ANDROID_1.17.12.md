# Android 1.17.12 循环单次提醒保护验收

2026-11-01从02e32c5继续原七项目标。Android升为1.17.12/404，Room维持34；Windows保持已验收的1.17.12.0及其原成品。本轮修复现有实例的提醒保护，没有确定新循环规划文本的绝对提醒归属。

## 修复与边界

首次循环生成清空新实例的绝对提醒；已有单次事项改为循环保留原ID和自己的绝对时刻，其他实例只继承相对规则。直接新建循环草稿若含尚未确定归属的绝对时刻，明确拒绝且不创建事项，不静默丢失或复制。

待办/日程的全部、后续和整课共同字段修改保留其他实例各自的绝对提醒；当前编辑实例接收自己的改动。选择无提醒实例改名不会关闭其他纯绝对拥有者；从启用提醒改为空规则则按所选范围关闭。保存后重新计算触发时间和开关。

Android替换循环系列会分配新ID，因此按系列日期锚点及编辑日期平移匹配旧实例，避免按绝对时刻或新ID误配。重建时保留仍符合新规则的已生成远期日期，避免初始窗口截掉其提醒。缩短范围或改变规则而被移除的实例不自动重新分配绝对提醒；本轮没有定义新文本的归属。待办修改现纳入Room事务，生成失败不会留下已删除的实例或模板。

入口：`TodoRepository.createFromDraft`、`createCalendarEventFromDraftTransaction`、`generateRecurringItems`、`generateRecurringEventItems`、`convertSingleTodoToRecurring`、`convertSingleEventToRecurring`、`updateRecurringSeries`、`updateRecurringCalendarSeries`、`updateCalendarEventMultiSlotBundleSharedFields`及实例提醒辅助函数。

## 真实检查

证据保存在`Scratch/PaykiTodo/recurrence-reminders-1.17.12`。新增回归先在未修复实现运行：10项中9项失败，复现复制、丢失和误关闭；修复后扩展为20项。重复构建或重跑不计为新的用例。

| 范围 | 结果 |
| --- | --- |
| 单元测试 | 258项通过，零失败/跳过 |
| Lint | 0错误、137警告、30信息，与前阶段数量相同 |
| 构建 | Debug、AndroidTest及签名Release通过 |
| 模拟器数据库 | 42项通过：20项新RecurringAbsoluteReminderRepositoryTest及22项现有CalendarUpdateDataContractTest、CalendarRepositoryTest、TransientPlanningRepositoryTest |
| 新回归范围 | 单次转循环、独立拥有者、全部/后续/整课修改、日期及钟点修改、新ID映射、远期实例、纯绝对拥有者、显式关闭、当前绝对时刻修改与去重、滚动补齐、备份、无归属拒绝及事务回滚 |
| 运行检查 | 另1项暖启动返回Status: ok，MainActivity为前台Activity；不作为冷启动通过证据 |
| 签名及元数据 | Release签名有效，与交付的1.17.11 Release证书一致；Debug/Release包名、版本名1.17.12、版本码404核对通过 |
| 原用户保护 | user0的646/646及user10的148/148文件哈希完全相同，零新增/变化/移除；专用user11测试变化有前后备份 |
| 交付 | 在既有1.17.12目录增加Debug、Release和Android说明；原487文件哈希不变，现490文件有哈希，不重建Windows |

首次启动指令错误使用applicationId作为Activity命名空间，记录Activity不存在；改为清单中的`com.example.todoalarm.ui.MainActivity`后两次冷启动等待超时，但随后进程和前台Activity均存在，暖启动返回正常。没有把等待超时写成成功，也没有重启/清空AVD。首次比较旧Release使用了已被构建输出更新的路径而失败，改用保留的1.17.11交付副本核对证书通过；失败记录保留。

只在emulator-5554/Pixel_8专用user11使用内存数据库和测试包，未操作真实手机、账号或普通用户数据库。最终恢复user10主页、停止user11应用/测试并核对AVD身份后恢复qemu PID20332的原暂停。前后完整备份在`Backups/PaykiTodo/recurrence-reminders-1.17.12`，所有中途副本保留。没有新增界面改动或完整Android UI流程验收；OEM闹钟/振动与真实手机升级仍不在本次证据内。

## 成品与剩余工作

- Debug：`app/build/outputs/apk/debug/PaykiTodo-1.17.12-debug.apk`，42,879,889字节，SHA-256 `AA69045F77FC3DBFC2D0EEAF68BA50A7CB7FC83EC1A30ECB9810954832A31E16`。
- 签名Release：`app/build/outputs/apk/release/PaykiTodo-1.17.12-release.apk`，21,079,024字节，SHA-256 `3DBCD84D5765A0B7C029FD1F3EE569790CE39E7534606EFC298E7820BD15A2FA`。
- 交付副本：`Deliverables/PaykiTodo/1.17.12`，与Windows成品同目录；Windows验收仍见[对应记录](VALIDATION_1.17.12.md)。

循环规划绝对提醒仍等待用户明确选择：只提醒一次并归属之后最近一次课程，或循环只允许提前分钟。候选校验保持，不能将本修复当作用户选择。新增提交继续进入完整历史审计，最终push尚未执行；没有tag、Release、上传或服务器部署，整体目标未完成。
