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
- keep calendar reminders on the full-screen / accessibility fallback chain

Do not change work mode into a normal ringing mode unless the product requirement changes explicitly.

## 4. Git commit message style

Commit messages should be specific enough to reconstruct what changed without opening the diff.

Use a concise Chinese subject plus a multi-line Chinese body when the change affects behavior, UI, versioning, or docs. The body should mention the concrete behavior changes, not only the version number.

## 5. Protected visual identity

The phone daily board's light/dark landscape backgrounds, the `payki_greeting` font, and the established Forest / Clay / Ocean / Night accent identity are deliberate product assets selected by the user. Do not replace them with generic typography or an unrelated global palette merely to make the interface look more "modern".

Starting with `1.15.6`, Sand is not the global light-mode background. Light operational screens use cool white / paper white / soft gray-green surfaces; Clay and other warm colors are limited to small accent roles. Do not restore large warm-yellow or beige page backgrounds without a new explicit product decision.

Layout density, spacing, accessibility contrast, and component hierarchy may be improved around the protected assets. Replacing the assets themselves requires an explicit user request and a before/after visual review. New Material components must receive palette-compatible container, foreground, and outline colors so framework-default purple does not leak into the UI.
