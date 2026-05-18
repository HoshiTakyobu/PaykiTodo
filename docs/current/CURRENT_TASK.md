# Current Task

## Active Development Focus

The current round is PaykiTodo `1.10.3` / `versionCode 221`.

Primary goal: fix Planning Desk and desktop-web issues found after publishing the `1.10.2` release:

1. Planning Desk must extract event locations such as `@图书馆3楼` and `"@主楼B1-412"` into the event location field instead of leaving them in the title.
2. Planning Desk event import must not auto-create a linked todo unless the user explicitly enables that preview option.
3. A manually enabled linked todo must not receive the old fixed note `由规划台日程自动生成，DDL 为日程结束时间。`.
4. Desktop-web event editing should offer the same compact preset event colors as the phone editor while keeping custom color input.
5. Documentation and tests should reflect the default-off linked-todo behavior.

## Completed In This Round

1. Local Planning Markdown parsing now recognizes inline `@地点`, quoted `"@地点"`, and `地点：...` event locations without polluting event titles.
2. Local natural-event candidates now default `createLinkedTodo=false`.
3. AI recognition now defaults event `createLinkedTodo=false` and the system prompt only allows linked todos when the user explicitly asks for them.
4. AI recognition adds local cleanup for imperfect model output: if an event title/source contains an `@地点` and the AI omitted `location`, PaykiTodo moves it into the location field and removes it from the title.
5. Phone-side Planning Desk import and desktop-sync Planning Desk import no longer write the fixed auto-generated note into linked todos.
6. Desktop-web Planning Desk preview uses the clearer label `同步创建以日程结束时间为 DDL 的待办任务`.
7. Desktop-web event editor now renders the same eight compact color swatches as the phone event editor and syncs the active swatch with the color input.
8. Unit tests cover default-off linked todos, comma-separated quoted locations, `@图书馆3楼` cleanup, and AI group sanitization.
9. Version metadata moved to `1.10.3 / 221`.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:testDebugUnitTest` passed.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleRelease` passed and produced `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`.
5. Release APK metadata confirms `versionName = 1.10.3`, `versionCode = 221`.
6. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer.
7. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed signing material and APK outputs are ignored.
8. After QQ released the old debug APK handle, `./gradlew.bat :app:assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`.
9. Debug APK metadata confirms `versionName = 1.10.3`, `versionCode = 221`.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the physical phone.
2. In Planning Desk, test:
   - `10:00-12:00 自习 @图书馆3楼`
   - `10:00-12:00, 【课程】习思想，"@主楼B1-412"`
   - `10:00-12:00 自习 地点：图书馆3楼`
3. Confirm those lines import as events with clean titles and correct locations.
4. Confirm a normal event import creates only the event.
5. Confirm manually enabling linked todo creates a todo with DDL equal to event end time and no fixed auto-generated note.
6. In desktop web, confirm the same Planning Desk cases work through AI preview and local fallback.
7. In desktop web event editor, confirm preset colors and custom color both save correctly.
