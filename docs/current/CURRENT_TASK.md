# Current Task

## Active Development Focus

The `1.15.11 / versionCode 353` reminder-channel and desktop-calendar visual round is complete in code and verification; the remaining local step is to record the focused commit.

The next round should start from physical-phone verification of the two reminder channels and continued testing with the user's real todo volume. Focus on whether the remaining list jank is acceptable on the target OEM device before adding larger architectural changes such as paging or baseline profiles.

## Completed Scope

1. 手机端、电脑端编辑器和规划台识别预览将全屏提醒与通知栏提醒改为两个独立渠道；两项都不选即不提醒，两项都选时并行展示。
2. 工作模式和闹钟模式不再绕过渠道选择强制触发全屏；已选择全屏时仍保留原有强提醒能力。
3. 电脑端日历左侧整点标签与右侧整点实线垂直对齐，完成日程保留原色并仅降低透明度。
4. 版本升级到 `1.15.11 / 353`，数据库版本保持 `29`。
5. 上一轮已完成待办批量选择顶部/底部操作布局、深色工作区、同步轮询隔离和长列表局部性能收口。

## Validation

- `:app:testDebugUnitTest` passed.
- `:app:lintDebug` passed.
- `:app:assembleDebug` passed.
- `node --check app/src/main/assets/desktop-web/app.js` passed.
- `git diff --check` passed.
- APK metadata confirms `PaykiTodo-1.15.11-debug.apk`, `versionName 1.15.11`, `versionCode 353`.
- Pixel_8 emulator verified the layered dark Todo workspace, long-press selection title, fixed bottom actions after scrolling from the first selected item to approximately the twentieth item, and system Back exiting selection mode.
- 102-todo scripted fast-scroll comparison improved janky frames 13.00% -> 10.93%, p95 38ms -> 34ms, p99 57ms -> 44ms, and Slow UI thread frames 31 -> 23.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
