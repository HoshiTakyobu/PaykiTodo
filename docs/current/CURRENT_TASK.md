# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.11` / `versionCode 168`, focused on checking whether the app's text-input syntaxes are internally reasonable and reducing the most visible inconsistencies.

The syntax review conclusion:

1. Planning Desk `#remind`, phone reminder editors, calendar batch `Remind=`, and snooze input now share the same reminder parser on Android.
2. Todo batch import still had a narrower DDL parser than Planning Desk, so natural forms such as `5.28`, `5月28日`, `明天`, and `周五` were not accepted there.
3. Desktop Web todo/event reminder input still lagged behind the Android parser and did not support `2:30 pm`, relative dates, weekdays, dot dates, Chinese dates, date-comma-time tokens, or Chinese comma splitting.
4. Calendar batch import remains intentionally stricter because it is a structured multi-field import format; that boundary should be documented rather than hidden.

Completed in this round:

1. Todo batch DDL now reuses Planning Desk date-time parsing.
2. Todo batch DDL accepts `HH:mm`, `5.28`, `5月28日`, `明天`, `周五`, `2026年5月28日`, and date-only values default to `23:59`.
3. Android reminder parsing now accepts relative dates and weekdays such as `明天 16:30` and `周五 16:30`, and treats `5.28,14:30` / `5月28日，14:30` as one concrete reminder token.
4. Desktop Web reminder parsing now accepts AM/PM times, relative dates, weekdays, dot dates, Chinese month-day dates, full Chinese dates, date-comma-time tokens, and Chinese comma separators.
5. Desktop Planning Desk preview edits now reuse Planning Desk date parsing for edited DDL / start / end fields instead of falling back to a narrower desktop-only syntax.
6. Input help, Wiki, README, CHANGELOG, TODO, and current docs were updated to describe the broader syntax.
7. App version metadata moved to `1.7.11` / `versionCode 168`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.11-debug.apk`
2. open 我的任务 -> 批量待办
3. verify rows like `5.28,整理材料,5`, `5月28日,整理材料,5`, `明天,整理材料,5`, and `周五,整理材料,5` parse correctly
4. verify `16:30,写报告,5` still means today 16:30
5. enable desktop sync and test desktop todo/event reminder inputs such as `5,15,2:30 pm,明天 16:30,周五 16:30,5.10 15:00,5月10日,14:30`
6. verify invalid desktop reminder times still surface an error instead of silently saving bad offsets

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

No external file or API key is needed for the current `1.7.11` verification task.
