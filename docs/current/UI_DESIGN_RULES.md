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
- force the stronger vibration pattern, even if the individual item disabled vibration
- respect the item's selected full-screen / notification channels; work mode must not upgrade a notification-only item into a full-screen reminder

Do not change work mode into a normal ringing mode unless the product requirement changes explicitly.

## 4. Git commit message style

Commit messages should be specific enough to reconstruct what changed without opening the diff.

Use a concise Chinese subject plus a multi-line Chinese body when the change affects behavior, UI, versioning, or docs. The body should mention the concrete behavior changes, not only the version number.

## 5. Protected visual identity

The phone daily board's light/dark landscape backgrounds, the `payki_greeting` font, and the established Forest / Clay / Ocean / Night accent identity are deliberate product assets selected by the user. Do not replace them with generic typography or an unrelated global palette merely to make the interface look more "modern".

Starting with `1.15.7`, Sand is not the global light-mode background. Light operational screens use a visible light blue-gray page layer, pure-white content surfaces, and soft gray-green controls; secondary text must remain dark enough to read without relying on display brightness. Clay and other warm colors are limited to small accent roles. Do not restore large warm-yellow/beige backgrounds or collapse the light palette back into several nearly identical whites without a new explicit product decision.

Layout density, spacing, accessibility contrast, and component hierarchy may be improved around the protected assets. Replacing the assets themselves requires an explicit user request and a before/after visual review. New Material components must receive palette-compatible container, foreground, and outline colors so framework-default purple does not leak into the UI.

## 6. Editing is transactional

Creation and editing surfaces should treat user input as a local draft until the user explicitly presses the page or sheet's primary `完成` action.

- Back, top-left cancel, backdrop dismissal, or sheet swipe should discard the draft without writing data.
- Do not show a second `放弃本次编辑` confirmation for ordinary form drafts; returning should behave as though the edit never happened.
- Do not add separate `保存配置` / `保存设置` buttons inside a page that already has a top-right `完成` action. One completion action should commit the whole page draft.
- Nested editors may use `完成` to update the parent page draft, but that nested action must not bypass the parent page transaction.
- Destructive actions, permission requests, backup/export actions, and semantically named operations such as `保存本周模板` are not ordinary form commits and may keep their own explicit confirmation/action wording.

## 7. Operational screen backgrounds

Daily Board artwork is product identity, not a universal application background. High-density operational screens such as Todo, Settings, and AI configuration should use stable solid workspace surfaces with readable light/dark contrast.

- Avoid pure-black full-page slabs in dark mode; use the established deep blue-gray workspace layer plus distinct content surfaces.
- Keep 16dp-class section spacing, compact but readable row height, and clear title/summary/control hierarchy.
- Avoid stacking several outlined cards inside another outlined card. Prefer one section surface with light internal grouping and a single obvious primary action.
