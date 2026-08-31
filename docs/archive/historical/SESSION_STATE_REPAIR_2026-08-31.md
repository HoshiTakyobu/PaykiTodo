# PaykiTodo Session State Repair - 2026-08-31

## Incident

The long-running PaykiTodo Codex thread repeatedly resumed with `1.15.11 / versionCode 353`, even though the repository, local APKs, Git tag, remote `main`, and GitHub Release were already at `1.15.18 / versionCode 360`.

Affected thread:

`019d4e16-8f61-78e3-9028-1dccf67f1661`

## Root Cause

Three non-backup rollout files shared the same thread UUID:

- `sessions/2026/06/16/rollout-2026-06-16T17-19-59-...jsonl`
- `sessions/2026/06/16/rollout-2026-06-16T17-32-52-...jsonl`
- `sessions/2026/04/02/rollout-2026-04-02T20-06-34-...jsonl`

The `threads.rollout_path` field in both local Codex state databases pointed to the `17-19-59` file. Its last `compacted` event was written on 2026-08-27 and described `1.15.11`, so the desktop client injected that stale summary on resume.

The `04/02` rollout contained the later release work. Its last `compacted` event described `1.15.18 / versionCode 360`, and its final JSONL event was a valid `task_complete` after the release and GitHub checks.

The legacy `session_index.jsonl` entry was also stale at 2026-05-16, and the stored thread working directory was only `G:\Workspace`, preventing reliable project-root bootstrap behavior.

## Repair Performed

Before modifying state, timestamped backups were created in both `C:\Users\hp\.codex-cpa` and `C:\Users\hp\.codex`:

- `state_5.pre-paykitodo-1.15.18-session-repair-20260831-184711.sqlite`
- `session_index.jsonl.pre-paykitodo-1.15.18-session-repair-20260831-184711.bak`
- `.codex-global-state.json.pre-paykitodo-1.15.18-session-repair-20260831-184711.bak`

The first repair attempt updated the thread record to:

- point `rollout_path` at the validated `04/02` rollout;
- use `G:\Workspace\Project\PaykiTodo` as its working directory;
- use the title `【开发】PaykiTodo`;
- record branch `main`, repository origin, and the current Git SHA;
- refresh the session index timestamp and workspace-root hint.

Because the Windows `sqlite3.exe` command-line path encoded direct Chinese SQL literals with the active console code page, the title and preview fields were rewritten from explicit UTF-8 byte literals and their stored hex values were verified.

The running Codex process subsequently restored `threads.rollout_path` to the active `17-19-59` rollout. Repointing the database was therefore not durable. The final repair:

- retained the active `17-19-59` rollout path;
- backed up that rollout as `rollout-2026-06-16T17-19-59-...pre-1.15.18-compaction-repair-20260831-1900.bak.jsonl` in both Codex homes;
- appended one structured `compacted` event marked `SESSION_BASELINE_REPAIR_V1_15_18_20260831`;
- recorded `1.15.18 / versionCode 360` as the current baseline and explicitly declared the older `1.15.11` compacted summary obsolete;
- kept the corrected repository working directory, title, branch, Git SHA, session index, and workspace hint.

No rollout file was deleted, renamed, concatenated, or replaced. The active rollout was modified only by appending the repair event after a byte-for-byte backup was created.

## Verification

At repair time:

- repository HEAD and `origin/main`: `83ff5cf`;
- exact tag: `v1.15.18`;
- app version: `1.15.18 / versionCode 360`;
- GitHub Release: `v1.15.18`;
- the active rollout's last compacted event contained the repair marker and `1.15.18 / versionCode 360`;
- that event explicitly marked the stale `1.15.11` baseline obsolete;
- both state databases pointed to the repaired active rollout;
- both session indexes and both workspace hints reflected the corrected thread metadata.

## Future Rule

Repository state remains authoritative. On every compacted resume, compare the inherited summary with Git, the Gradle version source, `docs/current/PROJECT_STATUS.md`, and `docs/current/SESSION_HANDOFF.md` before making changes.
