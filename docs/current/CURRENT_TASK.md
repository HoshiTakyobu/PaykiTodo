# Current Task

## Active Development Focus

Active immediate task: finish `1.14.20 / versionCode 340` Widget 与循环范围弹窗 UI 改版实施轮次。

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round implements the redesign described in `docs/current/WIDGET_AND_RECURRENCE_DIALOG_UI_REDESIGN.md`，按文档第 9 节建议顺序逐项落地：

1. 今日看板 Widget `今日日程` 标题去掉数量。
2. 倒数日 Widget 空状态从居中粗体卡片改为顶部轻提示。
3. 今日看板 Widget 容器层级瘦身，减少“全是同一种圆角卡片”的堆叠感。
4. 循环待办 / 日程范围选择弹窗从 `AlertDialog` 改为选项卡片样式自定义弹窗。
5. 不改动仓库层循环语义；只改 UI 选择面板和 Widget 视觉。

## Current Status

Implemented so far:

1. 版本号升级到 `1.14.20 / versionCode 340`；数据库版本保持 `28`。
2. `TodoWidgetService.kt` 的今日日程分区标题渲染为 `今日日程`，不再带 `（N）`；`今日待办（N）` 保持不变。
3. `widget_todo_preview.xml` 同步显示 `今日日程`，两个分区标题字号 21sp → 18sp，内联待办卡 elevation 改 0dp。
4. 倒数日 Widget 空状态（`widget_countdown.xml`）改为顶部对齐、左对齐、非粗体两行文案“暂无倒数目标 / 在待办或日程中打开倒数日”，使用新轻量背景 `widget_countdown_empty_background.xml`。
5. Widget 容器瘦身：
   - `widget_todo_section.xml` 标题 21sp → 18sp、弱化阴影、调整 padding。
   - `widget_todo_soft_card_background.xml` 降低底色 alpha、细化描边、圆角 22dp → 18dp（待办卡 / 空状态卡 / 倒数日预览共用）。
   - `widget_todo_task_card.xml` 取消 elevation、minHeight 68dp → 60dp，保留左侧色条作为主锚点。
   - `widget_schedule_inner_background.xml` 改为近透明底，`widget_schedule_event_background.xml` 去底色减圆角让左侧色条成为主识别元素，`widget_schedule_event_active_background.xml` 圆角对齐 12dp 并保留高亮描边。
6. 循环范围弹窗重构：新增 `RecurrenceScopeOptionUi` 模型、`recurrenceScopeDialogTitle/Subtitle/Options(mode)` 生成函数；`RecurrenceScopeDialog` 改为自定义 `Dialog + Surface`，标题 + 副说明 + 三张可点击选项卡（主标题 + 解释），删除场景使用 error 描边/标题色，底部独立“关闭”按钮。业务回调保持不变。

## Validation

Completed for this round:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
2. `./gradlew.bat --no-daemon :app:assembleDebug`
3. APK metadata inspection for `versionName = 1.14.20`, `versionCode = 340`, output `app/build/outputs/apk/debug/PaykiTodo-1.14.20-debug.apk`

## Remaining

1. Commit the completed round with a concrete Chinese commit message.
2. 真机/模拟器验证 Widget 渲染与循环弹窗交互（OEM launcher 渲染建议真机确认）。
