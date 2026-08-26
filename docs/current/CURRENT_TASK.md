# Current Task

## Active Development Focus

The `1.15.8 / versionCode 350` batch-completion, transactional-editor, AI-network, and Todo/Settings UI round is complete.

The next round should start from user testing of the new batch-selection density and AI configuration transaction behavior. Do not reintroduce per-card save buttons or Android's default Dalvik User-Agent.

## Completed Scope

1. Added a Todo batch-selection mode below group filters with individual selection, select-all-visible, clear, exit, and complete-selected actions.
2. Reused existing completion/history/recurrence/reminder cleanup semantics and reduced automatic backup to one pass per completed batch.
3. Changed shared todo/event editors so only the top-right completion action persists; all dismissal routes silently discard drafts.
4. Changed AI configuration to one page-level draft covering Provider operations, overall enable state, report schedules, and retention.
5. Removed separate AI/report save actions; the page-level top-right `完成` is the only persistence action.
6. Limited model connectivity testing to one fixed message and one endpoint, with no retry or fallback after HTTP 429.
7. Applied the locally verified Codex CLI `0.146.0` Windows User-Agent to AI and remote-quote HTTP requests.
8. Refined Todo, Settings, and AI configuration surfaces around solid deep blue-gray operational backgrounds and less nested card styling.
9. Bumped to `1.15.8 / 350` without a database migration.

## Validation

- `:app:compileDebugKotlin` passed.
- `:app:testDebugUnitTest` passed: 22 suites / 137 tests / 0 failures or errors.
- `:app:lintDebug` passed.
- `:app:assembleDebug` passed for `PaykiTodo-1.15.8-debug.apk`.
- APK metadata reports `1.15.8 / versionCode 350`, package `com.paykitodo.app`.
- Pixel_8 emulator verified batch completion, editor draft discard, AI draft discard/commit, and dark Todo/Settings/AI configuration rendering.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.
