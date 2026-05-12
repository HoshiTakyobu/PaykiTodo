# UI Design Rules

This document records PaykiTodo-specific UI rules that should be followed by future coding sessions.

## 1. Do not use button groups for ordinary options

Avoid presenting mutually exclusive choices as several large buttons laid out side by side or in a wrapped grid. This pattern wastes vertical space, looks heavy on phone screens, and makes Settings pages visually noisy.

Bad examples:

- playback channel as four large buttons
- week start as two large buttons
- reminder delivery mode as two large buttons
- any ordinary enum-like setting rendered as a pile of pill buttons

Preferred patterns:

- single-choice dropdown row for compact enum settings
- list dialog / bottom sheet with checkmarks when the options need descriptions
- multi-select checklist when multiple items can stay selected
- switch only for a true on/off state
- slider + numeric input for 0-100 percentage values

## 2. Settings page structure

Settings should separate common use from maintenance use.

Common settings should include frequent user actions such as reminder permissions, reminder sound strategy, system tone selection, calendar defaults, usage guide, desktop sync, and about information.

Advanced settings should be reserved for diagnostic or maintenance surfaces such as reminder-chain diagnostics, backup/import/export, and crash logs.

## 3. Reminder sound strategy wording

Work mode is a quiet strong-reminder mode. Its intent is to avoid outward sound in classroom / work scenarios while still making reminders harder to miss:

- suppress outward reminder sound by default
- strengthen vibration
- keep calendar reminders on the full-screen / accessibility fallback chain

Do not change work mode into a normal ringing mode unless the product requirement changes explicitly.

## 4. Git commit message style

Commit messages should be specific enough to reconstruct what changed without opening the diff.

Use a concise Chinese subject plus a multi-line Chinese body when the change affects behavior, UI, versioning, or docs. The body should mention the concrete behavior changes, not only the version number.
