# Current Task

## Active Development Focus

Active goal: implement `docs/goals/2026-05-18-paykitodo-1.11.0-revised-goal.md` from the current `1.10.3 / versionCode 221` baseline, plus the user's additional Android widget requirements.

The first completed slice focuses on the visible widget regressions before the larger `1.11.0` feature work:

1. `今日看板` Android widget removes the top menu/title/date header.
2. `今日看板` Android widget uses provider-owned minute refresh instead of relying on the system `updatePeriodMillis` floor.
3. Independent `倒数日` widget becomes a scrollable RemoteViews list instead of three fixed rows.
4. Independent `倒数日` widget uses larger readable countdown/title text, multi-line title/meta support, dynamic accent strips, and row deep links.
5. Do not push to GitHub unless the user explicitly asks.

## Completed In This Round

### Widget slice

1. `今日看板` widget actual layout and launcher preview no longer show the three-line icon, `每日看板` title, or date subtitle.
2. `今日看板` widget now schedules minute ticks through `AlarmManager`, refreshes the RemoteViews collection on each tick, and declares `updatePeriodMillis=0`.
3. `倒数日` widget was converted from three hard-coded rows to `CountdownWidgetService` + `ListView`.
4. `倒数日` widget rows now use a soft daily-board-style card, dynamic accent strip / countdown color, larger countdown text, multi-line title and meta text, and direct row deep links to the todo / event detail.
5. `倒数日` widget picker preview now matches the new card structure.

### Previous `1.10.3` Planning Desk pass

1. Local Planning Markdown parsing recognizes inline `@地点`, quoted `"@地点"`, and `地点：...` event locations without polluting event titles.
2. Local natural-event candidates default `createLinkedTodo=false`.
3. AI recognition defaults event `createLinkedTodo=false` and the system prompt only allows linked todos when the user explicitly asks.
4. AI result cleanup moves `@地点` out of event titles when the model forgot the `location` field.
5. Phone-side Planning Desk import and desktop-sync Planning Desk import no longer write the fixed auto-generated note into linked todos.
6. Desktop-web Planning Desk preview uses the clearer label `同步创建以日程结束时间为 DDL 的待办任务`.
7. Desktop-web event editor renders the same eight compact color swatches as the phone event editor and syncs the active swatch with the color input.
8. Unit tests cover default-off linked todos, comma-separated quoted locations, `@图书馆3楼` cleanup, and AI group sanitization.
9. Version metadata remains `1.10.3 / 221`; the full `1.11.0` version bump is still pending.

## Verification Completed This Round

### Widget slice

1. `./gradlew.bat :app:compileDebugKotlin` passed.
2. Static search confirmed removed widget header IDs and old fixed countdown row IDs are no longer referenced in `app/src/main/java` or `app/src/main/res`.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`.
5. Debug APK metadata confirms `versionName = 1.10.3`, `versionCode = 221`.

### Previous `1.10.3` Planning Desk pass

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:testDebugUnitTest` passed.
3. `./gradlew.bat :app:assembleRelease` passed and produced `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`.
4. Release APK metadata confirms `versionName = 1.10.3`, `versionCode = 221`.
5. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer.
6. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed signing material and APK outputs are ignored.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the physical phone.
2. Add / resize the `今日看板` widget and confirm the removed top header, minute refresh, and cross-day date/list update behavior on the launcher.
3. Add / resize the `倒数日` widget and confirm scroll behavior, readable multi-line rows, and row deep links on the launcher.
4. In Planning Desk, test:
   - `10:00-12:00 自习 @图书馆3楼`
   - `10:00-12:00, 【课程】习思想，"@主楼B1-412"`
   - `10:00-12:00 自习 地点：图书馆3楼`
5. Confirm those lines import as events with clean titles and correct locations.
6. Confirm a normal event import creates only the event.
7. Confirm manually enabling linked todo creates a todo with DDL equal to event end time and no fixed generated note.
8. In desktop web, confirm the same Planning Desk cases work through AI preview and local fallback.
9. In desktop web event editor, confirm preset colors and custom color both save correctly.

## Remaining 1.11.0 Work

The full goal remains active. Major remaining slices:

1. Remove focus mode entirely.
2. Add event check-in / time tracking.
3. Add Planning Desk AI image recognition and simplify shortcut bar.
4. Redesign drawer / todo page around multi-group filtering.
5. Complete performance / robustness tasks, R8 release validation, docs, schema, and version bump to `1.11.0 / versionCode 222`.
