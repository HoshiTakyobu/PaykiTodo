# Current Task

## Active Development Focus

The current round has produced a `1.6.52` baseline. The focus was fixing the desktop web regression where clicking an event card did not open the event editor.

Completed in this round:

1. Desktop web event-card click lookup now compares event IDs as strings instead of relying on strict numeric equality.
2. Event-card click handling now prevents default handling and stops propagation so the click cannot fall through into blank timeline creation.
3. Version and current docs were synchronized for `1.6.52`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.52-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. open the desktop web event timeline
4. click an existing event card and verify the event editor opens
5. save a small edit, refresh, and verify the edit persists
6. verify clicking blank timeline space still opens the create-event editor

## Repository-Verified Notes

The current code baseline includes these specific `1.6.52` changes:

1. `app/build.gradle.kts` is bumped to `1.6.52 / 124`.
2. `app/src/main/assets/desktop-web/app.js` renders event IDs as escaped string attributes.
3. `app/src/main/assets/desktop-web/app.js` adds `sameId` / `findEventById` and uses them for card-click editing.
4. Event-card clicks call `preventDefault()` and `stopPropagation()` before opening the editor.

## What Not To Do Immediately

- do not reintroduce inline edit/delete buttons on desktop event cards
- do not reintroduce the separate all-day strip above the desktop event timeline
- do not reintroduce large button groups for enum-like Settings choices
- do not describe the internal volume as a true Android system-level PaykiTodo volume channel
- do not change 工作模式 into a normal ringing mode unless explicitly requested
- do not enable temporary system volume boost by default
- do not move desktop Web resources back into Kotlin raw strings
- do not scan the whole workspace outside this repo
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.52` verification task.
