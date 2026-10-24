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

Daily Board artwork is product identity. Starting with 1.16.15, the user explicitly requests the same artwork behind Todo. Settings and AI configuration retain stable solid workspace surfaces with readable light/dark contrast.

- Avoid pure-black full-page slabs in dark mode; use the established deep blue-gray workspace layer plus distinct content surfaces.
- Keep 16dp-class section spacing, compact but readable row height, and clear title/summary/control hierarchy.
- Avoid stacking several outlined cards inside another outlined card. Prefer one section surface with light internal grouping and a single obvious primary action.
- Account identity occupies the drawer header; account settings open from that identity. Todo history belongs under Todo, not as an independent drawer destination.
- Calendar month/week changes use spatial pager transitions. Agenda rows use compact text and a content-height accent bar; the lower date rail does not duplicate the top strip's lunar label.
- Planning Desk keeps only document selection and recognition in the main toolbar. Document creation, rename and archive belong in the document list; help belongs in the top-right question-mark icon.

## 8. Desktop Web workstation layout

Desktop Web is a user-facing companion workspace, not an internal admin console.

- Prefer a light, low-height top navigation with the product identity on the left, primary work areas in the center, and compact connection state on the right.
- Keep the Calendar as the primary desktop scheduling surface: seven continuous days on wide screens, then three or one day on narrower screens instead of squeezing seven unreadable columns.
- Use flat white content surfaces, restrained borders, small radii, and blue as a limited action/selection accent. Avoid dark permanent sidebars, heavy gradients, large dashboard cards, and excessive nested panels.
- Secondary utilities such as Excel import should be collapsed by default so they do not compete with the current task.
- Desktop editor dialogs should be centered, keyboard-friendly, and ordered by user workflow rather than database field order. For events: title, time, location, reminder/check-in, notes, recurrence, group, color.
- Dark mode must redefine the complete surface/text/border variable set; do not let a light-only redesign fall back to mixed or unreadable colors.

## 9. Explicit reference images are binding

When the user supplies concrete UI reference screenshots and asks for reproduction, do not reinterpret the request as a vague styling preference.

- Match the visible geometry first: viewport scale, top-bar height, sidebar width, content boundaries, grid density, dialog position, dialog proportions, and internal split lines.
- Use browser screenshots at the same effective output size as the reference whenever possible. For the `1.15.14` Desktop Web Calendar references, verification used `1638x989` CSS pixels with `deviceScaleFactor = 1.25`, producing `2048x1236` output.
- Compare actual rendered screenshots, not only DOM/CSS source. Treat overflow, inherited widths/padding, browser auto-scroll, and hidden off-screen controls as blocking defects.
- Preserve PaykiTodo-specific capabilities and backend contracts, but express them inside the reference composition rather than restoring generic cards or admin-dashboard scaffolding.
- Do not claim reference-level completion after changing only palette, radius, or typography.

## 10. Native Windows consistency

- From the final 2026-10-22 annotated feedback, QingKeBenYueSong is reserved for greetings/daily quotes. The 2026-10-23 feedback supersedes the previous mandatory Sarasa default: native Windows defaults to Microsoft YaHei UI and offers installed fonts/Sarasa in Settings; Android defaults to system sans-serif and offers system, bundled and local TTF/OTF fonts. Preview changes remain drafts until completion. Drawer labels remain Semibold; phone body text is slightly larger. Windows product identity uses Century Gothic/Century with Segoe UI fallback. Preserve font licenses and verify bundled resources actually resolve.
- Native Windows Calendar follows the supplied calendar reference: compact toolbar, left mini month with selected-week background and today's circle, fixed weekday/date header, timezone label, continuous hour grid, and current-time label/red line. Columns before today use a dashed red line, today's left edge has a red dot, and today/future columns use a solid line. Blue is the calendar action/selection accent; do not turn calendar surfaces green.
- Keep the supplied board artwork and greeting font. All owned windows, reminders, confirmations and tray icons use the original PaykiTodo icon, not testhost/system question icons. Respect display scaling and check small-text legibility instead of applying decorative fonts indiscriminately.
- Keep dialog actions fixed outside scrollable forms. Planning Desk actions must remain inside the narrow-window viewport. Center reminder time/title/actions without centering ordinary form labels.
- TextBoxView already applies control padding: do not apply the same padding again as PART_ContentHost margin. Verify date/time and editable-model text visibility, not just saved values.
- Visual review is a supplement to save/cancel/navigation/state checks, not a substitute. Prefer isolated desktop controls for routine acceptance; do not steal the user's physical input to take screenshots.

## 11. Independent calendars and historical countdowns

- Calendar ownership is separate from task groups. Use named calendars and colored rounded checkboxes; hiding a calendar changes calendar visibility, not reminder scheduling. Preserve ownership in recurrence, templates, backups and edits.
- Windows blank-grid single click snaps to 15 minutes and previews a blue 30-minute draft; clicking that draft opens the editor. Cancel writes nothing. Keep scroll position when visibility or data refreshes.
- Native Windows event editing uses a compact form near the draft with a fixed blue completion action. Mini-month arrows browse independently of the main calendar; changing a selected date navigates the main view. Responsive week navigation advances by the actual visible day count.
- Recurring event edits, cancellation and deletion explicitly choose current, current-and-future or all. A one-occurrence move keeps its original series anchor. Drag/resize uses 15-minute snapping, previews the candidate and supports Esc cancellation; saving uses the same scope rules as form editing.
- Countdown AUTO retains the last7 local calendar days by default; KEEP survives completion and the cutoff, HIDE suppresses presentation. Ask recently completed/ended AUTO items whether to keep the memorial. Do not resurrect task active status to show a memorial.
- Settings uses compact categories: category navigation, title/summary/control rows and light dividers. Mobile backup emphasizes directory/export/restore and collapses maintenance tools.
