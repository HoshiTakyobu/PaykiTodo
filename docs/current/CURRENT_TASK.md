# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.11` / `versionCode 205`.

Primary goal: make no-DDL tasks usable as today tasks, reduce idle desktop-sync cost, clarify multi-IP sync addresses, and improve the cramped AI-provider configuration UI.

## Completed In This Round

1. Active no-DDL todos are now included in `今日待办` instead of being treated as future / floating tasks.
2. `DailyBoardSnapshotBuilder` now includes no-DDL active todos, so the phone daily board, Android widget snapshot, and desktop daily-board snapshot share the same semantics.
3. Desktop Web todo management now places no-DDL active todos under `今日待办` and removes the separate `未设置 DDL` section.
4. Planning Desk local parsing recognizes plain bullet items (`- ...`, `* ...`, `• ...`) as no-DDL todo candidates.
5. Desktop sync records the first authorized API request as a successful desktop connection.
6. `DesktopSyncService` automatically disables desktop sync and stops the local server if no authorized client connects within 5 minutes after enabling sync.
7. Desktop sync IP addresses are sorted with likely Wi-Fi `192.168.*` addresses first; Settings explains that `10.*` / `172.*` addresses can be VPN / hotspot / virtual adapter addresses, not localhost.
8. Settings -> AI 调用配置 provider rows were redesigned as summary cards with a visible enable switch and a more menu for edit / reorder / delete, reducing horizontal crowding.
9. Wiki, README, CHANGELOG, and current docs were synchronized for no-DDL today semantics, plain-bullet Planning Desk input, desktop sync auto-shutdown, and IP-address explanation.
10. Version metadata moved to `1.9.11` / `versionCode 205`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.11-debug.apk`.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` confirms `versionCode=205`, `versionName=1.9.11`, and `outputFile=PaykiTodo-1.9.11-debug.apk`.

## Verification Still Needed Before Release

1. Device-test no-DDL todos:
   - create a normal todo with DDL disabled
   - confirm it appears under `今日待办`
   - confirm it has no reminder / recurrence behavior until DDL is added
2. Device-test Planning Desk:
   - write `- 想办的事`
   - run recognition
   - confirm it imports as a no-DDL todo and appears in today todos
3. Device-test desktop sync:
   - enable sync and connect with the correct token within 5 minutes; it should stay running
   - enable sync and do not connect with the correct token; it should auto-disable after 5 minutes
4. Device-test Settings -> AI 调用配置 on a narrow phone screen for provider-row readability.

## Immediate Practical Next Steps

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.11-debug.apk` on the phone.
2. Device-test no-DDL todo placement, Planning Desk plain-bullet import, desktop-sync 5-minute auto-shutdown, and AI-provider card readability.
3. Do not push unless the user explicitly asks.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
