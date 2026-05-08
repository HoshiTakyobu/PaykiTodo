# TODO

## Current Iteration

### In Progress

- Verify the current launcher and notification icon surfaces on real Android UI instead of regenerating icon assets unnecessarily
- Keep `README.md`, `CHANGELOG.md`, and current-state docs aligned with version `1.6.10`
- Continue board / dashboard and calendar polish without regressing the current interaction model
- Improve repo-native handoff so new sessions do not depend on long chat history

### Needs Verification

- The current icon resource chain should be verified on actual device surfaces:
  - install prompt icon
  - launcher icon
  - monochrome themed icon
  - notification bar small icon
- The current time label behavior in calendar views should be verified against the user's intended behavior
- The current board / dashboard readability should be checked in both light and dark themes
- The final choice for `launcher_background` should be confirmed after visual inspection

## Mid-Term Follow-Ups

- Continue improving reminder reliability across foreground / background / lock-screen scenarios
- Expand the LAN desktop sync console with richer operations and better field coverage
- Clean up release signing and release build workflow for future stable publishing
- Keep historical docs usable while making current docs the default entry point

## Documentation Discipline

- Update `docs/current/*` whenever active direction or known status changes
- Keep `CHANGELOG.md` focused on released or release-like milestones
- Use `docs/current/SESSION_HANDOFF.md` to describe the current narrow takeover state

## Done Recently

- Switched the adaptive launcher icon chain to the current PaykiTodo mark resources
- Switched reminder and desktop-sync notifications to `ic_stat_payki_todo`
- Added the daily board default entry and the today / tomorrow schedule summary block
- Split dashboard background art into light and dark resources and refreshed launch / drawer visuals
- Added the release-signing template document under `docs/`
- Added repo-native new-session bootstrap docs under `docs/current/`
- Added repository-level `AGENTS.md` so new sessions know what to read first
- Preserved historical docs while marking them as non-baseline through current entry files
