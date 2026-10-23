# Current Task

## Completed: 1.16.25 / 390 两端体验反馈

按照 [REQUEST_2026-10-23.md](REQUEST_2026-10-23.md) 完成独立多日历、两步网格创建、字体自定义、倒数日保留规则与完成询问、重复待办折叠、设置重排、Android 名称纠正及相关两端逻辑。开始基线为 `6f3800f / 1.16.24 / 389`，下方是历史已完成轮次，不能替代本轮验收。

最终实现与验证见 [VALIDATION_1.16.25.md](VALIDATION_1.16.25.md)。新版Debug/签名Release/Windows成品均已构建，原模拟器业务数据保留。按项目规则本地中文提交，不推送或发布。

## Completed: 1.16.24 / 389 字体、日历与真实客户端验收

需求见 [REQUEST_2026-10-22_TYPOGRAPHY.md](REQUEST_2026-10-22_TYPOGRAPHY.md)。从 6005a10 恢复未提交修改，按最终批注完成无衬线标题/正文/导航/用户名、手机抽屉加粗、Windows 搜索和账号顺序、一体清单图标、参考日历主体与蓝色交互。清刻本悦宋仅用于问候；补修 Windows 辅助功能完成/恢复持久化，以及提醒/日历深色对比度。

最终 Debug/签名 Release/Windows 已重建，Android 190 单测及 Lint、Windows 22 项回归与一条 14 阶段 UIA 流程、正式 EXE 启动与搜索检查通过。专门测试账号完成 Windows 创建、手机修改回传、Windows 完成到手机历史、两端到点提醒及手机完成回传。原始手机 557 个文件已恢复并逐项匹配，78 个额外运行文件保留，测试数据另存备份。证据、失败记录和准确范围见 [VALIDATION_1.16.24.md](VALIDATION_1.16.24.md)。

本轮要求的本地实现和验收已收尾；不推送或发布。真机 OEM 长待机/锁屏/振动、物理键鼠全链路和既有 Windows 功能差异仍是后续边界，不作为本轮已通过事项。

## Completed: 2026-10-22 七项修复 / 1.16.23

- 上一完成轮次为 main / 8bbb136、1.16.22 / 387；恢复会话时已有未提交的 1.16.23 / 388 修改。当前 Room31 不变，需求见 [REQUEST_2026-10-22.md](REQUEST_2026-10-22.md)。
- 自动续传传输期间的新写入，核对遗漏的本机变化，阻止旧副本差异在后续自动轮询中绕过确认；独立副本完成状态传播、断网恢复和冲突副本保留已验证。
- 已登记近 10 分钟提醒恢复且不重复；过去的 DDL、日程与自定义提醒可保存，新增历史事项不补响。
- 三端/Wiki 内置授权中文字体，修复 Windows 分组显示、任务排版与重复账号入口；Android 中文名称、待办快捷方式和深链接已实现。
- 190 Android 单测、32 个设备用例分批复验、22 Windows 回归、1 条14阶段隔离控件流程、LAN 网页回归通过；Lint 0 errors/131 warnings。Debug/签名 Release/EXE 已构建并核对版本、哈希、签名；详见 [VALIDATION_1.16.23.md](VALIDATION_1.16.23.md)。
- 原 Pixel_8 数据恢复后 531/531 文件匹配，26 个新增运行文件保留；本次接续只读复核和文档收尾，不再重建安装包或改写用户数据。文档与代码按项目规则中文提交，不推送、发布或修改生产。
- 剩余边界：真机 OEM 长待机/锁屏/振动、厂商语音识别、Windows 完整真实账号验证与既有功能差异。没有替用户决定真实冲突数据取舍；手机较新而电脑仍旧时按 [同步恢复说明](SYNC_RECOVERY_1.16.23.md)检查。

## Completed: 1.16.22 / 387 Windows 排版与不干扰测试 (2026-10-19)

- 从 main / 429f6da、1.16.21 / 386、Room31 开始；上一轮实际操作验收已提交。
- 统一原生 WPF 导航、字体层次、控件对齐、编辑流程、各页面与弹窗；保留原看板图片、问候字体和品牌色。公共弹窗和托盘必须使用 PaykiTodo 原图标。
- 默认测试不注入全局鼠标/按键、不修改剪贴板、不切换用户桌面。通过独立 Win32 桌面上的真实 WPF 控件自动化接口验证；物理输入验收仍单独明确 opt-in，不混称。
- 升级 Android 1.16.22 / 387 与 Windows 1.16.22.0，构建 Debug、签名 Release 和独立 EXE；验证保存/取消、导航草稿、清单、主题、提醒、同步安全回归和实际渲染。
- 不推送、发布、修改生产服务器；保持显式退出锁定、过期离线提醒、revision/dirty 与传输期间新写入安全规则。
- 已完成上述改动、Debug/签名 Release/Windows 构建和本地验证：19 Windows 回归（含成品启动）、1 项14阶段 UIA 流程、186 Android 单测、Lint、签名验证、原 Pixel_8 升级启动。完整证据、复现命令和边界见 [VALIDATION_1.16.22.md](VALIDATION_1.16.22.md)。
- 修复实际渲染发现的日期/时间文字裁切和窄窗口规划按钮越界，增加可视区域断言；账号、AI 和隐私弹窗也覆盖排版/取消检查。不是仅换配色，也不宣称手机所有功能已在 Windows 对齐。
- 默认不再接管用户系统输入；物理命中/键盘全链路需另行安排。保留已有 OEM、Windows 农历/OCR/多时段创建及账号验证 E2E 的明确缺口，不算作已通过。

## Previous: 1.16.21 / 386 implementation and scoped interactive acceptance (2026-10-18)

The implementation round for [REQUEST_2026-10-17.md](REQUEST_2026-10-17.md) landed at main / 30c3176 from 856e714, Room 31. This is not a claim of complete cross-platform parity or all-scenario acceptance. Expired accounts retain offline editing/reminders and dirty data; explicit logout locks local access. Both clients provide encrypted credential memory and confirmed conflict recovery. Android adds persisted network sync jobs and notifications, cross-client checklists allow no title, settings use neutral surfaces, and native WPF fixes drafts, layout and calendar overlaps.

- Verified outputs: Debug and signed Release APKs at `app/build/outputs/apk/{debug,release}/PaykiTodo-1.16.21-*.apk`; Windows `desktop/dist/PaykiTodo.exe` / `1.16.21.0`.
- Passed: 186 Android unit tests, Lint, 23 emulator instrumentation tests, 18 Windows tests, 12 server tests and LAN browser regression. See [VALIDATION_1.16.21.md](VALIDATION_1.16.21.md) for actual paths, hashes and coverage.
- Next acceptance boundary: physical-device OEM long-idle/lock-screen behavior. No multi-day idle, physical vibration or vendor battery-policy evidence is claimed. Windows lunar/OCR/multi-slot creation remains incomplete; existing special fields are preserved. Desktop reminders require the program to run.
- No push, tag, publication or production server changes.

### User-requested actual operation audit

- Reused the user's Pixel_8 / emulator-5554. Actually tapped notification/account/settings/checklist/todo/history/calendar/planning/reminder controls. New formal login/account panels received real taps and keyboard input against a local server; actual Wi-Fi/data outages and request-time edits were tested.
- Re-ran 186 unit tests/Lint, original23 Android tests, 3 new Android acceptance tests, 18 Windows regressions and a 12-stage visible native OS pointer/keyboard flow. Visible Edge saved a titleless hierarchy into the actual phone LAN service, not mocked fetch routes.
- Scope, failures and remaining boundaries: [INTERACTIVE_VALIDATION_1.16.21.md](INTERACTIVE_VALIDATION_1.16.21.md). No fresh product APK/EXE; only AndroidTest rebuilt. Original emulator files restored with19/19 SHA-256 matches, network/temporary permissions restored and test server stopped.
- Remaining: OEM long-idle/lock-screen, timed redelivery, upgrade/time-change full E2E, Windows login/EXE entry full E2E and documented parity gaps. Android calendar modification save was not confirmed; creation/detail were. Do not describe these as passed.

## v1.16.20 Completed Work (2026-09-23)

User request resumed from session `01a05c72-896d-7831-afd4-0a2dc093ccec`:

1. Restore phone Daily Board agenda geometry from the supplied screenshot: left date strip, readable event rows, tomorrow below; preserve artwork.
2. Close all LAN Web editors and discard drafts when the phone disconnects or access expires.
3. Replace raw fetch errors with useful Chinese connection guidance.
4. Replace the Windows HTML workspace with a native desktop UI, retaining independent local storage and account sync.

That completed round used source version `1.16.20 / 385`; the public GitHub Release remains `v1.15.18`.

- Phone agenda implementation matches the supplied reference geometry and the dedicated `DailyBoardReferenceTest` passed on `emulator-5554` / `Pixel_8`.
- LAN regression passed all 8 modal types, canceled promises, stale response, disconnect, and reconnect errors; network failures show Chinese guidance.
- Windows WPF workspace, local-first account lifecycle, reminder behavior, special-field preservation, AI-key stripping, native editor interaction, and real local-server integration all passed 12 tests.
- Android Debug/AndroidTest/Release assembly, 185 unit tests, Lint, JavaScript syntax, LAN browser regression, Windows tests, and self-contained publish passed.
- This round is complete locally. No push, tag, or GitHub Release was performed; existing public release remains `v1.15.18`.

### v1.16.20 Known Limits

- Windows does not yet reproduce every Android-only surface such as OEM lock-screen behavior, lunar recurrence editing/generation, OCR/image capture, all advanced settings, or multi-slot editor details.
- Windows reminders require the client to remain running; physical-device Android reminder and widget behavior still needs OEM-specific testing.
- AI calls were tested with controlled parsing and local test servers, not with a paid provider account.

## Previous Active Focus

The `v1.16.19 / versionCode 384` round closes the remaining cross-client history restoration gap from the `v1.16.18` quick-checklist validation. LAN Web and the independent Windows client now expose restoration for ordinary historical todos and quick-checklist subtrees. This remains a local validation round and does not publish a GitHub Release.

## v1.16.19 Checklist

- [x] Administrator timestamps are emitted as epoch values and formatted in the visitor's browser timezone.
- [x] The quick checklist parser handles a title, arbitrary dash-depth nesting, visible `--#` notes, validation, and Enter prefix continuation.
- [x] Daily Board and Todo expose the quick checklist entry and real-time preview on Android.
- [x] Confirmed input creates real hierarchical TodoItems, never a title row or note-only row.
- [x] Parent completion excludes canceled children, does not revive canceled ancestors, and updates ancestors recursively.
- [x] Complete, cancel, delete, history, backup, cloud snapshot, LAN Web, Windows, and Widget paths carry the checklist fields.
- [x] Room `30 -> 31` migration and exported schema are present.
- [x] Parsing diagnostics locate the source line on Android, LAN Web, and Windows.
- [x] Widget rows expose checklist completion state and progress without replaying old completed items.
- [x] Windows complete/cancel operations preserve terminal states and fresh local data creates a valid default group.
- [x] LAN Web and Windows expose historical todo restore/delete actions, including quick-checklist subtree restoration.
- [x] Turnstile token polling is lifecycle-bound and cleaned up when its WebView is released.
- [x] Focused parser/repository/migration/backup/server/Windows tests and emulator instrumentation pass.
- [x] Documentation, final build verification, sensitive-data scan, and focused commit are complete.

## v1.16.19 Verification Evidence

1. `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest`, `node --check`, `dotnet test`, `dotnet build`, and the 12-test server suite passed during implementation.
2. Room migration metadata covers database `31`, and backup compatibility tests cover older archives without checklist fields.
3. `emulator-5554` / `Pixel_8` passed all 18 instrumentation tests, including a real Room `30 -> 31` migration.
4. Final verification after the `1.16.19 / 384` metadata update: 185 Android unit tests, 18 emulator instrumentation tests, Android Lint, Debug/AndroidTest/Release assembly, 12 server tests, JavaScript syntax, 5 Windows tests, and Windows self-contained publish all passed. Release APK v2 signature verification passed with the expected RSA-2048 signer.
5. The current Debug APK was installed on `emulator-5554` / Android 14, package `com.paykitodo.app` reported `versionName=1.16.19` and `versionCode=384`, and the app launched successfully. Physical-device OEM reminder/widget behavior remains outside this round.

## Completed `v1.16.14` Scope

1. Allowed Cloudflare's required `about:blank` and `about:srcdoc` internal documents in the Android Turnstile WebView.
2. Added restrictive CSP entries for the Turnstile iframe, internal documents, and the Cloudflare Insights script injected by the private proxy.
3. Suppressed duplicate delivery of one Turnstile token and kept AndroidView callbacks connected to the current Compose state after recomposition.
4. Raised Android and Windows application metadata to `1.16.14 / versionCode 379`.

## Validation For `v1.16.14`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed.
2. `:app:testDebugUnitTest` and `:app:lintDebug` passed; Debug and Release APKs were assembled.
3. Azure service restarted successfully; public `/healthz` returned `200`, and the public challenge CSP contains the required `about:blank` / `about:srcdoc` entries.
4. The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the Turnstile widget. A final successful token assertion remains pending on a physical device because Cloudflare challenges are intermittent in this emulator; the observed 401 is the documented Private Access Token probe.

## Completed `v1.16.13` Scope

1. Added the existing Turnstile production hostname to the widget without replacing the owner's widget or removing its local development hosts.
2. Retrieved and validated the existing Secret without displaying or committing it, then stored it in Azure `/etc/paykitodo/cloud.env`.
3. Added server-side hostname allowlist validation in addition to `success=true` and `action=account_auth`.
4. Added an Android WebView response-field fallback for WebViews that render Cloudflare success but omit the parent callback.
5. Raised Android and Windows application metadata to `1.16.13 / versionCode 378`.

## Validation For `v1.16.13`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed.
2. `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
3. Public `/healthz` returned `200`; `/v2/auth/challenge` returned a real Turnstile widget; a dummy registration token was rejected with `403 human_verification_failed`.
4. The final rebuilt APK was not installed on the emulator because `emulator-5554` disconnected; the earlier emulator session displayed Cloudflare `Success!` before this callback fallback was added.

## Completed `v1.16.12` Scope

1. Normal reminder acknowledgement, completion, notification retention, and stale-alarm rejection no longer erase the dispatch ledger for the current trigger.
2. Reminder ledger reset remains limited to deliberate todo or event rescheduling, schedule rebuilding, item replacement, or deletion across both Android and LAN Desktop Web writes.
3. Android uses the notification full-screen intent as the only primary reminder surface and enables direct Activity / accessibility fallback only when the system mechanism is unavailable.
4. AI reports remain AI-first with a visible local-template fallback; provider model discovery and ordered fallback remain available.
5. No periodic synthetic prompt traffic was added because it would disguise automated behavior and attempt to evade provider safeguards.
6. Raised Android and Windows application metadata to `1.16.12 / versionCode 377`.

## Validation For `v1.16.12`

1. `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
2. `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed; Lint reported 0 errors and 125 non-blocking warnings.
3. `emulator-5554` / `Pixel_8` installed and launched `PaykiTodo-1.16.12-debug.apk`; metadata reported `versionName=1.16.12` and `versionCode=377`, and the account page rendered without a fatal crash.
4. Debug SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
5. Release SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
6. The remaining physical-device checks are OEM full-screen reminder delivery, lock-screen behavior, notification shade rendering, and battery policy.
7. The signed release build is local-only in this round; no GitHub Release or push was performed.
8. Windows publish completed; the known `WindowsBase` 4.0/5.0 WebView2/WPF warning is non-fatal.

## Completed `v1.16.10` Scope

1. Reminder polling now passes the exact trigger timestamp into `ReminderForegroundService`, so receiver and polling paths share one dispatch identity.
2. New reminder scheduling no longer creates the old `+2s` backup alarm; application startup cancels legacy backup PendingIntents.
3. Planning Desk mappings, nodes, note creation, status refresh, and snapshot import notify the widget and remote-sync coordinator after direct Room writes.
4. Turnstile WebView allows the Cloudflare challenge host and third-party cookies and blocks HTTP/render-process failures.
5. AI reports remain AI-first with an explicit local-template fallback; no synthetic periodic anti-detection traffic was added.
6. Raised Android and Windows application metadata to `1.16.10 / versionCode 375`.

## Validation For `v1.16.10`

1. Server integration tests and Android unit tests passed before the version bump.
2. The deployed cloud source now exposes `/healthz` and `/v2/auth/challenge`; the production environment still needs the owner's Turnstile Site Key and Secret Key before a real widget can appear.
3. Run Android Lint, Debug/Release assembly, and emulator smoke tests after the final metadata update.

## Completed `v1.16.9` Scope

1. Android login, registration, and password recovery can obtain a Turnstile token from the configured challenge page.
2. The cloud service verifies the token against Cloudflare before processing authentication requests; missing configuration keeps existing deployments compatible.
3. Partial Turnstile configuration fails closed instead of silently accepting unverified authentication requests.
4. Added safe public/private configuration templates and kept the Turnstile Secret Key out of Android and Git.
5. Raised Android and Windows application metadata to `1.16.9 / versionCode 374`.

## Validation For `v1.16.9`

1. Passed the 7-test server integration suite, Android unit tests, Android Lint, and Debug/Release APK assembly.
2. On `emulator-5554` / `Pixel_8`, installed and launched the Debug APK; package metadata reported `1.16.9 / versionCode 374`, and the account UI was visible without a fatal crash.
3. The emulator did not have an authenticated test session, so a real scheduled reminder could not be triggered through the app. The service-start fallback remains covered by the production catch path and requires authenticated controlled runtime testing for end-to-end full-screen evidence.

## Completed `v1.16.8` Scope

1. Empty authenticated devices now initialize local sync revision at `0`, so an existing cloud snapshot is downloaded.
2. Devices with local data continue to upload to an empty cloud account or enter explicit conflict protection when the cloud already has data.
3. Added regression tests for empty-device download, local-data conflict, and local-data upload initialization.
4. Raised Android and Windows application metadata to `1.16.8 / versionCode 373`.

## Completed `v1.16.7` Scope

1. Removed all migration-package actions from the unauthenticated surface; export and import remain available after login in `设置 -> 数据与备份`.
2. Updated the Debug-to-Release migration instructions so both operations require an authenticated account session.
3. Raised Android and Windows application metadata to `1.16.7 / versionCode 372`.

## Validation For `v1.16.7`

1. Passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verified on `emulator-5554` that the unauthenticated screen contains no migration explanation, export action, or import action; authenticated `设置 -> 数据与备份` remains the migration entry point.
3. Passed `python -m unittest server.test_paykitodo_cloud_server -v` with all 5 integration tests.
4. Passed PowerShell syntax checks for the ignored local release-build and administrator-credential helpers without rotating production credentials.
5. Passed `git diff --check` and verified `user/`, signing files, environment files, and server database patterns are ignored.

## Completed `v1.16.5` Scope

1. Caught system-level failures when Android rejects a background foreground-service start and published a normal reminder notification fallback instead of crashing the process.
2. Preserved full-screen reminder intent behavior during the notification fallback and recorded the rejection/fallback result in the reminder chain logs.
3. Allowed a newly created calendar event that has already started to keep historical reminder offsets; the alarm scheduler continues to ignore past trigger times.
4. Added an explicit editor notice for retained historical reminders and kept future calendar events subject to future reminder validation.
5. Added the existing dashboard artwork and launcher icon to the account login/register/recovery screen with a scrollable narrow-screen layout.
6. Added `-Rotate` to the ignored administrator credential helper; the default mode reads credentials and rotation mode writes a new password and restarts `paykitodo-cloud.service`.
7. Raised Android and Windows application metadata to `1.16.5 / versionCode 370`.

## Validation For `v1.16.5`

1. Run `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verify the login page on the Android emulator in light and dark themes, including migration buttons and scroll behavior.
3. Verify the reminder fallback through a controlled Android 12+ background-start denial or an injected unit-level start failure; physical OEM lock-screen and battery policies still require device testing.
4. Verify creating an already-started calendar event with a past reminder saves successfully and does not schedule the missed trigger.
5. Parse the administrator helper with PowerShell without running `-Rotate`; do not rotate the production credential during build verification.

## Completed `v1.16.4` Scope

1. Moved Android login and registration network work to `Dispatchers.IO` so the authentication action does not block the UI thread.
2. Refreshed the ViewModel authentication signal immediately after successful login or registration, so the app opens the daily board without requiring a restart or a polling tick.
3. Installed the signed Release build on the Android emulator and verified login, relaunch persistence, daily board, drawer, todos, calendar, Planning Desk, settings, and appearance navigation.
4. Added `docs/current/ANDROID_RELEASE_UI_AUDIT_1.16.3.md` with UI-tree evidence scope and residual physical-device risks.
5. Restored the final build configuration to the HTTPS cloud endpoint; the local cleartext endpoint used for isolated emulator testing is not part of the final build.

## Completed `v1.16.2` Scope

1. Added migration-package export/import actions to the unauthenticated login surface, so a user can preserve local data before uninstalling a Debug-signed build and restore it after installing a Release-signed build.
2. Stopped the phone LAN service explicitly on account logout and blocked secondary entry points, reminders, and widgets from exposing data without a valid session.
3. Made reminder acknowledgement/snooze remove stale notification and foreground-service artifacts, and kept long-title reminder actions readable on narrow screens.
4. Changed automatic backup output to one latest archive updated on data changes, with cleanup of legacy timestamped archives.
5. Raised Android and Windows application metadata to `1.16.2 / versionCode 367`.

## Completed `v1.16.1` Scope

1. Unauthenticated `/admin` requests render a PaykiTodo-owned login page instead of returning a `WWW-Authenticate` challenge.
2. Added cookie-based administrator sessions with logout and preserved CSRF checks for state-changing actions.
3. Kept valid Basic Auth as a non-challenging compatibility path for scripts and tests.
4. Raised Android and Windows application metadata to `1.16.1 / versionCode 366`.

## Completed `v1.16.0` Scope

1. Added the account registration, login, token refresh, device, versioned encrypted snapshot, conflict, and administrator APIs on loopback port `18766`.
2. Added Android account UI and encrypted local account credentials while preserving Room as the local authority and `18765` as the separate LAN browser path.
3. Added a writable, offline-capable Windows local client with DPAPI-protected state and core todo/event/planning operations.
4. Added integration tests and public deployment/usage templates without private domain or server credentials.
5. Deployed the cloud API and administrator page behind the private HTTPS reverse proxy; the origin stays loopback-only on `18766`, with the existing phone LAN `18765` path unchanged.
6. Verified public health, account registration/authentication, administrator access, administrator CSRF protection, account disable/enable, and device revocation.

## Completed `v1.15.22` Scope

1. Split desktop todo and event creation buttons and made their visibility tab-specific.
2. Reflowed the desktop header so navigation stays left while creation and connection controls stay right without narrow-window overlap.
3. Matched the Desktop Web current-time line to the phone calendar's full-width red line and current-day marker convention.
4. Added minute-aligned indicator refresh, fixed mini-calendar selection geometry, centered week headers, and one-second selected-column feedback.

## Completed `v1.15.21` Scope

1. Added signed Release and Debug build tooling using the same source with separate signing/build types.
2. Added `.paykibak` ZIP migration archives with checksums, legacy JSON import compatibility, and secret exclusion.
3. Added local-first AES-GCM remote snapshot upload scheduling and the server-side ciphertext relay.
4. Added a self-contained Windows WebView2 shell for the existing LAN Desktop Web console.
5. Fixed Android API 26 compatibility in the remote-sync URL path and verified Android/desktop builds.
6. Added `我知道了` to every todo full-screen reminder, including same-day due todos.
7. Added remote snapshot summary inspection, explicit replacement confirmation, and pre-restore safety backup.
8. Added Windows server snapshot mode with local decryption and read-only board/todo/calendar rendering.
9. Unified the user-facing reminder control: one `提醒` checkbox now means full-screen plus notification-bar delivery; legacy channel values are migration-only.
10. Made todo reminder actions responsive: event reminders show only `我知道了`, intermediate todo reminders show completion/acknowledgement, and the final todo reminder additionally shows fixed ten-minute snooze.
11. Reflowed the accessibility overlay actions into a primary row and an optional second row, with single-line button labels and compact padding for narrow phones.
12. Revalidated retained-notification clicks so completed, canceled, or missing items only clear stale notifications instead of opening previews.

## Latest Public Release Baseline

1. `v1.15.18` remains the latest GitHub Release and public APK.
2. The `1.16.0` source and local APKs are not published until the user explicitly requests a release.

## Next Validation

1. Install `1.16.4` Release on a physical device and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify the single reminder switch and the event/intermediate/final todo action layouts on a physical phone.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Test Android + Windows login, offline edits, upload/pull, conflict blocking, logout, and device revocation against a disposable account on the deployed service.
7. Add field-level merge or explicit conflict-resolution UI in a later round; the current encrypted snapshot contract intentionally stops before automatic merges.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
