# Current Task

## Active Development Focus

The current round has produced a `1.6.51` baseline. The focus was fixing daily-board tomorrow copy and simplifying desktop web event timeline interactions.

Completed in this round:

1. Daily board now always shows a tomorrow schedule section.
2. If tomorrow has no events, the board explicitly says `明天暂无日程`.
3. Desktop web event timeline cards no longer show inline edit/delete buttons; clicking a card opens the existing event editor, where edit/delete actions remain available.
4. Desktop web event cards now prefer group color for display, matching the todo color model.
5. Desktop web event timeline no longer renders the separate all-day strip above the timed grid.
6. Version and current docs were synchronized for `1.6.51`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.51-debug.apk`
2. open daily board and verify the schedule card shows `明天暂无日程` when tomorrow has no events
3. open desktop web event timeline and verify the separate all-day strip above the grid is gone
4. click a desktop web event card and verify it opens the event editor
5. verify desktop web event cards no longer show inline edit/delete buttons
6. verify desktop web event cards use group colors first

## Repository-Verified Notes

The current code baseline includes these specific `1.6.51` changes:

1. `app/build.gradle.kts` is bumped to `1.6.51 / 123`.
2. `DashboardChrome.kt` shows the tomorrow schedule section even when tomorrow has no events.
3. `app/src/main/assets/desktop-web/index.html` removes the all-day strip markup from the event timeline panel.
4. `app/src/main/assets/desktop-web/app.js` removes inline edit/delete controls from event cards, keeps card-click editing, and prefers group color for event display.

## What Not To Do Immediately

- do not reintroduce large button groups for enum-like Settings choices
- do not describe the internal volume as a true Android system-level PaykiTodo volume channel
- do not change 工作模式 into a normal ringing mode unless explicitly requested
- do not enable temporary system volume boost by default
- do not move desktop Web resources back into Kotlin raw strings
- do not scan the whole workspace outside this repo
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.51` verification task.
