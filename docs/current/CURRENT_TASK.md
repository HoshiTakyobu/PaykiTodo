# Current Task

## Active Development Focus

Active immediate task: finish `1.14.15 / versionCode 335` fixes for Planning Desk reminder recognition, AI-source status visibility, and Desktop Sync LAN-address filtering.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs. The repository already ignores `keystore.properties`, `release/`, `*.apk`, `*.jks`, `.env*`, and local temp files.

## Current Round Scope

This round addresses three user-reported issues:

1. A Planning Desk line such as `06-25 14:00-22:00 【合唱】大艺展省赛录音 @成都大学 提醒时间：提前1h，06-24 22:10` must not put the reminder phrase into the event title.
2. Settings -> AI 调用配置 should give an overall summary of usable AI sources instead of forcing the user to inspect each provider one by one.
3. Settings -> Desktop Sync should not show mobile-data / VPN / virtual-network `10.*` or `172.*` addresses as ordinary browser connection URLs when the phone is not on a reachable LAN.

## Required Behavior

### Planning Desk Reminder Recognition

1. Local rule parsing should extract tail reminder clauses such as `提醒时间：提前1h，06-24 22:10` from natural event lines.
2. Reminder clauses should be converted to reminder offsets relative to the event start time.
3. Reminder clauses should not remain in the event title.
4. The shared reminder parser should support relative lead-time tokens such as `提前1h`, `提前 30 分钟`, and `提前2天`.
5. AI recognition post-processing should defensively strip reminder clauses from returned titles and recover offsets from either the returned title or the original source line.

### AI Source Status

1. Settings -> AI 调用配置 should show how many enabled providers are complete enough for Planning Desk recognition.
2. The same status surface should show how many complete providers support image recognition.
3. Incomplete enabled providers should be counted clearly.
4. The status card should not automatically call every API on page open, avoiding unexpected token or quota use.

### Desktop Sync Address Filtering

1. Desktop Sync status should prefer the active Wi-Fi network's IPv4 addresses.
2. If no active Wi-Fi address exists, it may show likely hotspot / Ethernet `192.168.*` addresses.
3. Mobile-data, VPN, and virtual-network addresses should not be shown as normal connection URLs.
4. Settings should explain when no reachable LAN address is detected.

## Verification Required Before Completion

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
2. Targeted parser tests for reminder input, Planning Desk local parsing, and AI post-processing.
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest`
4. `node --check app/src/main/assets/desktop-web/app.js`
5. `git diff --check`
6. `./gradlew.bat --no-daemon :app:assembleDebug`
7. APK metadata check for `versionName = 1.14.15`, `versionCode = 335`.
8. Commit the completed round with a Chinese message that describes user-visible behavior, version bump, and validation.

## Current Status

Implemented so far:

1. `ReminderTextParser` accepts `提前1h` / `提前 30 分钟` / `提前2天`.
2. `PlanningMarkdownParser` extracts natural event reminder clauses and keeps them out of titles.
3. `PlanningAiRecognizer` strips reminder clauses from imperfect AI titles and recovers offsets from source text.
4. Settings AI configuration shows an overall source-status card.
5. Desktop Sync status filters LAN addresses and adds a network hint.
6. Version metadata moved to `1.14.15 / versionCode 335`; database version remains `28`.

Validation completed so far:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
2. Targeted `ReminderTextParserTest`, `PlanningMarkdownParserTest`, and `PlanningAiRecognizerTest`
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest`
4. `node --check app/src/main/assets/desktop-web/app.js`
5. `git diff --check`
6. `./gradlew.bat --no-daemon :app:assembleDebug`
7. APK metadata inspection for `versionName = 1.14.15`, `versionCode = 335`, output `PaykiTodo-1.14.15-debug.apk`

Remaining:

1. Final commit.
