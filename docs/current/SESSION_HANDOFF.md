# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.15`
  - `versionCode = 335`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.15-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Active Goal

Finish and verify `1.14.15 / versionCode 335`: Planning Desk reminder-clause parsing, AI-source status visibility, and Desktop Sync LAN-address filtering.

## Live Desktop Sync Finding

The user-provided live request to `POST /api/planning/parse` with:

```text
06-25 14:00-22:00 【合唱】大艺展省赛录音 @成都大学 提醒时间：提前1h，06-24 22:10
```

returned `message = AI 识别：Deepseek；请在预览中确认` on the connected phone. The current phone response already produced:

- title: `【合唱】大艺展省赛录音`
- location: `@成都大学`
- reminder offsets: `950,60`

So the observed “提醒时间进标题” failure is most likely from local fallback parsing, an older installed APK, or an imperfect AI response at the time. The code patch intentionally fixes both local parsing and AI post-processing defenses.

## What Changed In The 1.14.15 Patch

1. **Planning Desk reminder clauses**
   - Natural event lines can now contain tail reminders such as `提醒时间：提前1h，06-24 22:10`.
   - The reminder phrase is removed from the event title.
   - The phrase is converted to reminder offsets relative to the event start time.

2. **Shared reminder syntax**
   - Reminder input now accepts relative lead-time tokens: `提前1h`, `提前 30 分钟`, `提前2天`.

3. **AI post-processing**
   - If an AI model returns `提醒时间：...` inside the title, the app strips it from the title.
   - If the AI omitted or defaulted reminder offsets, the app can recover offsets from the returned title or original source line.
   - The AI prompt now explicitly tells providers how to handle natural reminder phrases.

4. **AI settings status**
   - Settings -> AI 调用配置 now shows an overall status card:
     - complete providers usable for Planning Desk recognition;
     - complete vision-capable providers;
     - enabled providers with missing required fields.
   - The card is local-only and does not auto-call every API on page open.

5. **Desktop Sync address filtering**
   - The sync status now prefers active Wi-Fi IPv4 addresses.
   - Without active Wi-Fi, it may show likely hotspot / Ethernet `192.168.*` addresses.
   - Mobile-data, VPN, and virtual-network `10.*` / `172.*` addresses are no longer shown as ordinary connection URLs.
   - Settings explains when no reachable LAN address is detected.

6. **Version metadata**
   - App metadata moved to `1.14.15 / versionCode 335`.
   - Database version remains `28`; no Room schema migration was added.

## Validation

Passed so far:

- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.data.ReminderTextParserTest --tests com.example.todoalarm.data.PlanningMarkdownParserTest --tests com.example.todoalarm.data.PlanningAiRecognizerTest`
- `./gradlew.bat --no-daemon :app:testDebugUnitTest`
- `node --check app/src/main/assets/desktop-web/app.js`
- `git diff --check`
- `./gradlew.bat --no-daemon :app:assembleDebug`
- APK metadata inspection for `versionName = 1.14.15`, `versionCode = 335`, output `PaykiTodo-1.14.15-debug.apk`

Remaining before final handoff:

1. Commit the round with a detailed Chinese message.

## Next Session Notes

1. Do not report or hand off a new installable APK without confirming the APK metadata matches `1.14.15 / 335`.
2. Do not commit the user-provided desktop sync token or any AI API key/Base URL.
3. If Desktop Sync address filtering is refined further, keep phone-hotspot behavior in mind: ordinary mobile-data/VPN addresses should stay hidden, but hotspot `192.168.*` addresses may still be useful when a computer is connected to the phone hotspot.
