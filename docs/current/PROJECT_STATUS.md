# Project Status

## Current Baseline

- Android source and verified Debug/signed Release: `1.17.7 / versionCode 399`; Windows source/MSI/portable: `1.17.7.0`; Room remains `33`. Starting commit: `63fc037`.
- Both clients now retain acknowledged personal snapshots and conservatively merge independent edits. Completion/cancellation can converge with snooze/passive missed state; ambiguous concurrent edits and legacy divergence without a baseline remain protected. Diagnostic logs and delayed callbacks without content changes no longer create false conflicts. See [1.17.7 validation](VALIDATION_1.17.7.md).
- Passed: 237 Android units; Lint 0 errors/134 warnings/30 informational; 9 distinct account device tests after targeted retries plus 1 Android-to-already-open-Windows live test. Windows: 71 regressions passed, 2 environment-specific cases skipped, plus one 14-stage isolated UIA flow. MSI real 1.17.5 upgrade, repair, installed payload/app, uninstall and normal-user-data preservation passed. The live refresh measured 2083 ms once locally, not a network latency guarantee.
- User-connected Release phone upgraded from 1.17.6 using the same certificate. Backed-up divergent copies were reconciled, exported/rechecked and uploaded through the app; subsequent ordinary automatic upload and restart showed healthy sync. Original and final personal data remain private in Backups/Scratch. Another physical computer's UI was not inspected.
- Emulator user 0 retains 646/646 matching file hashes; user 10 retains 148/148. Dedicated user 11 changes are backed up. Test apps/fixture/Windows host stopped, test MSI uninstalled; headless Pixel_8 remains on user 10 home.
- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`; branch `main`; package `com.paykitodo.app`; Android 8.0+/API 26+, compile/target API 34; MIT license. Local focused commit only; no push, public release or server-code deployment.

## Published And Local Builds

- Published GitHub Release remains [v1.15.18](https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18). No public 1.17.7 tag/download is claimed.
- Android signed Release: `app/build/outputs/apk/release/PaykiTodo-1.17.7-release.apk`, 21,038,442 bytes; SHA-256 `1FC3A9B8616894B0442C3B654055F281FB40FF11B1DA606901AED9E744229A5F`.
- Android Debug: `app/build/outputs/apk/debug/PaykiTodo-1.17.7-debug.apk`, 41,386,223 bytes; SHA-256 `2748DB13B07A7B6C57A3049B475465B179ECB841CBB6E11DF9BC52101262A1EC`.
- Windows MSI: `desktop/installer-output/PaykiTodo-1.17.7-windows-x64.msi`, 80,163,924 bytes; SHA-256 `2A50BC35B576645779259EB77B2C3DEE7839148AD7B20180A7D8BB9103E2BD0C`.
- Windows portable entry: `desktop/dist/PaykiTodo.exe` (full directory required); SHA-256 `4E77BD494ED8CE6396C575DF735FE414B0DBBC105DA452A56A506D6918EC00EC`.
- Verified copies, installation guide and hashes: `G:\Workspace\Deliverables\PaykiTodo\1.17.7`. Older deliverables remain intact. This sync fix adds no new visual preview.

## Product Positioning

PaykiTodo is a local-first Android assistant for todos, DDLs, schedules, countdowns, planning notes, and strong reminders. It is developed through maintainer-led, AI-assisted vibe coding: the maintainer owns product decisions and acceptance, while AI tools assist implementation, testing, debugging, and documentation.

## Current Feature Baseline

- Daily board for active todos, remaining schedules, tomorrow's plan, announcements, and countdown targets.
- Todo management with optional DDL, multiple groups, recurrence, reminder offsets, archive/cancel history, and countdown marking.
- Calendar with day/week/month/list views, recurrence, locations, colors, reminders, check-in tracking, and multi-time-slot creation.
- One frequent reminder switch across phone and Desktop Web; disabled means no scheduling, enabled means full-screen plus notification-bar delivery.
- Reminder delivery falls back to a detailed notification when Android rejects a background foreground-service start, preventing `ForegroundServiceStartNotAllowedException` from crashing the app.
- Already-started calendar events may retain historical reminder settings; missed trigger times are not backfilled while future recurring instances remain schedulable.
- Planning Desk memo workflow with local-rule parsing, optional OpenAI-compatible AI recognition, preview, correction, and confirmed import.
- Local LAN Desktop Web console for board, todo, calendar, Planning Desk, AI reports, and Excel schedule import.
- Optional account-based encrypted cloud sync; the Room database remains authoritative and the server stores only encrypted migration-package bytes. Account registration/login, refresh tokens, device revocation, versioned uploads, conflict blocking, and a separate administrator user/device overview are implemented.
- Remote snapshot inspection shows export time and record counts before any restore; restore requires explicit confirmation and creates a local safety backup first.
- Windows self-contained desktop client supports an independent encrypted local data copy, account login/registration, offline todo/event/planning edits, and versioned cloud synchronization. Its phone LAN option remains a clearly separate compatibility mode using the phone's `18765`.
- Android daily-board and countdown widgets.
- Quick checklist groups with arbitrary hierarchy, visible `--#` notes, real TodoItems, parent completion, folding, backup and cross-client snapshot support.
- Local Room storage, `.paykibak` migration archive / legacy JSON restore, diagnostics, crash logs, and in-app Wiki.
- Desktop Web calendar now uses the same current-time line convention as the phone calendar, separates today's date from the selected date, and flashes the selected week column briefly.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

## v1.16.26 Validation Scope (not published)

- Windows循环编辑/取消/删除增加当前、之后、全部作用域，修正跨午夜锚点、月末/闰年补齐，并支持每月第几个星期及农历重复。全天边界/提醒、多时段统一提交与日历颜色联动在跨端迁移包往返中验证。
- 193 Android单测、13个设备用例、38 Windows回归和1条14阶段UIA流程通过。MSI安装、模拟旧版升级、路径记忆、修复、安装后EXE、卸载及正式用户资料不变通过；具体条件和初次失败见验收记录。
- 最终Debug安装到既有Pixel_8，正常进入登录页、无启动崩溃；人机验证加载失败，本轮未再次登录真实账号。原639个文件全部保留，613个字节一致，业务数据库逐表一致，另3个运行文件保留；应用/测试包停止，模拟器运行。

## v1.16.25 Validation Scope (not published)

- Android `1.16.25 / versionCode 390`，Windows `1.16.25.0`，Room `32`，开始基线 `6f3800f`。
- 193 Android 单测；Lint 0 errors / 131 warnings；13个不同模拟器用例经针对性复验通过；26 Windows 回归（含实际 EXE）及1条14阶段隔离 UIA 流程通过。
- Windows 生成迁移包→Android 独立 Room 数据库导入、修改→导出→Windows 读取，核对日历归属、名称与已完成纪念状态。手机及桌面新数据字段进入现有整份快照链路。
- 新版 Debug 安装在既有 `emulator-5554 / Pixel_8`（Android14），冷启动及登录页渲染正常；当次人机验证显示加载失败，未进行真实账号登录或绕过门禁，应用与测试包最后均停止，模拟器保持运行。

See [VALIDATION_1.16.25.md](VALIDATION_1.16.25.md) for reference inspection, new controls, scroll-fix regression, data preservation and boundaries.

## v1.16.24 Validation Scope (not published)

- Android drawer/body and Windows heading/navigation/editor typography use bundled Sarasa; greeting artwork/font remains, Windows greeting and calendar accents use blue. Native calendar adds mini-month/week/today markers, fixed date headers, timezone and continuous current-time line. Search, account order and integrated checklist icon are updated.
- Native completion/restoration now responds to accessibility Toggle state changes and persists across restart/sync. Android reminder background/system bars/action text and dark calendar button/focus colors have corrected contrast.
- Passed 190 Android units, Lint (0 errors / 131 warnings), Debug/signed Release builds and v2 signature; 22 Windows regressions plus one 14-stage isolated UIA flow, self-contained publish and packaged startup. Real dedicated-account clients completed phone edits, desktop completion propagation and a timed reminder on both platforms; human verification was performed by the user.
- Original Pixel_8 private data restored with 557/557 file hashes matching; 78 additional runtime files retained, test data separately backed up. The emulator remains running with app/test packages stopped; the isolated Windows test client was closed. See [VALIDATION_1.16.24.md](VALIDATION_1.16.24.md) for artifacts, initial failures, screenshots and scope.
- No push, tag, release or production deployment changes. OEM/physical-input/all-platform parity are not claimed; Android full-screen evidence here is from opening the delivered notification.

## v1.16.23 Validation Scope (not published)

- Automatic sync drains edits made during uploads, detects missed dirty flags with content fingerprints, and keeps divergent legacy copies blocked until explicit confirmation. Running clients check cloud versions about every 15 seconds; stopped/offline devices cannot promise that latency.
- Android recovers previously registered, undelivered reminders within 10 minutes without clearing dispatch history. Historical todo/event/reminder input is accepted without replay; Windows delivers only previously registered triggers.
- Bundled Sarasa UI SC across Android/WPF/LAN/Wiki, named desktop group selectors, compact task layout and removal of duplicate account navigation. Android launcher name is “派期待办”; task shortcuts and deep links retain the account gate.
- Passed: 190 Android unit tests; 32 distinct device tests after targeted retries; Lint 0 errors / 131 warnings; 22 Windows regressions plus one 14-stage isolated UIA flow; LAN browser checks; Debug/signed Release/Windows builds and Release v2 signature verification. Initial failures and precise scope are retained in [VALIDATION_1.16.23.md](VALIDATION_1.16.23.md).
- Existing Pixel_8 data restored; read-only recheck matches all 531 original files, with 26 extra runtime files retained. Final artifact hashes rechecked; resume work only completed documentation and verification, without rebuilding products or changing production.
- No push, tag or release. OEM behavior, vendor voice recognition, native Windows full login E2E and existing parity gaps remain explicit limits.

## v1.16.22 Validation Scope (not published)

- Native WPF typography, original high-resolution icons, neutral dark controls, navigation, dialogs and responsive board improved. Fixed date/time text clipping and pinned Planning Desk actions; expired-session/offline and explicit-logout rules remain unchanged.
- Default Windows test launcher creates a separate desktop before starting .NET/COM; tests reject the input desktop. UIA mode does not use global input or clipboard. Physical-input mode remains separately opt-in.
- Passed: 19 Windows regressions including packaged EXE smoke, plus 1 UIA workflow test with 14 stages; 186 Android unit tests, Lint (0 errors / 128 warnings), Debug/signed Release builds, Release v2 signature verification and Windows self-contained publish.
- Existing `emulator-5554` / `Pixel_8` upgraded in place after private-data backup; verified installed 1.16.22/387 and account login UI. Android runtime source was not changed this round; earlier instrumentation evidence is historical, not a newly repeated full phone suite.
- No push, tag, release or production-server changes. Full platform parity, OEM long-idle/lock-screen, real-provider AI and native login/human-verification E2E remain unproven. See VALIDATION_1.16.22.md.

## v1.16.21 Validation Scope (not published)

- Expiry retains local account access and reminders, posts one account notification and routes to reauthentication; explicit logout locks access. Remembered credentials are separate encrypted stores excluded from snapshots.
- Cloud synchronization uses persistent network jobs and callbacks independently of LAN. Concurrent writes remain dirty; cloud inspection and confirmed conflict choices validate account/revision/local changes and save both sides first.
- Titleless checklists work across Android/LAN/WPF, including actual save and Android archive restore. A stale empty-title filter in the archive reader was found by the new regression and fixed.
- Phone settings and native WPF use neutral dark surfaces; WPF drafts survive navigation/status refresh, overlapping events use lanes, toolbars wrap and weekly recurrence uses weekday selectors.
- Passed: 186 Android unit tests; Android Lint (0 errors, 128 warnings); Debug/AndroidTest/signed Release builds; 23 Pixel_8 instrumentation tests; 18 Windows tests; 12 server tests; LAN browser regression and JavaScript syntax; self-contained Windows publish and Release APK v2 signature verification.
- A dedicated run disabled emulator Wi-Fi and mobile data, expired the synthetic account, pressed Home, scheduled a real AlarmManager reminder and observed its notification/dispatch claim. Fixture account preferences and synthetic tasks were restored afterward; network was re-enabled.
- Validation details and remaining boundaries: [VALIDATION_1.16.21.md](VALIDATION_1.16.21.md). No push, tag, release or production-server change.
- Follow-up: original23 Android and 18 Windows regressions rerun; new3 Android local-server/login/conflict tests and1 visible12-stage WPF OS-input test pass. Actual phone and visible LAN steps are listed separately from test counts. Full platform parity, OEM behavior and every UI flow are not claimed.

## v1.16.20 Validation Scope (not published)

- Phone Daily Board restores the supplied left-date/right-agenda/tomorrow geometry while preserving the existing artwork and font. The dedicated `DailyBoardReferenceTest` passed on `emulator-5554` / `Pixel_8`.
- LAN Desktop Web closes all editor, confirmation, and form modals on phone disconnect or access expiry, cancels draft promises, rejects stale responses, and shows Chinese connection guidance.
- Windows now starts the native WPF workspace. WebView2 is used only for Turnstile account verification; the independent local-first cloud client and phone LAN browser remain separate paths.
- Windows cloud snapshots and exported migration packages omit AI API keys. Native edits preserve hidden/check-in/reminder fields and unknown special recurrence types.
- Validation passed: 185 Android unit tests, Android Lint, Debug/AndroidTest/Release assembly, JavaScript syntax, LAN browser regression, 12 Windows tests, and self-contained Windows publish.
- Debug APK SHA-256: `2119C69EAC9C6062A1581D2699BB29E916FCB64B47B49D75103D212E56E8E5B0`.
- Signed Release APK SHA-256: `FA47E46D44D82375C4EC78559856667E7089E925704656CA814AA53C881D525D`.
- Windows self-contained EXE SHA-256: `10B5C5396E582F366921AE6DD84530C0226121B9324D6E5BA987B06576F9A1DA`.
- Known limits: Android OEM reminder/widget behavior, Windows lunar/OCR/multi-slot parity, and paid-provider AI integration remain outside this local round. No push, tag, or GitHub Release was performed.

## v1.16.19 Validation Scope (not published)

- Administrator timestamps are serialized as epoch values and formatted in the visitor's browser timezone.
- Quick checklist parsing, preview, Enter continuation, validation, and hierarchical task creation work on Android.
- LAN Desktop Web and the Windows client expose matching create/fold/complete/cancel/delete flows, and all three clients expose history restoration for ordinary and quick-checklist todos. LAN Web keeps quick-checklist history in its dedicated section instead of duplicating those nodes in ordinary history.
- Parent completion excludes canceled children, does not revive canceled ancestors, and recurses upward. Canceled tasks enter history; deleted tasks do not.
- Room `30 -> 31` adds checklist group, parent, depth, order, note, and fold fields while preserving existing data.
- Error diagnostics select the matching source line on Android, LAN Web, and Windows. Widget rows show checklist completion state and progress, while Windows state transitions preserve canceled/completed terminal states.
- Android Turnstile token polling is lifecycle-bound, times out explicitly, suppresses duplicate delivery, and is stopped when the WebView is released.
- Final verification passed: 185 Android unit tests, 18 emulator instrumentation tests on `emulator-5554` / `Pixel_8`, Android Lint, Debug/AndroidTest/Release assembly, 12 server tests, JavaScript syntax, 5 Windows tests, and Windows self-contained publish. Release APK v2 signature verification passed with one RSA-2048 signer.
- The Debug APK was installed and launched on `emulator-5554` / Android 14; package `com.paykitodo.app` reported `versionName=1.16.19` and `versionCode=384`.
- Debug APK SHA-256: `F80C5B63E07EF7B7AA53B1620DEAA2D6190C236EAC7A18598C33D07DB61ED722`
- Signed Release APK SHA-256: `3ADB690F88C80385669156AFC7939B4A9738C96D29CA7B59ACAF5BFB45CBF295`
- Windows self-contained EXE SHA-256: `19BFC2AB4370B887112D45E252C4AFE9EF23997E1D250FC9D39492C90E289279`

## v1.16.15 Validation Scope (not published)

- Drawer account identity, immutable server user ID, nickname/password updates and signed-in/historical device management.
- Password changes atomically replace password hashes and re-encrypted cloud snapshots with revision checks, then revoke sessions without deleting local data.
- Calendar vertical month paging, continuous horizontal week paging, date-correct Today navigation, today agenda scrolling/highlight, compact agenda text and content-height accent bars.
- Removed calendar batch/template menus, moved history under Todo, restored Todo artwork, added a circular create button with an 800ms batch action and larger group choices.
- Planning document creation/rename/swipe archive moved into the document list; help moved to the top bar; editor guidance card removed.
- AI provider titles wrap at full width without numbering; sound preferences are merged and compacted.
- Azure account APIs are deployed; service health check passed. Public proxy accepts the encoded snapshot size needed by password rotation. Deployment credentials and rollback files remain private.
- Verification complete: 11 server tests passed; Android unit tests, Lint, Debug/Release assembly, and all 9 emulator interaction checks passed. Physical-device OEM reminder/widget checks remain.
- Emulator evidence: `emulator-5554` / `Pixel_8` installed the final Debug and instrumentation APKs; `MainActivity` launched with `versionName=1.16.15` and `versionCode=380`.
- APK evidence: Debug SHA-256 `054169AA284863F486D7EE7BAFC9D169CA63A9449985BC62BA78C45A8A3009B4`; signed Release SHA-256 `1FEF71C5367109E759E20F5FDCFD64CB4D05498CF268D82EC850C31A3DE79390`; `apksigner verify` passed the Release APK with one RSA-2048 signer.

## v1.16.14 Validation Scope (not published)

- Android WebView now allows the `about:blank` and `about:srcdoc` internal documents required by Cloudflare's mobile Turnstile flow.
- Duplicate callbacks for one Turnstile token are suppressed before they reach the Compose login state, and callbacks held by AndroidView always target the current composition.
- CSP keeps the challenge and injected Cloudflare Insights resources available while retaining a restrictive default policy.
- Raised Android and Windows application metadata to `1.16.14 / versionCode 379`.

## v1.16.13 Validation Scope (not published)

- Turnstile account authentication now requires a successful Cloudflare Siteverify response with the fixed `account_auth` action and an allowed production hostname.
- Added the production hostname allowlist to the server environment template; the real allowlist and Secret Key remain outside Git.
- Added an Android WebView fallback observer for the Turnstile response field so a successful widget can update the Compose authentication state even when a WebView omits the parent callback.
- Raised Android and Windows application metadata to `1.16.13 / versionCode 378`.

## v1.16.14 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed, including valid, wrong-hostname, incomplete-allowlist, and CSP internal-document assertions.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- Azure service restarted successfully; public `/healthz` returned `200`, and `/v2/auth/challenge` exposes the real widget without the Secret Key.
- The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the Turnstile widget. Cloudflare's documented 401 Private Access Token probe appeared; final token acceptance remains a physical-device check because the emulator challenge was intermittent.

## v1.16.13 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed, including valid, wrong-hostname, and incomplete-allowlist Turnstile responses.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- `:app:lintDebug`: passed with 0 errors after the final WebView change.
- Azure `/etc/paykitodo/cloud.env` contains the Site Key, Secret Key, and production hostname allowlist; values were only checked in redacted form.
- Public `/healthz` returned `200`; `/v2/auth/challenge` returned the real widget and did not expose the Secret Key.
- A public registration request with a dummy token returned `403 human_verification_failed`.
- The emulator previously displayed Cloudflare `Success!`; it disconnected before the rebuilt `1.16.13` APK could be installed for the final callback assertion.

## v1.16.12 Validation Scope (not published)

- Normal reminder acknowledgement, completion, notification retention, stale-alarm rejection, and service shutdown no longer clear the dispatch ledger for the current reminder time.
- A reminder dispatch is cleared only when its schedule is deliberately rebuilt or a todo/event is rescheduled through Android or LAN Desktop Web, so a past trigger cannot be redispatched by the polling fallback.
- Android uses the system notification full-screen intent as the only primary reminder surface; direct Activity and accessibility-overlay paths are reserved for devices where that system mechanism is unavailable.
- AI reports remain AI-first, model discovery remains available from provider settings, and no synthetic anti-detection traffic is generated.
- Raised Android and Windows application metadata to `1.16.12 / versionCode 377`.

## v1.16.12 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
- `:app:testDebugUnitTest`: passed.
- `:app:lintDebug`: passed with 0 errors and 125 non-blocking warnings.
- `:app:assembleDebug` and `:app:assembleRelease`: passed.
- ADB smoke test on `emulator-5554` / `Pixel_8`: installed and launched the Debug APK; package metadata reported `versionName=1.16.12` and `versionCode=377`; the account page rendered normally and showed the explicit “当前服务器未启用人机验证” state without a fatal crash.
- Debug APK SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
- Release APK SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
- Windows EXE SHA-256: `39BE39CA7C26E36413102AE14331E56EA822FDD8A89EB7200FB3E959302447DE`.
- Physical-device validation remains required for OEM lock-screen and full-screen reminder behavior.
- Windows publish still emits a non-fatal `WindowsBase` 4.0/5.0 WebView2/WPF assembly warning; the build completes successfully.

## v1.16.10 Validation Scope (not published)

- Reminder polling passes the concrete trigger timestamp to the foreground service, matching receiver-side dispatch deduplication.
- Removed newly scheduled `+2s` reminder backups and clean up legacy backup alarm PendingIntents at application startup.
- Planning Desk direct writes and snapshot imports notify the local widget and remote-sync coordinator through one repository callback.
- Turnstile WebView accepts the Cloudflare challenge host, third-party cookies, and reports HTTP/render-process failures as blocked states instead of allowing submission.
- Raised the application metadata to `1.16.10 / versionCode 375`.
- The public cloud environment was updated to the current server source. Its Turnstile keys are still absent, so the endpoint intentionally reports that protection is not configured until the owner supplies both keys.

## v1.16.10 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v` and `:app:testDebugUnitTest` passed before the version bump.
- Public `/healthz` returned `200` and `/v2/auth/challenge` returned the current server response after deployment; the response correctly reports missing Turnstile configuration and contains no widget until keys are configured.
- Debug/Release APK assembly and final ADB smoke test remain to be run after the version bump.

## v1.16.9 Validation Scope (not published)

- Added optional Cloudflare Turnstile to Android account login, registration, and password recovery.
- Added server-side Turnstile Siteverify validation and a no-store challenge page; the server secret remains environment-only.
- Turnstile configuration now fails closed when only the public Site Key or only the server Secret Key is present.
- Kept IP rate limiting and made the feature disabled-compatible when Turnstile is not configured.
- Raised the application metadata to `1.16.9 / versionCode 374`.

## v1.16.9 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 7 tests passed, including missing-token rejection, mocked Siteverify success, and incomplete-key fail-closed behavior.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- ADB smoke test: `emulator-5554` / `Pixel_8` installed `PaykiTodo-1.16.9-debug.apk`, reported versionCode `374`, opened `MainActivity`, and exposed the login/register/recovery UI without a fatal crash.
- The authenticated reminder trigger was not executed on the emulator because no test account/session was configured. Full-screen launch, notification permission, exact-alarm permission, lock-screen behavior, and OEM background policy still require a controlled authenticated test and physical-device verification.

## v1.16.8 Validation Scope (not published)

- Fixed first-login synchronization on an empty device: an existing cloud snapshot is now downloaded instead of being treated as already current.
- Kept local-data upload and explicit conflict protection unchanged for devices that already contain data.
- Raised the application metadata to `1.16.8 / versionCode 373`.

## v1.16.7 Validation Scope (not published)

- Removed all unauthenticated migration-package actions from the Android account page; export and import remain in authenticated `设置 -> 数据与备份`.
- Updated the Debug-to-Release migration instructions to require login before either operation.
- Raised the application metadata to `1.16.7 / versionCode 372`.

## v1.16.2 Development Scope (not published)

- Added migration-package export/import actions to the unauthenticated login surface so Debug-to-Release data transfer remains available before account login.
- Stopped the phone LAN service explicitly during account logout and blocked unauthenticated secondary entry points, reminder surfaces, and widgets from exposing local data.
- Made reminder dismissal remove stale notification/service artifacts and kept long-title reminder actions readable on narrow screens.
- Changed automatic backups to one latest file updated after data changes instead of accumulating timestamped files.

## v1.16.5 Validation Scope (not published)

- Added a notification fallback when Android rejects the background foreground-service start, preventing reminder-triggered process crashes while preserving notification details and the full-screen pending intent.
- Allowed reminders on already-started calendar events to remain saved without backfilling missed trigger times; future event reminders retain the future-time validation.
- Redesigned the account entry surface with the existing dashboard artwork and launcher icon, while keeping migration actions available before login.
- Added explicit `-Rotate` behavior to the ignored administrator credential helper; ordinary runs read the current credential, while rotation updates the server and restarts the cloud service.
- Raised the application metadata to `1.16.5 / versionCode 370`.

## v1.16.4 Validation Scope (not published)

- Moved account login/registration off the UI thread and refreshed the authenticated ViewModel state immediately after success.
- Completed signed Release emulator navigation and UI audit for the account gate, board, drawer, todos, calendar, Planning Desk, settings, and appearance selector.
- Recorded audit evidence and physical-device residual risks in `ANDROID_RELEASE_UI_AUDIT_1.16.3.md`.

## v1.16.1 Development Scope (not published)

- Replaced the browser-native administrator Basic Auth prompt with a PaykiTodo-owned HTML login page and cookie session flow.
- Preserved CSRF-protected administrator actions and script-compatible valid Basic Auth without issuing an authentication challenge for ordinary browser requests.

## v1.16.0 Development Scope (not published)

- Added an account-based cloud API with password hashing, short-lived access tokens, refresh-token rotation, device management, audit records, and an independently protected administrator page.
- Added Android cloud account login/registration, encrypted local token/key storage, local-first synchronization, automatic pull when clean, upload after local changes, and explicit conflict blocking.
- Added an independent Windows cloud client with DPAPI-protected local account/data files, offline editing, core todo/event/reminder/recurrence/countdown/planning fields, and the existing phone LAN compatibility mode.
- Added generic cloud server environment, systemd, Nginx, desktop usage, and integration-test materials without private hostnames, addresses, credentials, or certificates.

- Split the desktop create controls into independent todo and event actions with tab-specific visibility.
- Reordered the desktop header into left navigation and right-side creation/connection controls with responsive wrapping.
- Unified desktop calendar current-time rendering with the phone timeline and added minute-aligned refresh.
- Fixed mini-calendar selected-day geometry and added one-second selected-week-column feedback.

## v1.15.21 Development Scope (not published)

- Added a `.paykibak` ZIP migration archive containing business data, planning data, history, and ordinary settings while excluding AI API keys, remote-sync secrets, Desktop Sync credentials, and file-provider authorization state; provider labels, Base URLs, and model names are retained without keys.
- Import protects the current installation's local credentials and backup authorization, refreshes reminder scheduling and widgets, and remains compatible with legacy JSON backups.
- Added a local-first encrypted remote-sync coordinator: data changes schedule a delayed upload, enabled sync performs a one-minute fallback cycle, and disabled sync owns no background upload jobs.
- Added the Azure/Nginx-compatible ciphertext relay service and deployment materials without uploading user data or secrets.
- Added a Windows `win-x64` self-contained shell around the existing LAN Desktop Web console and a repeatable build script.
- Upgraded the desktop WebView2 package to `1.0.4191.47`.
- Fixed Android API 33-only URL encoding in the new sync path so the API 26 minimum remains lint-clean.
- Added `我知道了` to every todo full-screen reminder, including todos due today; it only acknowledges the current reminder and does not complete, cancel, or move the task.
- Added remote snapshot inspection with local/remote counts and export time, explicit replacement confirmation, and automatic pre-restore local backup while preserving local credentials.
- Added Windows server-snapshot read-only mode with local PBKDF2-SHA256/AES-GCM decryption and `.paykibak` parsing; the server secret is not persisted by the desktop client.
- Unified the user-facing reminder control into one `提醒` checkbox: enabled means full-screen plus notification-bar delivery, disabled means no reminder scheduling.
- Reflowed full-screen and accessibility reminder actions for narrow phones: events show only `我知道了`, intermediate todo reminders show completion/acknowledgement, and only the final todo reminder offers fixed ten-minute snooze.
- Retained notification clicks now re-check the item state and clear stale notifications without opening completed, canceled, or missing items.

## v1.15.18 Release Scope

- The published build used direct full-screen/notification channel selection; current development supersedes this user-facing choice with one unified `提醒` switch while preserving all three stored values for migration compatibility.
- Desktop Web calendar hour labels share the grid coordinate system and align with solid hour rules.
- Ended Desktop Web events fade as complete cards, including title, time, location, border, and background.
- Database version remains `30`; no migration is required.

## Verification Baseline

- `node --check app/src/main/assets/desktop-web/app.js`
- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
- `dotnet publish desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj --configuration Release --runtime win-x64 --self-contained false`
- `tools/Build-PaykiTodoDesktop.ps1` (self-contained `win-x64` publish)
- APK metadata inspection for package/version/min/target SDK
- Release APK v2 signature verification with `apksigner`
- `git diff --check`
- `python -m unittest server.test_paykitodo_cloud_server -v`
- 公网云服务健康检查、账号注册/鉴权和管理员页访问检查
- `dotnet build desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj --configuration Release`
- Desktop Web calendar screenshot verification using production CSS

## Remaining Real-Device Risk

- Full-screen reminder behavior depends on Android/OEM permissions, background-launch policy, exact-alarm access, and battery management.
- Launcher widget rendering and notification appearance can differ across OEM launchers and notification panels.
- Desktop Sync requires a reachable trusted LAN; campus networks may expose an address while client isolation still blocks peer access.
- Remote sync is currently encrypted whole-snapshot synchronization rather than field-level operation merge. Conflicts are blocked and must be resolved by explicit local/remote restore; there is no automatic field-level merge.
- The administrator service is deployed behind the owner's private Cloudflare hostname through an independent Nginx server block; the real hostname, DNS values, Cloudflare settings, certificate paths, administrator password, and server pepper remain outside Git.
- The origin certificate is issued specifically for the private PaykiTodo hostname and Certbot renewal is enabled. The public route exposes only HTTPS `443`; the cloud process remains loopback-only on `18766` and is not the 1Panel service.
- Cloudflare WAF/rate-limit policy still needs periodic review in the owner's Cloudflare account. The current application-level registration/login rate limits and administrator web-session login are active, but administrator MFA is not implemented yet.
- The Windows cloud mode is writable against its local copy; LAN mode still requires a running phone service and all modes require WebView2 Runtime.
- Desktop self-contained publish reports a non-fatal `WindowsBase` assembly version warning from the WebView2 package's WPF reference while compiling the WinForms shell.
- A release build signed with a different key cannot directly replace a locally installed debug build. Export a backup before switching signatures.

## Documentation Lifecycle

- Public project information: root `README.md`, `CHANGELOG.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, and related community files.
- Live maintainer truth: `docs/current/`.
- Completed goal prompts: `docs/goals/`.
- Obsolete requirements, recovery notes, and old status snapshots: `docs/archive/historical/`.
