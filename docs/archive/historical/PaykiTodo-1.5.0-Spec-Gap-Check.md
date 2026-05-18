# PaykiTodo 1.5.0 Spec Gap Check

## Fully Implemented

- Reminder chain persistent diagnostics.
- Reminder chain test entry in settings.
- Recent reminder diagnostics display in settings.
- Multi-format import foundation: custom grammar, CSV, TSV, ICS, clipboard paste.
- Recurring item current-occurrence edit support.
- Calendar-side week-template save flow.
- Calendar-side template apply-to-week flow.
- Calendar-side semester recurring generator based on saved weekly templates.
- Batch-import group mapping via custom grammar, CSV / TSV, and ICS categories fallback.
- Recurring preview dialogs for todo and calendar creation/edit flows.

## Partially Implemented

### Calendar Multi-View

- Implemented as first-pass timeline/week/month/list switching.
- Gap: week/month/list are simplified browse views, not full interactive dense calendar layouts.

### Schedule Template and Semester Features

- Data model and core UI flows now exist via `ScheduleTemplate`.
- Gap: template manager is still lightweight and does not yet support row-level editing, rename-in-place, or template diff preview.
- Gap: semester generation is template-driven, not yet a full guided “课表向导” with dedicated semester semantics.

### Bulk Import Expansion

- Core multi-format paths exist.
- Gap: timetable-specific grammar is currently absorbed by custom or CSV-like paths, not yet exposed as a dedicated guided workflow.
- Gap: no explicit import-time group picker or group auto-complete UI yet; grouping is field-driven.

### Advanced Recurrence Editing

- Current occurrence editing now works.
- Preview UI now exists inside todo and calendar editors.
- Gap: explicit split-series visualization is not yet delivered.
- Gap: dedicated one-off skip or pause semantics are not yet distinct from edit/cancel behavior.

## Not Yet Delivered

- Deep schedule-template editing interface.
- Semester-level guided wizard UI.
- Dedicated one-off skip / pause rule model.
- Rich series preview screen with scope diff visualization.
- Dedicated recurring exception table/model.

## Recommended Next Implementation Order

1. Add schedule template save/apply UI and JSON payload schema.
2. Add one-off skip / pause actions and a dedicated exception model.
3. Add semester generator wizard for class schedules.
4. Refine week/month/list views into richer calendar interactions.
