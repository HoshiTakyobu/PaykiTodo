# PaykiTodo Docs Index

This directory is intentionally split by document lifecycle.

## Current truth

- `docs/current/`

Use this folder as the live project baseline. New sessions should read `AGENTS.md`
first, then the required files under `docs/current/`.

Internal maintenance files that should not clutter the public repository root also live under `docs/current/`, including:

- `START_NEW_SESSION.txt`
- `PROJECT_BACKLOG.md`

Long obsolete backlog material is archived under `docs/archive/historical/`.

## Historical archive

- `docs/archive/historical/`

Older versioned requirement, design, status, and handoff documents live here.
They are kept for traceability, but they are not the current product baseline.
Git history still preserves their original paths in earlier commits.

## Goal prompts

- `docs/goals/`

Goal-mode prompt files are archived inputs for completed work rounds. They must
not contain API keys, signing material, tokens, private Base URLs, or other
secrets.

## Safe templates

- `docs/templates/`

Templates here must be safe to commit. Do not fill real passwords, API keys,
keystore paths containing private material, or signing secrets into files under
`docs/`.

For release signing, fill the ignored root-level `keystore.properties` file
instead. Use `keystore.properties.example` as the safe copy source.
