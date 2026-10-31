# Windows 1.17.12 循环单次提醒修复验收

后续Android1.17.12/404也已独立修复并交付，258单测、Lint及42项模拟器数据库检查通过，见[Android验收](VALIDATION_ANDROID_1.17.12.md)。本页下方的“Android保持1.17.11/无新APK”仅描述Windows修复阶段，其成品未重建。

2026-11-01从b9bbaac继续原目标，检查剩余循环绝对提醒时发现独立的实例复制缺陷。Windows升为1.17.12.0；Android保持1.17.11/403，Room34。没有选择尚待用户确定的规划语法分支，整轮目标与最终push仍未完成。

## 修复行为

Windows补齐循环实例时会从已有事项复制原型，旧逻辑保留其`reminderAbsoluteTimesCsv`。系列共同字段编辑和改期也会把当前编辑实例的绝对提醒写给其他实例；选择没有提醒的实例改名，还可能关闭另一个实例的提醒。

新实例清空原型的绝对提醒，继续使用模板相对规则。系列保存按目标实例保留绝对提醒，当前编辑实例接收自己的修改；显式关闭提醒仍作用于所选范围。保存/模板应用后重算实际触发时间和开关，无任何规则的新实例不保留空提醒。原拥有者、其他实例各自的提醒、原始时刻及备份均保留。

源码入口为`desktop/PaykiTodo.Desktop/DesktopRecurrence.cs`中的`ResetOccurrence`、`WithOccurrenceReminders`、`RefreshOccurrenceReminders`、`SaveRecurringItem`和`SaveSeriesContent`。规划候选校验保持；本修复不确定新循环文本中绝对时刻应归属哪次课程，也不自动创建额外待办。

## 验证与成品

证据在`Scratch/PaykiTodo/recurrence-reminders-1.17.12`。首次新增3个补齐用例均复现失败；补齐修正后38项通过。新增10个系列编辑用例再次复现原复制/关闭问题；完整修正后48项通过。最后补充实际改期和范围扩展用例，最终49项通过。旧用例与重跑不重复统计。

| 检查 | 结果 |
| --- | --- |
| 最终数据/提醒回归 | 49项通过：RecurrenceScopeTests、CalendarSemanticsTests、TransientPlanningTests；其中14个不同新增用例，覆盖待办/日程、相对与纯绝对、备份后窗口外补齐、全部/后续范围、两个独立拥有者、选择其他实例改名、显式关闭、实际改期及范围扩展 |
| 正式程序检查 | 另2项PackagedDesktopTests通过，验证实际1.17.12程序启动/独立资料保留及Loaded nonce确认；合计51个不同检查，本轮没有重跑完整UIA或Android套件 |
| MSI及便携payload | 新MSI构建通过，484个payload文件逐一与MSI清单匹配，文件版本1.17.12.0；公共构建未传本地服务器配置，payload中没有client-config.json |
| 交付副本 | Deliverables/PaykiTodo/1.17.12含MSI、484文件完整Windows-portable、清单、使用说明和SHA256.json；487个交付文件记录哈希，便携484文件与MSI清单一致 |

使用独立Win32桌面和专用测试资料，没有全局输入、真实账号操作或普通用户目录迁移。微软WebView2引导安装器复用已有有效微软签名缓存。构建完成后才运行实际成品检查；未再执行MSI安装/升级/修复/卸载生命周期，其既有验收仍见[1.17.11](VALIDATION_1.17.11.md)。本轮没有界面视觉变化或新Android运行行为，无新APK、手机安装或模拟器操作。

MSI：`desktop/installer-output/PaykiTodo-1.17.12-windows-x64.msi`。

最终payload：`desktop/obj/installer/1.17.12-773fce09cffd481888a0003a9a2ecb1d/payload`；不要用旧desktop/dist代替。

MSI SHA-256：`A12AF3672F10080BFE0D05404E17D563E319AF1B199FDA9EF31A003A4061CD0D`。

最新Android Debug仍为`app/build/outputs/apk/debug/PaykiTodo-1.17.11-debug.apk`，本轮没有重建。循环规划绝对提醒选择仍待答复；历史60提交清理已完成，新增提交须继续审计后才执行最终授权push。没有push、tag、Release、上传或服务器部署。
