# Current Task

## Active Development Focus

The current round has produced a `1.6.43` baseline. The implemented focus was narrowing the desktop/mobile feature gap for editing:

1. Desktop web now has a `PUT /api/todos/{id}` endpoint for editing existing todos.
2. Desktop web todo cards expose an explicit `编辑` action.
3. The todo modal now switches between create and edit mode and can save or delete an existing todo.
4. Existing todos can be edited from desktop web for title, notes, DDL, reminder time, group, recurrence, ring, and vibration.
5. Timed and all-day event cards expose explicit `编辑` actions while keeping the existing event editor.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.43-debug.apk`
2. enable desktop sync on the phone and open the LAN web page from a computer
3. create or choose an existing todo and click `编辑`
4. change DDL and reminder time, save, then verify the phone reflects the change and reminder is rescheduled
5. click edit on a timed event and an all-day event and verify the event editor opens with fields prefilled
6. verify delete from the todo edit modal still asks for confirmation

## Repository-Verified Notes

The current code baseline includes these specific `1.6.43` changes:

1. `DesktopSyncCoordinator.kt` routes `PUT /api/todos/{id}` to the new todo update flow.
2. The desktop todo update flow reuses repository draft update logic and clears/reschedules reminder artifacts.
3. `DesktopSyncWebAssets.kt` tracks `editingTodoId` and switches the todo modal title/button/delete visibility by mode.
4. Todo cards now render an `编辑` button; event cards and all-day event cards also render explicit `编辑` buttons.
5. `app/build.gradle.kts` is bumped to `1.6.43 / 115`.

## What Not To Do Immediately

- do not claim desktop UI is fully identical to the phone UI yet
- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.43` verification task.
