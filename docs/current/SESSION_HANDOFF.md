# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.7`
  - `versionCode = 327`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.7-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.13.11-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11`

## Active Goal

Emergency fix for calendar display failure caused by 1.14.6 scrolling logic changes.

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
