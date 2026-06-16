# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.9`
  - `versionCode = 329`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.9-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.13.11-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11`

## Active Goal

Restore continuous horizontal scroll in calendar three-day view (removing HorizontalPager pagination).

## What Changed In The Latest 1.14.9 Patch

1. **恢复日历三日视图连续滚动**：移除 1.13.10 引入的 HorizontalPager 翻页机制，恢复到之前的连续滚动实现（参考 1.13.9 之前的版本）：
   - 移除 `HorizontalPager` 和 `rememberPagerState` 相关代码
   - 添加 `horizontalScrollableState`（rememberScrollableState）实现三日视图的连续左右滚动
   - 三日视图：使用 `Modifier.scrollable(horizontalScrollableState, Orientation.Horizontal)` 实现连续滚动
   - 日视图：保留翻页手势（`detectHorizontalDragGestures`，左右滑动超过56px切换日期）
   - 添加 `horizontalOffsetPx` 状态变量（`rememberSaveable`）跟踪滚动偏移
   - 计算 `effectiveHorizontalOffsetPx`：三日视图使用 `horizontalOffsetPx`，日视图使用 `selectedIndex * dayColumnWidthPx`
   - `timelineFocusIndex` 根据 `effectiveHorizontalOffsetPx` 动态计算当前显示的中心日期
   - 移除 HorizontalPager 相关的 `LaunchedEffect`（scrollToPage、currentPage 监听）
   - 所有子组件统一使用 `timelineHorizontalOffsetPx` 实现视图偏移

2. 技术实现细节：
   - `horizontalScrollableState` 的 lambda 更新 `horizontalOffsetPx`，限制在 `[0, maxHorizontalOffsetPx]` 范围内
   - `visibleRange` 根据 `timelineFocusIndex` 动态计算，包含中心日期前后各1天加上 overscan
   - `visibleTimedEventPlacements` 直接调用 `buildTimedEventPlacementsForDays(events, visibleDays)`，简化数据流
   - 手势处理：日视图积累 `totalDrag`，结束时判断方向并调用 `shiftViewBy(±1)`

3. 修改文件：`CalendarPanel.kt`（移除 HorizontalPager，恢复 scrollable + 手势）、`build.gradle.kts`（版本1.14.9/329）

4. 数据库版本保持 `28`。验证通过：`assembleDebug` 成功，APK 元数据确认 1.14.9/329。

5. **历史背景**：版本 1.13.10（提交 1d39fa4，2026-05-22）引入 HorizontalPager 用于"降低大量列表滚动复用成本"和"修正日历拖拽翻页冲突"。但用户反馈翻页体验不如之前的连续滚动，因此本次恢复到 1.13.9 之前的实现方式。

## What Changed In The Latest 1.14.8 Patch

1. **每日看板卡片紧凑度优化**（参考飞书日历设计）：大幅减小卡片内边距和间距，提升信息密度，视觉更简洁：
   - 倒计时卡片（DashboardChrome.kt L1441）：padding `14dp/12dp` → `10dp/8dp`，颜色条 `4dp` → `3dp`，高度 `44dp` → `38dp`，水平间距 `14dp` → `10dp`，标题最大行数 `2` → `1`，字重 `Bold` → `SemiBold`，字号 `titleMedium` → `15sp`，副标题 `bodySmall` → `12sp`，行间距 `4dp` → `2dp`
   - 日程列表卡片（DashboardChrome.kt L1773）：padding top/bottom `12dp` → `8dp`，end `10dp` → `8dp`，颜色条 `5dp` → `3dp`，水平间距 `16dp` → `10dp`，标题最大行数 `2` → `1`，字重 `Bold` → `SemiBold`，字号 `titleMedium` → `15sp`，时间副标题颜色 `onSurface` → `onSurfaceVariant`，字号 `bodyMedium` → `12sp`，字重 `Medium` → `Normal`，行间距 `4dp` → `2dp`

2. 修改文件：`DashboardChrome.kt`（两处卡片组件）、`build.gradle.kts`（版本1.14.8/328）

3. 数据库版本保持 `28`。验证通过：`assembleDebug` 成功，APK 元数据确认 1.14.8/328。

4. **未完成问题**：
   - 日历界面仍然是翻页滚动（HorizontalPager），不是连续滚动 - 此问题需要专门设计方案，不能仓促修改（1.14.6的教训）

## What Changed In The Latest 1.14.7 Emergency Patch

## What Changed In The Latest 1.14.7 Emergency Patch

1. **紧急回退滚动逻辑破坏性改动**：1.14.6 尝试将 `HorizontalPager` 改为 `horizontalScroll` 实现连续滚动，但导致日历界面显示空白且日期显示为错误的 2024年6月。根因是移除 HorizontalPager 后，状态管理代码（`timelineScrollState` + `scrollBasedIndex`）完全失效，因为视图层并未真正应用 `horizontalScroll` 修饰符，导致 `timelineScrollState.value` 始终为0，`scrollBasedIndex` 计算错误，`visibleRange` 和 `horizontalOffsetPx` 传值混乱。已完全回退到 1.14.4 的 HorizontalPager 实现。

2. **保留有效的修复**（从 1.14.6 中挑选）：
   - 日程高度修复（3处）：`coerceAtLeast(48.dp)` → `24.dp`（L2079），`48.dp.toPx()` → `24.dp.toPx()`（L1879），`coerceAtLeast(20)` → `5`（L2913）
   - 卡片紧凑度优化：垂直 padding `7.dp` → `4.dp`，水平 padding `7.dp` → `6.dp`，颜色条宽度 `4.dp` → `3.dp`，水平间距 `6.dp` → `5.dp`，标题行高 `14.sp` → `13.5.sp`，地点行高 `12.sp` → `11.5.sp`，行间距 `2.dp/1.dp` → `1.dp/0.dp`

3. **桌面Web修复保持不变**（1.14.6 的桌面端改动未受影响）：颜色优先级（自定义优先于分组）、侧边栏对比度提升、勾选框样式优化、颜色选择器动画等改动均有效。

4. 修改文件：`CalendarPanel.kt`（回退滚动逻辑 + 保留高度/间距修复）、`build.gradle.kts`（版本1.14.7/327）

5. 数据库版本保持 `28`。验证通过：`assembleDebug` 成功，APK 元数据确认 1.14.7/327。

6. **经验教训**：移除 Compose 的高级组件（如 HorizontalPager）并替换为底层 API（如 horizontalScroll）需要完整重写状态管理和视图结构，不能只改状态层而不改视图层。分页滚动问题需要后续专门设计方案，不能仓促重构。

## What Changed In The Latest 1.14.6 Patch (ROLLED BACK IN 1.14.7)

此版本因破坏性改动已在 1.14.7 中回退，仅保留高度和间距优化部分。

原本包含的改动：
- 日历日程高度修复（已保留）
- 卡片紧凑度优化（已保留）
- 桌面Web颜色优先级修复（已保留）
- 桌面Web UI优化（已保留）
- HorizontalPager 改 horizontalScroll 实现连续滚动（❌ 已回退，导致显示空白）

## What Changed In The Latest 1.14.4 Patch

Fix four Desktop Web issues the user reported after seeing the 1.14.3 light redesign on the real device: (1) sidebar text was invisible (white text left over from the dark sidebar design while `--sidebar` had been lightened), (2) the hour axis and day columns separated when the window was narrowed (two independent scroll contexts that the responsive breakpoint stacked vertically), (3) the red-line time chip should sit at the far-left axis aligned with the hour marks (matching the phone layout) instead of riding on the red line, and (4) the sidebar should become a collapsible left menu. All four are fixed in 1.14.4, plus an alignment bug discovered during the rework.

1. Sidebar restored to dark blue (`--sidebar: #243246`) with white text — the 1.14.3 light-palette change had lightened `--sidebar` to `#f8fafc` while sidebar text stayed white, making it invisible. Reverted to the Slack/Notion "dark nav + light content" pattern.
2. Calendar restructured to the phone layout model: the hour axis and day columns now live inside a single `#board-scroll` scroll container (two-column grid: 84px axis + timeline wrap), so they scroll together and can never separate. Removed the old separate `position: sticky` `.hour-axis` element and the responsive breakpoint rule that turned it static and stacked the layout.
3. Red-line time chip moved back to the far-left hour axis, aligned with the hour marks (matching the phone `CalendarPanel.kt` layout where the current-time label sits left-aligned in the time column). The chip is no longer embedded in `.current-line`; `renderHourAxis()` regenerates it as `.hour-current-chip` and `renderCurrentLine()` outputs only the red line.
4. Sidebar made collapsible: added a collapse/expand toggle button and `.shell.sidebar-collapsed` CSS state (narrow icon-only sidebar) with JS click handler next to the disconnect button.
5. Alignment bug fixed (found during rework): inside `#board-scroll`, the left `.hour-axis` column and the right timeline column start at the grid top, but the right column's timed grid is pushed down 58px by the sticky `.event-day-headers`. Hour labels and the red chip now add `EVENT_HEADER_HEIGHT` (58px) to their `top`, and `.hour-axis` height is `calc(var(--hour-height) * 24 + 58px)`, so the time marks line up with the day grid. Verified the 58px offset and CSS height are present in the packaged APK.
6. Changed files: `app.css` (sidebar palette, board scroll restructure, collapsible sidebar, chip + axis alignment), `app.js` (hour-axis/red-line rendering, sidebar toggle handler), `index.html` (board-scroll DOM restructure, collapse button), `build.gradle.kts` (version 1.14.4/324).
7. Database version remains `28`. Verification passed: `node --check app.js`, CSS brace-pair check (495/495), `git diff --check`, `assembleDebug`, APK metadata confirmed 1.14.4/324, and grep of packaged APK assets confirmed the 58px offset + axis-height changes shipped. No Kotlin changed this round.
