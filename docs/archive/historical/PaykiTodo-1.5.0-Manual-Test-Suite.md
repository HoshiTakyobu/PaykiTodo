# PaykiTodo 1.5.0 Manual Test Suite

## 1. Purpose

This document is the executable manual test suite for `PaykiTodo 1.5.0`.

It is intended to solve two problems:

- give the tester a clear checklist instead of vague free-form testing
- give the project a measurable quality baseline instead of only subjective impressions

This suite should be used together with:

- `PaykiTodo-1.5.0-Development-Status.md`
- `PaykiTodo-1.5.0-Spec-Gap-Check.md`
- `PaykiTodo-1.5.0-Batch-Import-Format.md`

## 2. Scope

This round focuses on four major areas:

1. reminder reliability and diagnostics
2. calendar enhancement and weekly-template workflow
3. stronger batch import
4. recurring item editing and preview

## 3. Test Environment Record

Before testing, record the following:

- App version: `1.5.0`
- APK file: `app/build/outputs/apk/debug/PaykiTodo-1.5.0-debug.apk`
- Device model:
- Android version:
- ROM name and version:
- Screen lock type:
- Notification permission: `On / Off`
- Exact alarm permission: `On / Off`
- Full-screen intent permission: `On / Off`
- DND access: `On / Off`
- Ignore battery optimization: `On / Off`
- Accessibility reminder service: `On / Off`
- Reminder mode under test: `Notification / Fullscreen`

## 4. Pass / Fail Standard

Use the following result labels:

- `PASS`: behavior matches expected result completely
- `PARTIAL`: core function works, but detail or UX is wrong
- `FAIL`: function does not work, is unstable, or blocks usage
- `BLOCKED`: cannot test because prerequisite is missing

## 5. Quantitative Quality Metrics

The tester should try to produce measurable results, not only comments.

### 5.1 Reminder Reliability Metrics

- Reminder dispatch success rate:
  - formula: `successful reminders / total reminder attempts`
  - target for current round: `>= 90%` on one stable device configuration
- Fullscreen arrival success rate in unlocked background state:
  - target: `>= 85%`
- Fullscreen arrival success rate in locked-screen state:
  - target: `>= 70%`
  - note: lower target is accepted because ROM-specific restrictions are still a known risk
- False trigger count:
  - target: `0`
- Duplicate trigger count:
  - target: `0`

### 5.2 Stability Metrics

- Crash count in one full test session:
  - target: `0`
- ANR or obvious freeze count:
  - target: `0`

### 5.3 Calendar Workflow Metrics

- Week-template save success rate:
  - target: `100%` on valid weeks with events
- Apply-template-to-target-week success rate:
  - target: `100%`
- Semester-generation success rate:
  - target: `100%` on valid input

### 5.4 Import Metrics

- Valid custom-grammar import success rate:
  - target: `100%`
- Valid CSV / TSV import success rate:
  - target: `100%`
- Valid ICS import success rate for simple `VEVENT` files:
  - target: `>= 90%`
- Invalid input error readability:
  - target: every bad row reports a readable message, not a crash

### 5.5 UX Metrics

Use a 1-5 subjective score.

- Reminder page readability score
- Calendar readability score
- Calendar interaction fluency score
- Batch import learnability score

Current acceptable minimum for this round:

- no score should be below `3/5`

## 6. Test Data Preparation

Prepare the following sample data before testing:

### 6.1 Todo Samples

- one normal single todo with DDL and reminder
- one normal single todo with DDL but no reminder
- one todo without DDL
- one recurring todo: daily
- one recurring todo: weekly multi-day
- one recurring todo: monthly day
- one recurring todo: yearly date

### 6.2 Calendar Samples

- one single event within the same day
- one all-day event
- one cross-day event
- one recurring weekly class event
- one recurring weekly duty event

### 6.3 Import Samples

- custom grammar happy path
- custom grammar with `Group=组名`
- CSV with `group` column
- TSV with `group` column
- ICS with `CATEGORIES`
- invalid mixed input with at least 2 broken rows

## 7. Feature Map

This section helps the tester understand what currently exists.

### 7.1 Reminder Features

- exact reminder scheduling
- reminder chain diagnostics in settings
- quick reminder chain test entry
- notification-mode reminder
- fullscreen reminder path with fallback behavior

### 7.2 Calendar Features

- timeline view
- week view
- month view
- list view
- batch import
- save current week as schedule template
- apply saved template to a target week
- generate semester recurring schedule from saved weekly template

### 7.3 Recurrence Features

- recurring todos
- recurring calendar events
- current-only edit behavior
- current-and-future edit behavior
- all-occurrences edit behavior
- preview recurring generation before confirm

## 8. Core Manual Test Cases

Each case contains ID, target, preconditions, steps, expected result, and metric record.

### 8.1 Reminder Reliability

#### Case R-01: Reminder chain test entry

- Preconditions:
  - app installed
  - notification permission granted
- Steps:
  1. open `设置`
  2. open `提醒链路测试`
  3. set `15` seconds
  4. start test
- Expected:
  - test task is created successfully
  - reminder is triggered near the configured time
  - diagnostics list shows multiple chain stages
- Record:
  - actual trigger delay in seconds:
  - result: `PASS / PARTIAL / FAIL`

#### Case R-02: Foreground reminder

- Preconditions:
  - create one test todo with reminder 1-2 minutes ahead
  - keep app in foreground
- Steps:
  1. wait until reminder time
  2. observe sound, vibration, fullscreen page, or notification behavior
- Expected:
  - reminder is triggered
  - no crash
  - expected reminder UI appears
- Record:
  - success: `Yes / No`
  - duplicate trigger: `Yes / No`
  - crash: `Yes / No`

#### Case R-03: Background unlocked reminder

- Preconditions:
  - create test todo with fullscreen reminder
  - return to launcher, keep device unlocked
- Steps:
  1. wait for trigger
- Expected:
  - reminder reaches user without needing manual app open
  - if fullscreen path fails, diagnostics should still show where it failed
- Record:
  - fullscreen arrived: `Yes / No`
  - fallback notification arrived: `Yes / No`
  - diagnostics useful: `Yes / No`

#### Case R-04: Locked-screen reminder

- Preconditions:
  - create test todo with fullscreen reminder
  - lock screen and wait
- Steps:
  1. wait for trigger while locked
  2. unlock device if needed
- Expected:
  - user receives reminder on lock or unlock-return path
  - no silent disappearance
- Record:
  - lockscreen visible: `Yes / No`
  - unlock-return visible: `Yes / No`
  - result:

#### Case R-05: Clear diagnostics

- Preconditions:
  - existing reminder chain logs available
- Steps:
  1. tap `清空诊断`
- Expected:
  - diagnostics list becomes empty immediately

### 8.2 Calendar Views and Template Workflow

#### Case C-01: Calendar view switching

- Steps:
  1. open calendar
  2. switch timeline -> week -> month -> list -> timeline
  3. repeat 3 times
- Expected:
  - no crash
  - no blank unresponsive state
- Record:
  - average smoothness score `1-5`:

#### Case C-02: Save current week as template

- Preconditions:
  - current visible week contains at least 2 events
- Steps:
  1. tap `保存本周模板`
  2. enter template name
  3. save
- Expected:
  - save succeeds
  - template appears in template manager

#### Case C-03: Apply template to target week

- Preconditions:
  - at least one saved template exists
- Steps:
  1. open template manager
  2. choose target week
  3. tap `复制到目标周`
- Expected:
  - target week receives copied events
  - no missing title/time/location fields

#### Case C-04: Generate semester recurring schedule

- Preconditions:
  - at least one saved weekly template exists
- Steps:
  1. open template manager
  2. choose `生成整学期`
  3. set first week start date
  4. set semester end date
  5. confirm
- Expected:
  - recurring weekly events are created
  - generated dates follow weekly pattern
  - no crash or duplicate immediate spam

#### Case C-05: Delete template

- Preconditions:
  - at least one saved template exists
- Steps:
  1. delete one template
- Expected:
  - template disappears from list
  - no unrelated data loss

### 8.3 Batch Import

#### Case I-01: Custom grammar happy path

- Input example:

```text
2026-04-27: 10:20-11:55, 辅导员助理值班, @MB-B1-412, Weekly, 2026-06-30, Remind=15m, Group=值班
```

- Expected:
  - parse preview succeeds
  - event created after import
  - group is mapped to `值班`

#### Case I-02: Clipboard paste

- Steps:
  1. copy valid import text to clipboard
  2. open batch import
  3. tap `粘贴剪贴板`
  4. parse and import
- Expected:
  - pasted content appears correctly
  - import succeeds

#### Case I-03: CSV import with group column

- Required columns:
  - `date,start,end,title,location,notes,allDay,color,remind,mode,ring,vibrate,recurrence,recurrenceEnd,group`
- Expected:
  - valid rows import
  - `group` is resolved or auto-created

#### Case I-04: TSV import with group column

- Expected:
  - same as CSV path

#### Case I-05: ICS import with categories

- Preconditions:
  - ICS contains `SUMMARY`, `DTSTART`, `DTEND`, and `CATEGORIES`
- Expected:
  - event is imported
  - `CATEGORIES` is used as best-effort group name

#### Case I-06: Invalid mixed input

- Preconditions:
  - at least one valid row and two invalid rows
- Expected:
  - no crash
  - invalid rows show readable error text

### 8.4 Recurring Editing and Preview

#### Case E-01: Todo recurrence preview

- Steps:
  1. create or edit a recurring todo
  2. tap `预览循环生成`
- Expected:
  - preview dialog appears
  - count and first several generated time points are readable

#### Case E-02: Calendar recurrence preview

- Steps:
  1. create or edit a recurring calendar event
  2. tap `预览循环生成`
- Expected:
  - preview dialog appears
  - start and end times are readable

#### Case E-03: Edit current occurrence only

- Preconditions:
  - recurring todo or recurring calendar event exists
- Steps:
  1. edit current occurrence only
  2. save changes
- Expected:
  - only current occurrence changes
  - whole series is not unexpectedly overwritten

#### Case E-04: Edit current and future

- Expected:
  - current and future items change
  - past items remain unchanged

#### Case E-05: Edit all occurrences

- Expected:
  - full series updates consistently

## 9. Session Summary Table

Fill this after each test session.

| Area | Total Cases | Pass | Partial | Fail | Blocked | Notes |
|---|---:|---:|---:|---:|---:|---|
| Reminder Reliability | 5 |  |  |  |  |  |
| Calendar Views / Templates | 5 |  |  |  |  |  |
| Batch Import | 6 |  |  |  |  |  |
| Recurring Editing | 5 |  |  |  |  |  |

## 10. Bug Record Template

Use this template for each discovered bug.

```text
Bug ID:
Area:
Device / ROM:
Preconditions:
Steps to reproduce:
Expected result:
Actual result:
Frequency: always / often / sometimes / once
Severity: blocker / major / minor / cosmetic
Related log or screenshot:
```

## 11. Recommended Test Order

To reduce confusion, test in this order:

1. environment and permission check
2. reminder chain test
3. foreground/background/lockscreen reminder runtime
4. calendar views
5. week-template save/apply/semester generation
6. batch import
7. recurring preview and scope editing

## 12. Release Readiness Suggestion

For a `1.5.0` candidate build, the minimum recommendation is:

- no crash in one full manual session
- reminder dispatch success rate `>= 90%` on the main test device
- no duplicate reminder bug
- week-template save/apply/semester generation all pass on at least one dataset
- custom grammar, CSV, TSV import all pass on at least one valid sample
- all major failures recorded with logs or screenshots
