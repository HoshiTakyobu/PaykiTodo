# Current Task

## Active Development Focus

The `1.15.9 / versionCode 351` Todo interaction round is complete.

The next round should start from physical-phone testing of long-press batch selection on the Daily Board and Todo page, including cancellation of recurring instances. Do not reintroduce per-card save buttons or Android's default Dalvik User-Agent.

## Completed Scope

1. 每日看板和待办页的待办卡片均支持长按进入批量选择，长按项自动选中；普通状态不再显示常驻批量入口。
2. 选择模式支持逐项切换、全选可见、清空、完成所选、取消所选和退出；批量取消一次确认后逐条写入已取消历史，循环项只取消当前实例。
3. 待办分组筛选改为近全宽、限高的下拉菜单，菜单顶部提供管理分组，并保留新建、编辑、删除和交集 / 并集切换。
4. 保留 `1.15.8` 已完成的事务式编辑、AI 单请求测活、Codex CLI User-Agent 和 Todo/Settings 视觉层级改动。
5. Bumped to `1.15.9 / 351` without a database migration.

## Validation

- `:app:testDebugUnitTest` passed: 22 suites / 137 tests / 0 failures or errors.
- `:app:lintDebug` and `:app:assembleDebug` passed for `PaykiTodo-1.15.9-debug.apk`.
- APK metadata reports `1.15.9 / versionCode 351`, package `com.paykitodo.app`.
- Pixel_8 emulator verified the near-full-width group dropdown and top “管理分组” entry, long-press selection on Todo and Daily Board, complete/cancel actions, cancellation confirmation, and Back exiting selection before navigation.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.
