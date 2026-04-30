# PaykiTodo 1.5.0 Architecture Design

## Design Principles

- Keep the product local-first.
- Prefer additive migrations over disruptive rewrites.
- Put reminder diagnostics on-device so the user can inspect failures directly on the phone.
- Reuse the existing Room + Repository + ViewModel + Compose stack.

## Main Additions

### Reminder Diagnostics Layer

- Add a persistent `ReminderChainLog` entity.
- Add a lightweight logger utility callable from scheduler, receivers, service, accessibility fallback, and reminder activity.
- Expose recent chain logs in settings.
- Add a dedicated reminder test entry that creates a near-term test reminder and logs the full path.

### Calendar Expansion Layer

- Keep the current timeline view as one calendar mode.
- Add week, month, and list modes in the calendar panel.
- Add reusable `ScheduleTemplate` storage for weekly and semester-oriented schedule generation.
- Add UI entry points for:
  - template-based semester generation
  - weekly template save/apply
  - multi-format import

### Bulk Import Layer

- Introduce a higher-level import hub above the current custom text parser.
- Keep the existing grammar parser as one strategy.
- Add format detectors and parsers for:
  - custom text
  - CSV or TSV
  - timetable text
  - ICS text
- Normalize all successful parses into `CalendarEventDraft` objects before persistence.

### Advanced Recurrence Editing Layer

- Preserve existing recurrence scopes: current, current and future, all.
- Add current-occurrence-only modification support for recurring items.
- Keep split-series behavior based on the existing current-and-future branch approach.
- Add preview generation before creating or replacing large recurring sets.

## UI Surfaces

- `SettingsPanel`
  - reminder diagnostics card
  - reminder chain test dialog
- `CalendarPanel`
  - view-mode switcher
  - import hub entry
  - template save/apply actions
- `CalendarBatchImportDialog`
  - clipboard paste
  - format detection
  - grammar help remains embedded

## Data Flow

1. User creates or imports schedule content.
2. Parsers normalize into drafts.
3. Repository validates and expands recurring rules.
4. Database stores task or event instances and templates.
5. Scheduler registers reminders.
6. Reminder diagnostics logger records chain events at each stage.
