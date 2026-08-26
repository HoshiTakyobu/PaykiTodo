# Current Task

## Active Development Focus

The `1.15.10 / versionCode 352` Todo batch-layout, dark-workspace, and long-list performance round is complete.

The next round should start from physical-phone testing with the user's real todo volume. Focus on whether the remaining list jank is acceptable on the target OEM device before adding larger architectural changes such as paging or baseline profiles.

## Completed Scope

1. 长按进入批量模式后，顶栏显示“多个待办 · 已选 N 项”，并提供全选 / 清空；返回箭头和系统返回键优先退出选择模式。
2. “取消所选 / 完成所选”改为无图标圆角矩形并固定悬浮在底部，列表保留底部空间，滚动到较后位置时仍可直接操作。
3. 待办页深色模式改为分层深蓝灰工作区，不再使用接近纯黑的底板；每日看板山景背景未修改。
4. 电脑同步状态轮询从主 `TodoUiState` 拆出并只在设置页收集；待办页停止看板时钟和看板专属日程 / 倒数派生计算。
5. 待办卡片减少嵌套绘制层、重复颜色 / DDL 格式化和无效协程，分组解析仅覆盖当前页面事项。
6. Bumped to `1.15.10 / 352` without a database migration.

## Validation

- `:app:testDebugUnitTest` passed: 22 suites / 137 tests / 0 failures or errors.
- `:app:lintDebug` and `:app:assembleDebug` passed for `PaykiTodo-1.15.10-debug.apk`.
- APK metadata reports `1.15.10 / versionCode 352`, package `com.paykitodo.app`.
- Pixel_8 emulator verified the layered dark Todo workspace, long-press selection title, fixed bottom actions after scrolling from the first selected item to approximately the twentieth item, and system Back exiting selection mode.
- 102-todo scripted fast-scroll comparison improved janky frames 13.00% -> 10.93%, p95 38ms -> 34ms, p99 57ms -> 44ms, and Slow UI thread frames 31 -> 23.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
