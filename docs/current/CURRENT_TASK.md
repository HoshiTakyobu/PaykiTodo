# Current Task

## Active Development Focus

The current round has produced a `1.6.45` baseline. The implemented focus was desktop Web resource structure cleanup and documentation.

Completed in this round:

1. Desktop Web HTML / CSS / JS were moved out of `DesktopSyncWebAssets.kt`.
2. The files now live under `app/src/main/assets/desktop-web/`:
   - `index.html`
   - `app.css`
   - `app.js`
3. `DesktopSyncWebAssets.kt` is now a small asset loader with a minimal fallback page.
4. `DesktopSyncCoordinator` still serves `/`, `/index.html`, `/app.css`, and `/app.js`, but now reads from APK assets.
5. `docs/current/DESKTOP_WEB_ARCHITECTURE.md` documents why the APK contains the desktop UI, why this is expected, and how to evolve the structure later.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.45-debug.apk`
2. enable desktop sync on the phone
3. connect from a computer browser with the 4-character access key
4. verify `/`, `/app.css`, and `/app.js` load normally from the phone
5. verify the desktop UI still shows the refined `1.6.44` editor visual style
6. create / edit one todo and one event to confirm API calls still work after the asset move

## Repository-Verified Notes

The current code baseline includes these specific `1.6.45` changes:

1. `app/build.gradle.kts` is bumped to `1.6.45 / 117`.
2. `app/src/main/assets/desktop-web/` contains the desktop browser UI resources.
3. `DesktopSyncWebAssets.kt` reads those assets through `Context.assets`.
4. `DesktopSyncCoordinator.kt` calls `DesktopSyncWebAssets.indexHtml(context)`, `appCss(context)`, and `appJs(context)`.
5. The old Kotlin raw-string desktop UI body has been removed.

## What Not To Do Immediately

- do not reintroduce large desktop HTML / CSS / JS strings into Kotlin
- do not split the desktop Web UI into an independently hosted site unless there is a concrete product reason
- do not claim the desktop Web UI is fully identical to the Android Compose UI
- do not expand backend scope unless a concrete UI parity gap requires it
- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.45` verification task.
