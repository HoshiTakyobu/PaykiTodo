# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.7` / `versionCode 190`, focused on correcting the Android `今日看板` launcher widget after visual review: the widget should look like the in-app daily board rather than a generic list of large event cards.

## Completed In 1.8.7

1. The widget root now uses a light/dark gradient board background instead of a plain rounded system-card surface.
2. The widget content starts with a compact greeting card (`早上好/下午好/晚上好，Payki`) plus the current date guidance, matching the in-app board's greeting-card direction.
3. Todo rows now show a left vertical group-color strip derived from the todo's task group, so todo cards no longer look like plain text rows.
4. The schedule area is now one aggregated board card:
   - left date block for today
   - right-side today event rows
   - today empty / all-finished message
   - `明天` label
   - tomorrow event rows or `明天暂无日程` guidance
5. Individual schedule rows inside the aggregated schedule card still deep-link to their corresponding calendar event.
6. Todo / announcement deep links remain intact.
7. Version metadata is now `1.8.7` / `versionCode 190`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

## Immediate Practical Next Steps

Before final completion, create a focused local commit with the required `完成内容概要：` bullet-list body. Do not push unless the user explicitly asks.

After installing the `1.8.7` APK on device, verify:

1. The launcher widget visually resembles the in-app daily board more than the old bordered/list card.
2. The sample state from the user screenshot shows a greeting card, `今日待办（0）`, the empty todo card, `今日日程（0）`, and one schedule board card containing today / tomorrow content.
3. Widget resizing still reveals more content without clipping.
4. Todo / event / announcement row taps still deep-link to the correct in-app screen.
5. Dark-mode widget background, cards, and text remain readable on the actual launcher.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
