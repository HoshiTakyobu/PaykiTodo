# Current Task

## Active Focus

The `v1.15.18` development round is complete and published. The next work should begin from post-release validation rather than reopening old `1.14.x` or early `1.15.x` context.

## Completed Publication Scope

1. Updated public and maintainer documentation to `1.15.18 / versionCode 360`.
2. Published the signed `PaykiTodo-1.15.18-release.apk` through GitHub Release `v1.15.18`.
3. Replaced the obsolete Android-14-only GitHub description with an Android-wide todo and schedule assistant description that acknowledges maintainer-led AI-assisted vibe coding.
4. Removed Claude co-author trailers from the three affected historical commits so automated contributor attribution no longer credits Claude.
5. Preserved all source changes while rewriting only those commit messages.

## Next Validation

1. Install the release APK on a clean or release-signed device profile and smoke-test startup, backup restore, todo creation, event creation, and Desktop Sync.
2. Verify all four reminder-channel states on a physical phone: neither, notification only, full-screen only, and both.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Continue the calendar-todo UX review described in `docs/current/CALENDAR_TODO_AND_UX_REVIEW_PLAN.md` only after release smoke testing.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
