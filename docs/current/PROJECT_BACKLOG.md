# Project Backlog

This file is the current high-level backlog for PaykiTodo. Historical long-form TODO material has been archived at:

- `docs/archive/historical/PROJECT_BACKLOG_LEGACY.md`

## Current Priorities

1. Keep GitHub-facing project presentation clean:
   - README should stay concise and public-facing.
   - CHANGELOG should stay structured by user-visible version.
   - `.github/` templates and CI should stay valid.
   - Release notes should match the latest published APK.
2. Continue real-device verification for Android-specific surfaces:
   - launcher widgets;
   - lock-screen / full-screen reminders;
   - vibration and sound channels;
   - OEM battery and alarm behavior.
3. Keep desktop Web UI moving toward phone parity:
   - todo editor;
   - event editor;
   - Planning Desk;
   - daily board.
4. Continue Planning Desk usability polish:
   - Outliner input flow;
   - publish / draft clarity;
   - AI recognition preview;
   - conflict handling.
5. Keep reminder reliability and diagnostics strict:
   - exact alarm permission handling;
   - fallback notifications;
   - safe startup recovery;
   - diagnostic logs without leaking private data.

## Verification Backlog

- Test latest GitHub release APK on a real phone after each public release.
- Verify widgets after launcher resize, date change, timezone change, and app update.
- Verify desktop sync on trusted LAN and ensure it stops when disabled.
- Keep secret handling covered: `PlanningAiProviderSerializationTest` verifies provider JSON and `BackupSnapshot` export/import handling omit AI API keys and Desktop Sync tokens for backups; release QA should still spot-check a real exported backup file before public distribution.
- Verify backup / restore after database migrations.

## Documentation Rules

- Put public user/developer information in root-level files such as `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `SECURITY.md`, and `PRIVACY.md`.
- Put live maintainer state in `docs/current/`.
- Put old session logs, historical prompts, and obsolete requirement notes in `docs/archive/` or `docs/goals/`.
- Do not put secrets, signing material, generated APKs/AABs, private Base URLs, or personal backups/logs into any committed document.
