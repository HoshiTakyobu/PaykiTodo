# Session Handoff

## Current 2026-10-19: 1.16.22 Windows UI / 不干扰测试已完成

从 main / 429f6da 完成 1.16.22 / 387、Windows1.16.22.0，Room31 不变。原生 WPF 字体、导航、控件、浅深色层次、原品牌图标、编辑/确认/提醒/账号/AI 弹窗已统一。实际画面发现并修复日期时间裁切、窄窗口规划操作越界。保留退出锁定、过期离线、revision/dirty 与传输期间新写入保护。最新制品和验证见 [VALIDATION_1.16.22.md](VALIDATION_1.16.22.md)，不要将下方历史1.16.21制品当作最新。

- 当前 APK：`app/build/outputs/apk/debug/PaykiTodo-1.16.22-debug.apk`；签名 Release：`app/build/outputs/apk/release/PaykiTodo-1.16.22-release.apk`。Windows：`desktop/dist/PaykiTodo.exe`，必须保留完整 dist。哈希见 PROJECT_STATUS / VALIDATION。
- 最终 Windows 运行：`tools/tests/Invoke-DesktopAcceptance.ps1 -PackagedExe .\desktop\dist\PaykiTodo.exe -Evidence G:\Workspace\Scratch\PaykiTodo\ui-1.16.22\final-4`；19 回归+1项14阶段控件流程。独立桌面不切换到用户可见桌面、不调用全局输入或剪贴板。不要直接启用物理输入模式干扰用户。
- Android：186 单测、Lint 0 errors/128 warnings、Debug/Release 和签名验证通过。沿用 `emulator-5554` / `Pixel_8`，先备份 `G:/Workspace/Backups/PaykiTodo/emulator-before-1.16.22-20261019-0956/private-data.tar`（1,227,776 bytes / 53 entries），再原地升级 Debug，检查387/1.16.22及真实登录界面。未清除数据、未创建手机合成业务数据，模拟器继续运行。Android 本轮只有版本变更，不把历史26项仪器测试称为新跑。
- Windows 用户正式资料保持不变，验收使用 Scratch 内独立 profile；新 `--data-directory` 隔离数据、凭据及验证资料。进程退出后不保证提醒，农历/OCR/多时段创建、OEM长待机/锁屏及真实账号验证 E2E 仍是边界。
- 不推送、发布或修改生产服务器。本轮完成后按项目规则中文聚焦提交；下次先核实 git HEAD/status，不依赖聊天版本。

## Historical handoff: 1.16.21 / 386 actual-operation audit (2026-10-18)

The full request is preserved in REQUEST_2026-10-17.md. The implementation landed at 30c3176; Room remains31. Do not interpret the previous completed heading as full parity/all-scenario acceptance. See INTERACTIVE_VALIDATION_1.16.21.md for the user's actual-operation audit, failures and boundaries; VALIDATION_1.16.21.md preserves the build record. Expiry and explicit logout have different local access gates; keep this distinction. Do not reintroduce title-required checks into archive readers or client save buttons. Sync requests must preserve newer local edits and reject stale cloud reviews.

- Debug: `app/build/outputs/apk/debug/PaykiTodo-1.16.21-debug.apk`; SHA-256 `16FF0F8D56AA57F92798D6C4FE9E31477248522A91326D443851C9DF2ACA8330`.
- Signed Release: `app/build/outputs/apk/release/PaykiTodo-1.16.21-release.apk`; SHA-256 `DD073D336E620D5A28DC11A5433D3E0AF825B3AEFBBB5E3CEF9B9CA5B2E68093`.
- Windows: `desktop/dist/PaykiTodo.exe` (`1.16.21.0`); SHA-256 `552485021D2C17C7F0D60E936F907D9DEA45D23DA54EEF237C3BFD326AB553A7`. The distribution directory retains the private ignored client config.
- Validation: 186 unit tests, Lint (0 errors / 128 warnings), 23 Android instrumentation tests, 18 Windows tests, 12 server tests, LAN syntax/browser checks, builds and Release APK v2 signature verification passed.
- Emulator: existing `Pixel_8`, `emulator-5554`, Android 14; installed the current Debug/AndroidTest APKs. Checked notification-to-account routing, neutral dark Settings, real offline background expired-account reminder delivery, titleless archive restore, calendar/daily-board and workspace controls. Emulator remains running. Wi-Fi/mobile data were disabled only for the focused reminder run and re-enabled afterward.
- Evidence: local `G:/Workspace/Scratch/PaykiTodo/acceptance-1.16.21/`; pre-test private-data backup: `G:/Workspace/Backups/PaykiTodo/emulator-before-1.16.21-20261018-175229/`. Tests restore preference values and remove only their own synthetic tasks. No personal data or credential dumps are committed.
- Physical OEM long-idle/lock-screen/vibration remain unverified. Native Windows lunar/OCR/multi-slot creation are still gaps; Windows reminders need the process running. These are boundaries, not completed parity claims.
- No push, tag, GitHub Release or production-server change.

### Actual-operation follow-up

- Added test-only account/sync integration, formal login/conflict real-touch components, external ADB fixture/helpers, visible Edge-to-phone LAN script and opt-in OS-input WPF flow. No product source/version changes; Debug/Release/EXE hashes above unchanged, only AndroidTest rebuilt/installed.
- Original23 Android regressions passed on the user's same Pixel_8; new3 local-server tests passed together59.778s;18 Windows regressions and1 visible12-stage WPF flow56s passed.186 units/Lint and12 server tests rerun. Components/steps are not counted as full app E2E.
- Actual phone notification click/account/settings/titleless checklist/duplicate/todo/history/calendar creation/planning import/reminder snooze/completion/restore performed. Reboot offline expired-account alarm rebuilt and notified; insufficient permission fallback delayed. Pixel Launcher ANR blocked full-screen takeover proof.10-minute second delivery, calendar modification save and OEM behavior not passed.
- Evidence `G:/Workspace/Scratch/PaykiTodo/interactive-1.16.21/`. Backup `G:/Workspace/Backups/PaykiTodo/emulator-before-interactive-20261018/private-data.tar`;19 archived files matched SHA-256 on final restore. Extra synthetic diagnostic/safety files were not silently deleted. Emulator running at original login page; Wi-Fi/data on, app-ops default,18765 forward removed, local fixture process stopped. Windows user data/configs left unchanged.
- New external fixture is opt-in and does NOT restore itself; require a full private-data backup and restore in external harness even after failure. Local TLS exceptions must remain androidTest-only. WPF tests own foreground input/clipboard; don't run them while user is editing.

## Historical handoff baseline (2026-09-23)

The four latest user requests are recorded at the top of CURRENT_TASK.md. This round is complete on source `1.16.20 / 385` at commit `7e4f8da`; the working tree is clean and no local commit is pending. Native Windows code is in `NativeWindow*.cs`, `NativeEditors.cs`, `DesktopWorkspace.cs`, and `DesktopReminders.cs`; `Program.cs` launches WPF and the old MainForm/HTML renderer is not the default workspace. Temporary reference images and screenshots are under ignored `.tmp/resume-20260923/`. The `Pixel_8` AVD was used as `emulator-5554` for validation.

## v1.16.20 Verification Evidence

- Android Debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.16.20-debug.apk`; SHA-256 `2119C69EAC9C6062A1581D2699BB29E916FCB64B47B49D75103D212E56E8E5B0`.
- Signed Release APK: `app/build/outputs/apk/release/PaykiTodo-1.16.20-release.apk`; SHA-256 `FA47E46D44D82375C4EC78559856667E7089E925704656CA814AA53C881D525D`.
- Windows self-contained output: `desktop/dist/PaykiTodo.exe`; SHA-256 `10B5C5396E582F366921AE6DD84530C0226121B9324D6E5BA987B06576F9A1DA`.
- Android: 185 unit tests, Lint, Debug/AndroidTest/Release assembly, and the dedicated daily-board instrumentation test passed on `emulator-5554` / `Pixel_8`.
- Windows: 12 tests passed, including native navigation/editing, reminder offsets, recurrence deletion, logout locking, migration field preservation, local-server two-device sync, profile/device APIs, and password rotation.
- LAN Web: `node --check` and `tools/tests/desktop-web-connection.cjs` passed; all eight modal classes, pending promises, stale responses, disconnect and reconnect errors are covered.
- Known limits are recorded in CURRENT_TASK.md; no push, tag, or GitHub Release was performed.

## Previous Completed State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.16.20 / versionCode 385` (local validation; not published)
- Room database version: `31`
- Latest public GitHub Release: `v1.15.18`
- Android outputs after final verification:
- `app/build/outputs/apk/debug/PaykiTodo-1.16.20-debug.apk`
- `app/build/outputs/apk/release/PaykiTodo-1.16.20-release.apk`
- Windows output: `desktop/dist/PaykiTodo.exe` is rebuilt with `1.16.20` metadata.
- Private cloud deployment: service enabled on the Azure host, origin loopback `127.0.0.1:18766`, public access through the privately configured Cloudflare/Nginx HTTPS hostname, and the administrator page at `/admin`.

## v1.16.19 Completed

- Quick checklist is implemented across Android, LAN Desktop Web, and the Windows client: parser, live preview, hierarchical creation, notes, folding, completion, cancel/delete, history restore, backup/snapshot fields, and Widget summary.
- LAN Web and Windows now display historical todos and expose restore/delete actions; restoring a checklist node restores only its subtree and recalculates valid ancestors. LAN Web filters checklist nodes out of ordinary history so each historical task is rendered once.
- Parent completion excludes canceled children, cannot revive canceled ancestors, and recurses through valid ancestors. Deletion and cancellation follow the existing history rules.
- Error diagnostics select the matching source line across Android, LAN Web, and Windows; save failures select the first invalid line.
- Widget rows show checklist state and progress, while completed checklist items only return to today's Widget when they were completed today.
- Windows complete/cancel actions preserve canceled and completed terminal states, and fresh snapshots create the default group using the correct 64-bit ID type.
- Android Turnstile fallback polling now has native lifecycle ownership, explicit timeout handling, duplicate suppression, and cleanup on WebView release.
- Administrator timestamps use epoch values and browser-local formatting.
- Room is `31`; migration `30 -> 31` and committed schema `31.json` preserve existing installations.
- Focused Android unit/instrumentation tests, JavaScript syntax, Windows build, and server integration tests pass. No push, tag, or GitHub Release was performed.

## v1.16.19 Verification Evidence

- Android unit tests: 185 passed; instrumentation: 18 passed on `emulator-5554` / `Pixel_8`; server suite: 12 passed; Windows tests: 5 passed; JavaScript syntax, Android Lint, Debug/AndroidTest/Release assembly, and Windows self-contained publish passed.
- The current Debug APK was installed and launched on `emulator-5554` / Android 14; package `com.paykitodo.app` reported `versionName=1.16.19` and `versionCode=384`.
- Release APK v2 verification passed with one RSA-2048 signer. Debug SHA-256: `F80C5B63E07EF7B7AA53B1620DEAA2D6190C236EAC7A18598C33D07DB61ED722`; signed Release SHA-256: `3ADB690F88C80385669156AFC7939B4A9738C96D29CA7B59ACAF5BFB45CBF295`; Windows EXE SHA-256: `19BFC2AB4370B887112D45E252C4AFE9EF23997E1D250FC9D39492C90E289279`.
- Room `30 -> 31` is exercised against a real version-30 database; backup compatibility tests confirm old archives without checklist fields import as empty checklist metadata.
- Server integration suite: 12 passed, including browser-local timestamp payload behavior.
- `node --check app/src/main/assets/desktop-web/app.js`, Android Lint, Debug/AndroidTest/Release assembly, 5 Windows tests, and Windows self-contained Release publish passed.
- The prior `1.16.18` Debug APK evidence is superseded by the `1.16.19` installation evidence above.

## Prior `v1.16.18` Verification Evidence
- Release APK v2 signature verification passed with one RSA-2048 signer. Debug APK SHA-256: `63F727FE4BC5257B9EC0506E84121083637F07E02B53410B64ED63008B3D5A5F`; signed Release APK SHA-256: `7002003829AE1ED4B84FF28ACFC4E3D9A4E1426F5CFF8938D56A7552E7D922D0`; Windows self-contained EXE SHA-256: `403B143A0D7E062C6422055406BBB8C2F861D398BDC891812602AC06F5777A83` (file version `1.16.18.0`).

## v1.16.15 Completed

- Implements the user's 17 UI/account requests, as tracked in CURRENT_TASK.md.
- New account profile, password rotation and device-status APIs deployed to the existing private Azure service. The old server source/config is backed up remotely for rollback; no business records were changed by deployment.
- Server suite: 11 passed. Android unit tests, Lint, Debug/Release assembly, and all 9 instrumented tests passed.
- Emulator: started existing `Pixel_8`, device `emulator-5554`, installed the versioned Debug and instrumentation APKs. Test components use synthetic fixtures and do not bypass the production login gate.
- First calendar run caught excessive-index precision loss; pager windows now use bounded indices and recenter at 50-year boundaries. Month switching/double-tap/continuous week reversal passed after that fix.
- Screenshots are under ignored `user/ui-acceptance/`. Two initial test harness errors (short-hold helper and duplicate preloaded month date nodes) were corrected before the final run.
- Final verification is complete. Physical-device OEM checks remain for lock-screen/full-screen reminders, notification shade rendering, launcher widgets, and background battery policy. No push/tag/GitHub Release was performed.

## v1.16.15 Verification Evidence

- `emulator-5554` / `Pixel_8` installed `PaykiTodo-1.16.15-debug.apk` and `PaykiTodo-1.16.15-debug-androidTest.apk`.
- `com.example.todoalarm.ui.CalendarInteractionTest` and `WorkspaceControlsTest` passed all 9 tests, including calendar paging/today navigation, dark agenda readability, long-press batch entry, drawer account identity, provider title wrapping, and document actions.
- Normal `MainActivity` launch succeeded; package inspection reported `versionName=1.16.15` and `versionCode=380`.
- Build outputs are `app/build/outputs/apk/debug/PaykiTodo-1.16.15-debug.apk` and `app/build/outputs/apk/release/PaykiTodo-1.16.15-release.apk`.
- SHA-256: Debug `054169AA284863F486D7EE7BAFC9D169CA63A9449985BC62BA78C45A8A3009B4`; Release `1FEF71C5367109E759E20F5FDCFD64CB4D05498CF268D82EC850C31A3DE79390`. `apksigner verify --verbose --print-certs` passed Release with one RSA-2048 signer.

## v1.16.14 Product Changes

- Android Turnstile WebView now allows Cloudflare's required `about:blank` and `about:srcdoc` internal documents and keeps the restrictive challenge CSP compatible with the private proxy's injected Insights script.
- One token is delivered to Compose only once, and AndroidView uses current callbacks after recomposition so a successful widget updates the active login screen.
- Android and Windows metadata is `1.16.14 / versionCode 379`.

## v1.16.14 Verification Evidence

- Server tests, Android unit tests, and Android Lint passed; Debug and Release APKs exist for `1.16.14`.
- Azure service restarted successfully; public `/healthz` returned `200`, and the challenge CSP includes `about:blank` / `about:srcdoc`.
- The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the widget. Cloudflare documents the observed 401 Private Access Token probe as non-fatal; final token acceptance remains a physical-device check because the emulator challenge was intermittent.

## v1.16.13 Product Changes

- The existing Cloudflare Turnstile widget now includes the production hostname and remains in managed/no-clearance mode with its local hosts preserved.
- The server stores the existing Secret only in `/etc/paykitodo/cloud.env` and requires `success=true`, `action=account_auth`, and the configured production hostname from Siteverify.
- Android's challenge WebView keeps the official callback and also observes the response field to handle WebViews that show Cloudflare success without invoking the parent callback.
- Android and Windows metadata is `1.16.13 / versionCode 378`.

## v1.16.13 Verification Evidence

- Server unit/integration suite: 9 tests passed.
- Android unit tests and Debug/Release assembly passed.
- Public health and challenge checks passed; dummy registration was rejected with `403 human_verification_failed`.
- The emulator disconnected before the final rebuilt APK could be installed; the earlier session showed the Cloudflare widget reaching `Success!`.

## v1.16.12 Product Changes

- Normal reminder acknowledgement, completion, notification retention, and stale-alarm rejection preserve the current reminder's dispatch claim instead of clearing it.
- Reminder dispatch state is reset only by deliberate todo/event schedule changes through Android or LAN Desktop Web, preventing the polling fallback from re-firing a past trigger.
- AI reports remain AI-first with explicit local-template fallback; model discovery and ordered fallback remain available.
- Synthetic periodic prompts intended to look like user activity are intentionally not generated.
- Android and Windows metadata is `1.16.12 / versionCode 377`.

## v1.16.12 Verification Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed; Lint reported 0 errors and 125 non-blocking warnings.
- ADB device `emulator-5554` / AVD `Pixel_8` installed and launched `PaykiTodo-1.16.12-debug.apk`; package metadata reported `versionName=1.16.12` and `versionCode=377`, and the account page rendered without a fatal crash.
- Debug APK SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
- Release APK SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
- Windows EXE SHA-256: `39BE39CA7C26E36413102AE14331E56EA822FDD8A89EB7200FB3E959302447DE`.
- Physical-device reminder behavior still requires an OEM test for lock-screen and full-screen delivery.
- Windows publish completed with one existing non-fatal `WindowsBase` 4.0/5.0 WebView2/WPF assembly warning.

## Operational Notes

- When Android has permission to use notification full-screen intents, the system notification is the only reminder surface. The accessibility overlay and direct Activity launch are fallback paths for devices where Android rejects or cannot use that mechanism, reducing the chance of two different reminder UIs appearing for one trigger.
- The production authentication challenge is now backed by the existing Turnstile widget. The Secret and hostname allowlist remain in the private Azure environment and are never included in Android or Git.
- AI provider failover is limited to real planning/report/vision calls. No periodic prompts are generated to disguise automated traffic or evade provider safeguards.

## v1.16.10 Product Changes

- Reminder polling passes the concrete trigger timestamp into the foreground service, matching alarm-receiver deduplication.
- New schedules omit the legacy `+2s` backup alarm; application startup cancels old backup PendingIntents.
- Planning Desk direct Room writes and snapshot import notify the shared widget/remote-sync callback.
- Turnstile WebView accepts the Cloudflare challenge host and third-party cookies and blocks HTTP/render-process failures.
- AI reports remain AI-first with an explicit local-template fallback; synthetic anti-detection traffic is not generated.
- Android and Windows metadata is `1.16.10 / versionCode 375`.

## v1.16.10 Verification Evidence

- Server integration tests and Android unit tests passed before the version bump.
- The cloud source and Turnstile environment were redeployed on 2026-09-06; `/healthz` returned `200`, `/v2/auth/challenge` returned the real widget, and a dummy registration token was rejected.
- Final Android Lint, Debug/Release assembly, and emulator smoke verification remains after the version bump.

## v1.16.9 Product Changes

- Added optional Cloudflare Turnstile to Android login, registration, and password recovery.
- Authentication requests are accepted only after server-side Siteverify when a Turnstile Secret Key is configured; the secret remains outside Android and Git.
- Partial Turnstile configuration fails closed instead of leaving the authentication endpoint unprotected.
- Kept IP rate limiting as a second layer and preserved compatibility when the feature is not configured.

## v1.16.9 Verification Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 7 tests passed.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- ADB device `emulator-5554` / AVD `Pixel_8` installed `app/build/outputs/apk/debug/PaykiTodo-1.16.9-debug.apk`; package inspection reported `versionName=1.16.9` and `versionCode=374`.
- The app launched `MainActivity` and the UI tree exposed the account login, registration, and password recovery controls. No fatal crash was observed during launch.
- No authenticated reminder smoke test was run on the emulator. Because the app had no test account/session, the scheduled reminder could not be created through the UI; full-screen/notification behavior remains subject to the separate permission and OEM checks documented below.

## v1.16.8 Product Changes

- Fixed first-login cloud snapshot download for an empty device by initializing its local sync revision at zero.
- Preserved local-first upload behavior and explicit conflict protection when both local and cloud data exist.

## v1.16.7 Product Changes

- The unauthenticated account page no longer exposes local migration export or import; both operations remain under authenticated `设置 -> 数据与备份`.
- Debug-to-Release migration instructions now require logging in before exporting from the old installation and before importing into the new installation.
- Version metadata is `1.16.7 / versionCode 372`.

## v1.16.7 Verification Evidence

- Android `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease` passed for `1.16.8`.
- `emulator-5554` was previously updated with `PaykiTodo-1.16.7-debug.apk`; the `1.16.8` build was verified by unit tests and signed APK assembly. The unauthenticated account page remains free of local migration actions.
- `python -m unittest server.test_paykitodo_cloud_server -v` passed all 5 integration tests.
- PowerShell parser checks, `git diff --check`, and ignored sensitive-path checks passed. No production credential rotation was run.

## v1.16.6 Product Changes

1. The account login/register/recovery card is vertically centered and remains scrollable on compact screens.
2. Unauthenticated users can export local data, but migration import is only exposed after successful login in `设置 -> 数据与备份`.
3. Normal account and recovery passwords use salted `scrypt` hashes with server pepper; the separate administrator deployment credential remains outside Git in the protected server environment.
4. Version metadata is `1.16.6 / versionCode 371`.

## v1.16.5 Product Changes

1. Reminder delivery catches Android background foreground-service start rejection and falls back to a detailed notification, preserving the full-screen pending intent when configured.
2. Already-started calendar events may retain past reminder settings; the scheduler ignores missed trigger times instead of treating the event as invalid.
3. The account screen uses the existing dashboard background and launcher icon and remains scrollable for small devices.
4. The administrator credential helper reads by default and rotates only with explicit `-Rotate`, including service restart and post-rotation verification.
5. Version metadata is `1.16.5 / versionCode 370`.

## v1.16.2 Product Changes

1. The unauthenticated Android login surface now exposes local migration-package export/import actions, so Debug-to-Release migration does not require a cloud login first.
2. Secondary entry points, reminder surfaces, widgets, and phone LAN access require a valid account session; session expiry stops cloud sync and phone LAN service.
3. Logout explicitly stops the phone LAN service and refreshes widget data.
4. Reminder acknowledgement/snooze removes stale notification and foreground-service artifacts; long-title reminder actions remain readable on narrow screens.
5. Automatic backup writes one latest archive and cleans up legacy timestamped archives.

## v1.16.1 Product Changes

1. The administrator page now renders an in-page PaykiTodo login form for unauthenticated browser requests; it no longer relies on a native Basic Auth dialog.
2. Successful login creates an HttpOnly, Secure, SameSite cookie session; logout clears it. CSRF protection remains required for administrator state changes.
3. Valid Basic Auth remains available for non-browser scripts without sending an authentication challenge for missing or invalid credentials.

## v1.16.0 Product Changes

1. `server/paykitodo_cloud_server.py` implements user registration/login, short access tokens, rotating refresh tokens, devices, encrypted snapshot revisions, idempotent uploads, audit records, and a Basic-Auth administrator page with CSRF-protected state changes.
2. The cloud origin defaults to `127.0.0.1:18766`; public clients use Cloudflare/Nginx HTTPS `443`. Real hostnames, IPs, certificates, environment values, cloud databases, and administrator credentials are not in Git.
3. Android Settings -> Data and Backup exposes cloud registration/login, account state, revision, pending/conflict/error state, manual sync, cloud inspection, and logout.
4. Android keeps Room authoritative, stores tokens/snapshot keys in encrypted preferences, uploads after local writes, pulls only when the local copy is clean, and blocks concurrent dirty revisions instead of silently overwriting.
5. Windows cloud mode owns a DPAPI-protected local `.paykibak` working copy, works offline, and can add/edit/complete/cancel/delete todos, add/edit/cancel/delete events, configure core reminder/recurrence/countdown fields, and edit a Planning Desk memo.
6. Windows phone-LAN compatibility mode still embeds the phone's `18765` page and directly changes phone data. It is not the cloud client and does not require the cloud account server.
7. The legacy `/v1/sync/{deviceId}` encrypted relay remains available for old Android settings.

## Verification

- Python cloud integration tests cover register/login, wrong password, token refresh, device list, versioned upload, idempotency, conflict rejection, encrypted snapshot download, administrator authentication, CSRF rejection, user disable/enable, and device revocation.
- Public deployment smoke checks cover HTTPS health, account registration/authentication, administrator page access, and cleanup of disposable deployment test accounts.
- Android cloud tests cover stable cross-client account-key derivation and AES-GCM wrong-key rejection.
- Android compile and full unit tests, Debug/Release APK assembly, Lint, Windows Release build/publish, and `git diff --check` must be rerun after the final documentation commit.
- The existing non-fatal WebView2 `WindowsBase` assembly warning remains during Windows build.

## Deployment Completed / Follow-Up

1. The private server environment is installed outside Git with random administrator/password-pepper secrets; the service runs as the dedicated `paykitodo` user and stores SQLite under `/var/lib/paykitodo`.
2. Nginx proxies the private HTTPS hostname to `127.0.0.1:18766`; the hostname-specific origin certificate is installed and Certbot renewal is enabled. The old `paykitodo-sync.service` remains unchanged.
3. Cloudflare proxy and HTTPS routing are working. WAF/rate-limit policy should be reviewed in the Cloudflare console before adding real users.
4. Android and Windows real-client login/offline edit/conflict testing remains the next device-level acceptance step; do not migrate important data until that smoke test passes.

5. Android Release acceptance on 2026-09-02: device `emulator-5554` installed signed `PaykiTodo-1.16.3-release.apk`, then the authentication refresh fix was verified and the current source was advanced to `1.16.4`. Login, relaunch persistence, daily board, drawer, todos, calendar, Planning Desk, settings, and appearance navigation were checked. Physical Android reminder behavior and real Android/Windows account conflict testing remain outstanding.
6. The account transition fix runs login/registration on `Dispatchers.IO` and explicitly signals the ViewModel after success; without this, a successful server response could leave the login screen visible until a later lifecycle refresh.
7. UI audit evidence is recorded in `docs/current/ANDROID_RELEASE_UI_AUDIT_1.16.3.md`; screenshots and XML dumps remain only in ignored `.tmp/audit-1.16.3/`.
8. The cloud service was redeployed from the current source on 2026-09-02; its HTTPS reverse-proxy `/admin` check returned the in-page login UI without `WWW-Authenticate`.
9. The local `user/Build-PaykiTodoRelease.ps1 -SkipDebugBuild` wrapper remains the local Release build entry point; signing material, output APKs, and checksums remain ignored local artifacts.

10. On 2026-09-02, `PaykiTodo-1.16.5-release.apk` was installed on `emulator-5554` after clearing the emulator app data. The unauthenticated account screen was visually checked in its default light theme: dashboard background, launcher artwork, login form, agreement control, and local migration actions were all visible and the page remained scrollable. Dark-theme and physical-device OEM reminder behavior remain follow-up checks.

## Known Limitations

- Synchronization is versioned whole-snapshot sync, not field-level operation merge. Conflicts stop for explicit resolution.
- The Windows client covers core writable todo/event/planning fields but is not yet feature-identical to every Android-only surface such as alarm delivery, widgets, AI reports, check-in runtime, and advanced recurrence scope operations.
- The administrator page now uses an HTTPS-only HttpOnly web session rather than a browser Basic Auth challenge; administrator MFA is not implemented yet, so the private hostname and Cloudflare access controls remain required.

## Safety

- Never commit private domains, Azure IPs, Cloudflare settings, SSH keys, environment files, SQLite user databases, keystores, passwords, tokens, API keys, real backups, or user screenshots.
- Do not push or create a GitHub Release until the user explicitly requests it.
