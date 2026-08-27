# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version: `1.15.11 / versionCode 353`
- Database version: `29` (unchanged; compatibility schema retained)
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.11-debug.apk`
- Latest signed release APK available locally: `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`
- Context recovery record: `docs/current/CONTEXT_RECOVERY_V1_15_10.md`

## Completed In This Round

1. Phone, desktop Web, and Planning Desk reminder controls now independently select full-screen and notification delivery. Neither selected means no reminder scheduling; both selected means both surfaces are delivered.
2. Work mode and alarm mode no longer force a notification-only item into the full-screen path; the selected channels remain the source of truth for delivery.
3. Desktop Web calendar hour labels align to the hour rules, and completed event cards keep their original color while using opacity only.
4. Todo batch selection, dark workspace, isolated Desktop Sync polling, and local list rendering optimizations remain from the previous round.

## Validation Evidence

- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `node --check app/src/main/assets/desktop-web/app.js`, and `git diff --check` passed.
- Reused `emulator-5554` / AVD `Pixel_8`, installed `PaykiTodo-1.15.10-debug.apk`, and checked:
  - Todo page dark workspace uses deep blue-gray layers;
  - long-pressing a todo changes the title to `多个待办 · 已选 1 项`;
  - `取消所选` / `完成所选` remain fixed after scrolling from the first selected item to approximately `Perf Todo 020`;
  - system Back exits selection mode and returns to the ordinary `待办` title.
- 102-todo fast-scroll benchmark, ten scripted swipes:
  - before: 300 frames, 39 janky (13.00%), p95 38ms, p99 57ms, Slow UI thread 31;
  - after: 311 frames, 34 janky (10.93%), p95 34ms, p99 44ms, Slow UI thread 23.

## Next Recommended Round

1. Install the rebuilt `PaykiTodo-1.15.11-debug.apk` on the physical phone and verify no-channel, notification-only, full-screen-only, and combined reminder behavior.
2. If physical-device jank remains substantial, capture a Perfetto/System Trace before introducing paging, baseline profiles, or further card simplification; do not guess from emulator-only numbers.
3. Recheck Daily Board long-press selection with a real today/no-DDL todo because the retained emulator stress dataset contains future items and the final check focused on the Todo page.

4. Before the next feature round, commit the completed `1.15.11` reminder/calendar changes with a Chinese behavior-focused message. Do not push unless the user explicitly asks.

## Notes

- `emulator-5554` may still be running.
- Emulator stress data is local-only and must not be committed.
- Never commit Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, screenshots, backups, test databases, or local diagnostic logs.
