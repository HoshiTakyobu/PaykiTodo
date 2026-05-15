# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.0` / `versionCode 183`, focused on turning Planning Desk from a one-shot importer into a persistent upstream planning document with stable item mappings, refresh/postpone/undo operations, and explicit conflict handling across phone and desktop.

## Completed In 1.8.0

1. Planning Desk import now creates `planning_line_mappings`, linking each imported Markdown line to the created todo or event instead of relying only on `#imported`.
2. Mapping relocation uses `PlanningLineMatcher` with normalized fingerprints plus fuzzy text matching, so inserted lines or edited wording do not immediately break the linkage.
3. Mapping status sync now classifies each imported line as `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, or `CONFLICT`.
4. Phone-side Markdown preview now renders those mapping states directly with pills and strike-through treatment, and completed imported tasks can be manually written back to source Markdown as `- [x]` through `同步完成状态到原文`.
5. `refreshPlanningImportedItems` now refreshes only unfinished active mappings, supports current-section or whole-document scope, skips completed/canceled items, and marks missing or diverged mappings as orphaned/conflict instead of silently overwriting.
6. `postponePlanningImportedItems` now batch-shifts unfinished imported items and the corresponding Markdown time text together, with scope options for current section or document tail.
7. The latest import / refresh / postpone batch can now be undone; undo also restores Markdown marker or shifted-time state when applicable.
8. Conflict handling now supports both directions: document overwrites item, or current item state rewrites the original planning line.
9. Desktop-web Planning Desk now exposes the same mapping-aware loop through `/api/planning/*`, including mapping preview, refresh, postpone, undo, and conflict actions.
10. Planning note backup / restore now includes mapping records, and Room moved to database version `11` with `MIGRATION_10_11` creating `planning_line_mappings`.
11. Version metadata is now `1.8.0` / `versionCode 183`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` succeeded.
2. `./gradlew.bat testDebugUnitTest` succeeded.
3. `./gradlew.bat assembleDebug` succeeded.

## Immediate Practical Next Steps

When device-testing the `1.8.0` APK, verify:

1. Importing from Planning Desk creates visible mapping state pills in phone preview and desktop preview.
2. Completing or canceling imported items changes mapping status to `已完成` / `已取消`.
3. `同步完成状态到原文` rewrites only completed imported task lines to `- [x]`.
4. `刷新已导入项` skips completed/canceled items and only refreshes unfinished mapped items.
5. `批量顺延` updates both formal items and Markdown time text, and `撤销上次操作` restores the latest import / refresh / postpone batch.
6. Editing an imported item outside Planning Desk produces `已手动修改`, and both “以文档为准覆盖” and “以事项为准更新文档” resolve it correctly.
7. Desktop Planning Desk shows the current note title, empty-state guidance, mapping preview, refresh, postpone, undo, and conflict actions after reconnecting to the phone.

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

No external file is required for the current `1.8.0` verification task. The original `app/build/outputs/apk/debug/goal.md` objective file is no longer present in the working tree, so current completion checks must rely on the implemented code, synchronized docs, and the verified build/test outputs.
