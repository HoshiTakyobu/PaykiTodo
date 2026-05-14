# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.13` / `versionCode 170`, focused on Planning Desk parser boundary fixes called out by syntax review.

The syntax review conclusion:

1. Planning Desk `#remind`, phone reminder editors, calendar batch `Remind=`, and snooze input now share the same reminder parser on Android.
2. Todo batch import still had a narrower DDL parser than Planning Desk, so natural forms such as `5.28`, `5月28日`, `明天`, and `周五` were not accepted there.
3. Desktop Web todo/event reminder input still lagged behind the Android parser and did not support `2:30 pm`, relative dates, weekdays, dot dates, Chinese dates, date-comma-time tokens, or Chinese comma splitting.
4. Calendar batch import remains intentionally stricter because it is a structured multi-field import format; that boundary should be documented rather than hidden.

Completed in the `1.7.11` syntax implementation round:

1. Todo batch DDL now reuses Planning Desk date-time parsing.
2. Todo batch DDL accepts `HH:mm`, `5.28`, `5月28日`, `明天`, `周五`, `2026年5月28日`, and date-only values default to `23:59`.
3. Android reminder parsing now accepts relative dates and weekdays such as `明天 16:30` and `周五 16:30`, and treats `5.28,14:30` / `5月28日，14:30` as one concrete reminder token.
4. Desktop Web reminder parsing now accepts AM/PM times, relative dates, weekdays, dot dates, Chinese month-day dates, full Chinese dates, date-comma-time tokens, and Chinese comma separators.
5. Desktop Planning Desk preview edits now reuse Planning Desk date parsing for edited DDL / start / end fields instead of falling back to a narrower desktop-only syntax.
6. Input help, Wiki, README, CHANGELOG, TODO, and current docs were updated to describe the broader syntax.
7. App version metadata moved to `1.7.11` / `versionCode 168`.

Completed in the `1.7.12` documentation/UI-copy round:

1. Phone Planning Desk help now documents common DDL forms, natural reminder forms, and preview-edit time behavior.
2. Todo batch import dialog/help now states that English commas split fields and date/time inside one field should use a space or Chinese comma.
3. Calendar batch import help now documents `Remind=` support for the shared reminder syntax, including natural dates and Chinese date-time commas.
4. Built-in Wiki now documents the same field boundary and reminder syntax across Planning Desk, todo batch import, calendar batch import, and desktop Planning Desk.
5. Desktop Web Planning Desk help and reminder placeholders now show the same accepted reminder examples.
6. App version metadata moved to `1.7.12` / `versionCode 169`.

Completed in the `1.7.13` parser boundary-fix round:

1. Inline leading dates no longer pollute natural event titles.
2. Heading date context is explicit and resets on plain headings such as `# 收集箱`.
3. Date headings can include a description, such as `# 5/28 周末计划`, while descriptive headings such as `# 我的明天计划` are not treated as dates.
4. Natural schedules can have the time range later in the line, such as `复习 14:00-16:00`.
5. Slash dates, common full-width separators, Chinese AM/PM, and full-width time-range separators are normalized before parsing on phone-side Planning Desk / reminder input, and desktop web reminder parsing has the same common input coverage.
6. Top-level events can be used as parent titles for indented subtasks.
7. Unsupported semantic tags such as `#today`, `#tomorrow`, `#important`, and `#project` remain visible in titles instead of being silently stripped.
8. App version metadata moved to `1.7.13` / `versionCode 170`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.13-debug.apk`
2. open 我的任务 -> 批量待办
3. verify rows like `5.28,整理材料,5`, `5月28日,整理材料,5`, `明天,整理材料,5`, and `周五,整理材料,5` parse correctly
4. verify `16:30,写报告,5` still means today 16:30
5. enable desktop sync and test desktop todo/event reminder inputs such as `5,15,2:30 pm,明天 16:30,周五 16:30,5.10 15:00,5月10日，14:30`
6. verify Planning Desk rows such as `5.28 14:00-16:00 小组讨论`, `复习 14:00-16:00`, and `5/28 下午 2:30～下午 4:00 小组讨论`
7. verify headings such as `# 我的明天计划`, `# 明天`, `# 收集箱`, and `# 5/28 周末计划` behave as documented
8. verify desktop web todo/event reminder fields accept `下午 2:30`, `5/10 下午 2:30`, and `5月10日，14:30`
9. open Planning Desk help, todo batch help, calendar batch help, built-in Wiki, and desktop Planning Desk help to verify the same syntax rules are explained consistently
10. verify invalid desktop reminder times still surface an error instead of silently saving bad offsets

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

No external file or API key is needed for the current `1.7.13` verification task.
