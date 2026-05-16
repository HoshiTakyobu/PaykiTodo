# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.9` / `versionCode 192`, focused on correcting the Android `今日看板` launcher widget after visual review: the widget should look like the in-app daily board rather than a generic list surface.

## Completed In 1.8.9

1. The widget root now layers daily-board background art with a light/dark scrim instead of only using a plain gradient/list-like surface.
2. The widget header now resembles the in-app daily-board top bar with a circular menu button, `每日看板` title, and current-date subtitle.
3. Announcement rows now render as an orange rounded banner rather than a generic white/gray card.
4. Greeting, empty, todo, and schedule cards use stronger light/dark surfaces, wider color strips, larger section titles, and daily-board-like spacing.
5. The aggregated schedule card still shows today / tomorrow in one board card, with the left date block and row color strips preserved.
6. Todo / event / announcement deep links remain intact.
7. Version metadata is now `1.8.9` / `versionCode 192`.

## Follow-up Widget Visual Tightening

After another visual review, the widget RemoteViews resources were tightened again to better match the phone-side daily board:

1. The widget root uses larger outer padding, a lighter board scrim, and title/subtitle shadows so the wallpaper-backed board reads closer to the in-app surface.
2. Greeting, empty, todo, and schedule cards now use 28dp-style rounded surfaces with stronger opacity.
3. Section titles are larger and shadowed, matching the visual weight of the app's `今日待办` / `今日日程` blocks.
4. Schedule rows have more breathing room, narrower vertical event strips, and larger titles so the schedule board no longer looks like a compact table.
5. Day/night card opacity and inner schedule surfaces were adjusted for real launcher readability.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

## Follow-up Verification

1. `./gradlew.bat :app:processDebugResources`
2. `./gradlew.bat assembleDebug`
3. `git diff --check`

## Immediate Practical Next Steps

1. Install the latest locally built APK and re-add / refresh the launcher widget to verify the new card rhythm on a real launcher.
2. Because the worktree also contains separate uncommitted `1.9.0` focus-session changes, keep any widget commit focused and do not push unless the user explicitly asks.

After installing the latest APK on device, verify:

1. The launcher widget visually resembles the in-app daily board background/topbar/card hierarchy more than the old bordered/list card.
2. The sample state from the user screenshot shows the circular menu header, greeting card, `今日待办（0）`, the empty todo card, `今日日程（0）`, and one schedule board card containing today / tomorrow content.
3. Widget resizing still reveals more content; because RemoteViews cannot fully guarantee child bitmap clipping on every launcher, check for square-corner background bleed specifically.
4. Todo / event / announcement row taps still deep-link to the correct in-app screen.
5. Dark-mode widget background, cards, and text remain readable on the actual launcher.

## Active Goal Version Note

Two goal docs remain under `docs/goals/` for the future focus-session and AI-report work. They were written before the `1.8.8 / 191` and `1.8.9 / 192` widget fixes. If that goal resumes, continue from the current code version and do not reuse old versionCodes.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
