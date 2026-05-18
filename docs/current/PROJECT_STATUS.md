# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.3"`
  - `versionCode = 221`

## Current Build Facts

- Latest `1.10.2` signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - Published as GitHub Release tag `v1.10.2`
- Latest signed release APK built in this round:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`
- Verification completed for this `1.10.3` continuation:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleRelease`
  - APK metadata inspected: `versionName = 1.10.3`, `versionCode = 221`, output `PaykiTodo-1.10.3-release.apk`
  - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed local signing material and APK outputs are ignored
- Debug APK build note:
  - `./gradlew.bat :app:assembleDebug` is currently blocked by QQ process `53028` holding `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` open. Close the QQ file preview / transfer to allow Gradle to delete the old debug output directory.
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, APK/AAB outputs, API keys, tokens, and private Base URLs must stay out of Git
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is being advanced from `1.10.2 / 220` to `1.10.3 / 221` for Planning Desk import correctness and desktop-web event color parity.

Most important current baseline facts:

- Database version remains `17`; no migration is required for this round.
- Active no-DDL todos are still treated as today todos across phone daily board, Android widget board query, desktop board, and desktop todo management.
- Countdown-enabled todos use their DDL time as the target; countdown-enabled events use their start time.
- Countdown targets whose exact target time has passed are filtered out before board / widget / desktop rendering.
- Planning Desk event import defaults to event-only. A linked todo is created only if the user explicitly enables the preview option.
- Planning Desk linked todos no longer receive a fixed auto-generated note.
- Planning Desk local parser and AI cleanup both treat `@地点` as event location text, not a required prefix or title content.
- Desktop-web event editor now exposes compact preset color swatches matching the phone editor.

## Immediate Manual Verification Targets

1. Planning Desk local rule import:
   - `10:00-12:00 自习 @图书馆3楼`
   - `10:00-12:00, 【课程】习思想，"@主楼B1-412"`
   - `10:00-12:00 自习 地点：图书馆3楼`
2. Planning Desk AI import:
   - verify title, location, group, and linked-todo checkbox defaults.
3. Desktop web:
   - event color preset swatches update the color input and persist after saving.
4. Regression:
   - normal event import does not create an unwanted todo;
   - manually linked todo has DDL equal to event end time and no fixed generated note.
