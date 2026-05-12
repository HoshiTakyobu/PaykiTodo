# Current Task

## Active Development Focus

The current round has produced a `1.6.44` baseline. The implemented focus was desktop Web UI refinement after the `1.6.43` desktop editing parity work.

Completed in this round:

1. Desktop web todo and event editor modals now use a bottom-sheet-like structure.
2. Editor headers use left cancel, centered title / subtitle, and right save action to better match the phone-side visual direction.
3. Editor fields are grouped into card-like surfaces instead of feeling like a raw admin form.
4. Todo timeline, all-day event, and timed event cards now use lighter borders, less heavy action buttons, and denser spacing.
5. The shared `.hidden` CSS rule now hides destructive buttons in create mode, instead of only hiding modal backdrops.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.44-debug.apk`
2. enable desktop sync on the phone and open the LAN web page from a computer browser
3. connect with the 4-character access key
4. open an existing todo with `编辑` and verify the editor looks like the refined sheet layout
5. open a timed event and an all-day event with `编辑` and verify the same visual language is used
6. create a new todo / event and verify destructive delete buttons are not shown in create mode
7. narrow the browser window and verify the sheet falls back to a bottom-aligned layout without breaking fields

## Repository-Verified Notes

The current code baseline includes these specific `1.6.44` changes:

1. `app/build.gradle.kts` is bumped to `1.6.44 / 116`.
2. `DesktopSyncWebAssets.kt` adds modal handles, sheet-style headers, card-styled editor field containers, and lighter timeline / event action styles.
3. `openModal()` now focuses the primary editor title field first rather than the cancel button.
4. A global `.hidden` rule prevents create-mode delete buttons from leaking into the UI.

## What Not To Do Immediately

- do not claim the desktop Web UI is fully identical to the Android Compose UI
- do not expand backend scope unless a concrete UI parity gap requires it
- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.44` verification task.
