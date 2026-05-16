# Goal Prompt Archive

This directory stores historical goal-mode prompts and large development objective files for PaykiTodo.

Use this directory for files like `goal.md` that are meant to drive one substantial coding round.

Rules:

- Do not place these files under `app/build/outputs/` or any other build output directory; those locations are temporary and may be deleted by builds.
- Prefer descriptive names such as `2026-05-16-paykitodo-1.8.1-goal.md` instead of many unrelated `goal.md` files.
- Commit a goal prompt only when it is useful project history and contains no API keys, tokens, private Base URLs, signing material, or personal secrets.
- If a prompt contains secrets or temporary local-only instructions, keep it outside git or redact it before committing.
- Current project truth still belongs in `docs/current/`; goal prompts are historical inputs, not the live status source.
