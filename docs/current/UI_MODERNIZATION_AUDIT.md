# Phone UI Modernization Audit

## Audit Baseline

- Audited version: `1.15.4`, followed by the drawer implementation in `1.15.5`.
- Verification surface: Pixel_8 Android Emulator, 1080 x 2400, light and dark themes.
- Screens checked: Daily Board, Todo, Calendar, Planning Desk, Settings, navigation drawer, and theme selection.
- Product constraint: preserve the Daily Board landscape artwork, `payki_greeting`, and the Forest / Clay / Ocean / Sand / Night palette.

## Overall Finding

PaykiTodo does not mainly look old because of its colors. The larger problem is that different screens currently speak different UI languages:

- the Daily Board is a decorative personal dashboard;
- Todo is a large-heading card list with a crowded horizontal filter strip;
- Calendar resembles a compact desktop calendar with icon-only controls;
- Planning Desk resembles a large bordered form;
- Settings is the closest screen to a current mainstream Android hierarchy.

Do not copy QQ's account, membership, or social structure. Borrow the useful navigation principles instead: a clear top visual anchor, grouped high/low-frequency destinations, consistent row height and icon scale, obvious selection state, and fixed auxiliary actions.

## Implemented In 1.15.5

### Navigation drawer

- Replaced the plain app-name header with a rounded identity card containing the launcher art, product name, and one-line purpose.
- Grouped Board, Todo, Calendar, and Planning Desk under `主要功能`.
- Exposed AI Reports and History under `回顾与记录` instead of hiding them behind `更多`.
- Kept Settings and appearance controls fixed at the bottom so they do not move with the main navigation list.
- Unified menu row height, 16dp selection shape, 34dp icon containers, text weight, and light/dark selection colors.
- Replaced the floating dropdown theme menu with a Material bottom sheet containing descriptions and a selected checkmark.

## Implemented In 1.15.6

### Light color system

- Removed Sand beige from the global light background role.
- Operational page backgrounds now use cool white, cards use paper white, and subdued containers use soft gray-green.
- Calendar inherits the cool neutral work surface instead of filling the timeline with warm beige.
- Forest, Ocean, and Clay remain accent colors; warm colors are no longer large-area utility surfaces.

### Daily Board schedule hierarchy

- The upper schedule area is labeled `今日剩余` and shows the remaining-event count.
- The lower area is a full-width tinted `明日安排` band with date, weekday, and count.
- Tomorrow displays at most two rows, then exposes the remaining count and a Calendar route.
- Empty tomorrow state links to Planning Desk without introducing a nested card.

## Implemented In 1.15.7

### Light contrast system

- Increased the difference between the light blue-gray page layer, pure-white content surfaces, and gray-green control containers.
- Darkened `onSurfaceVariant` so subtitles, time-axis labels, empty states, and Settings summaries remain readable.
- Strengthened the Daily Board tomorrow band and count badge while retaining the landscape artwork.

### Calendar work surface

- Calendar toolbar and Calendar content are separate white surfaces with a visible page-background gap.
- Removed heavy panel outlines after emulator review; contrast now comes from layer luminance rather than borders.
- Strengthened toolbar button fills and compacted the timezone label to two centered lines.
- Added initial-scroll top inset so the first visible hour label is not pressed against the rounded content edge.

## Remaining UI Work

### P1 - Todo

1. The group filter strip can end with a partially clipped chip and does not clearly communicate horizontal scrolling.
2. Group management, filter selection, batch import, and add actions use several competing controls; the two bottom actions need clearer primary/secondary hierarchy.
3. Empty sections use large cards and headings, producing excessive vertical weight when there are few tasks.
4. Todo cards and section empty states should share one compact spacing/token system with the Daily Board rather than maintaining separate sizes.

### P1 - Calendar

1. The toolbar contains `+`, view mode, and overflow icon-only buttons whose purpose is not discoverable without trial and error.
2. Month selection, today action, day strip, timezone label, and timeline compete in a dense header without a clear primary control.
3. The top of the time grid can appear clipped after automatic scrolling; the first visible hour needs a stable inset.
4. Calendar should remain a solid operational surface, but its typography, icon containers, and action shapes should use the same global tokens as the drawer and Settings.

### P1 - Planning Desk

1. Three large actions in one row make the document title and recognition action compete for width.
2. The explanation block plus a very large outlined editor makes the page look like an old data-entry form rather than a lightweight memo.
3. Document selection, help, recognition, and secondary document operations need one obvious writing-first hierarchy.
4. Keep recognition preview-first behavior, but visually separate writing from import management.

### P2 - Shared chrome and Daily Board

1. The top app bar is visually heavy on all sections; title/icon scale should be reduced outside the Daily Board.
2. Utility screens currently reuse the decorative wallpaper while Calendar uses a solid surface. A later round should decide intentionally which screens are decorative and which are operational; the Daily Board artwork remains protected.
3. The onboarding card is useful once but dominates the empty board. Its dismissed state is correct; do not use it as the permanent layout baseline.
4. Establish shared dimensions for page margins, card radius, row height, icon containers, section titles, and action emphasis before broad visual edits.

### P2 - Settings

1. The basic structure is already the strongest phone screen and should not be rewritten.
2. Long subtitles should receive consistent two-line limits and spacing.
3. Common and advanced sections should keep their current separation; only card density and page background integration need later refinement.

## Recommended Order

1. Drawer and appearance picker - completed in `1.15.5`.
2. Todo filter/action hierarchy and empty-state density.
3. Calendar toolbar discoverability and timeline inset.
4. Planning Desk writing-first visual simplification.
5. Shared top-bar/card tokens and Settings polish.

Each round should be checked on the emulator in both light and dark themes before moving to the next page. Avoid a one-shot redesign of all screens.
