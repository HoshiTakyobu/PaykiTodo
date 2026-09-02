# Android Release UI Audit 1.16.3

## Audit Scope

- Device: Android Emulator `emulator-5554`, 1080 x 2400, 420 dpi.
- Package: `com.paykitodo.app`.
- Build under audit: signed `1.16.3 Release`.
- Screens inspected: account login, daily board, navigation drawer, todo list,
  calendar, Planning Desk, settings, and appearance selector.
- Evidence is kept only in the ignored `.tmp/audit-1.16.3/` directory. It is not
  a product asset and must not be committed.

## Results

### Account Gate

- Login screen exposes account, password, user agreement, registration, password
  recovery, local migration export, and migration import.
- A successful login reached the daily board after the authentication refresh fix.
- Force-stopping and relaunching the app retained the valid session and opened the
  daily board instead of returning to login.
- The temporary local audit cloud service was reachable through `10.0.2.2:18766`.
  The production build must continue using HTTPS and the private configured cloud
  endpoint.

### Daily Board

- Today, remaining schedules, tomorrow's plan, first-use hint, and the `知道了`
  action are visible on the 1080 x 2400 viewport.
- Long text did not overlap the next section or the bottom navigation area.
- The first-use hint is intentionally dismissible and does not block the board.

### Navigation Drawer

- Primary destinations are grouped under `主要功能`; review destinations and
  settings are separated below.
- The drawer has a stable close/open affordance and does not clip the final
  appearance entry when scrolled.
- The current low-density visual system uses consistent icon tiles and selected
  state surfaces.

### Todos

- Group filter, today section, planned section, and bottom-right create/import
  actions are visible without overlap.
- Empty-state copy remains inside its surface and does not push the floating
  actions outside the viewport.

### Calendar

- The calendar renders the selected month, local timezone, date strip, timeline
  grid, hour labels, and current-time red line. It is not blank on an empty data
  set.
- The current-time marker is aligned to the corresponding hour grid coordinate.
- Date selection uses a circular selected-day surface rather than a stretched
  shape in the visible date strip.
- The empty calendar has a large unused lower area by design; it is a visual
  polish opportunity, not a functional defect.

### Planning Desk

- The memo-style editor, document selector, help action, and `识别计划` action
  are visible together.
- The editor accepts natural examples without requiring legacy hashtag syntax.
- The editor uses a large scrollable surface; the lower blank area is expected for
  memo entry and is not a clipping defect.

### Settings and Appearance

- Settings are presented as a scrollable list of grouped rows with icons,
  summaries, and navigation affordances.
- The appearance selector exposes system, light, and dark choices in a separate
  sheet.
- Lower settings continue below the viewport and are reachable by scrolling;
  no row was found to be permanently inaccessible.

## Fix Applied During Audit

The account authentication request completed successfully, but the screen did not
immediately observe the new authenticated state. The fix runs login and
registration on `Dispatchers.IO` and bumps the ViewModel authentication signal on
success. This removes the apparent login hang and makes the success transition
deterministic.

## Residual Risk

- The emulator cannot prove OEM-specific notification shade rendering, vibration,
  lock-screen full-screen behavior, launcher widget sizing, or battery-management
  behavior.
- The emulator's public network path is unavailable in this environment; the
  authenticated flow was validated against an isolated local service, while the
  public HTTPS health endpoint was independently checked from the host.
- The calendar and Planning Desk empty-state proportions can be refined in a later
  visual pass, but neither caused overflow, overlap, or loss of interaction in
  this audit.
