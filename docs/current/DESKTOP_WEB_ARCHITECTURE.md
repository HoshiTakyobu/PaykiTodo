# Desktop Web Architecture

## Current Model

PaykiTodo is still an Android-first local application. The desktop browser UI is not an independent cloud website and is not a separate desktop app.

The current model is:

```text
Android PaykiTodo APK
  ├─ phone UI and reminder logic
  ├─ local Room database
  ├─ LAN HTTP server for desktop sync
  ├─ /api/... endpoints that operate on phone-side data
  └─ assets/desktop-web/
       ├─ index.html
       ├─ app.css
       └─ app.js
```

When desktop sync is enabled, the phone serves the desktop Web UI to a browser on the same LAN. The browser then calls the phone-side API with the access key.

## Does The APK Contain The Desktop UI?

Yes. The APK contains the desktop Web UI files because the phone is the HTTP server that returns those files.

This is expected for the current architecture. It does not mean the phone UI will show desktop UI, and it should not materially affect phone startup. The files are static assets and are read when the desktop browser requests them.

## Why The Directory Was Split

Before `1.6.45`, the desktop HTML / CSS / JS lived inside `DesktopSyncWebAssets.kt` as large Kotlin raw strings. That worked functionally, but it was poor for maintenance:

- CSS and JS were hard to read and edit.
- JavaScript template strings needed Kotlin `$` escaping.
- Android sync service code and desktop UI code were mixed in one Kotlin file.
- Small Web UI changes produced noisy Kotlin diffs.

Starting from `1.6.45`, the desktop Web files live under:

```text
app/src/main/assets/desktop-web/index.html
app/src/main/assets/desktop-web/app.css
app/src/main/assets/desktop-web/app.js
```

`DesktopSyncWebAssets.kt` is now only a small asset loader with a minimal fallback page.

## Recommended Future Direction

Keep this model for now:

```text
phone app owns data and APIs
desktop browser is a LAN helper UI served by the phone
desktop Web assets are packaged into the APK
```

This keeps the desktop UI version aligned with the phone-side API and avoids cloud accounts, external hosting, CORS complexity, or version mismatch.

If the desktop UI becomes much larger, the next step should be a separate source directory or front-end build step, for example:

```text
desktop-web/
  src/
  dist/

app/src/main/assets/desktop-web/   # generated or copied dist output
```

Do not move to an independently hosted Web app unless there is a clear product reason. That would add networking, security, discovery, and versioning complexity.

## Maintenance Rule

- API / LAN server logic belongs in `app/src/main/java/com/example/todoalarm/sync/`.
- Desktop browser UI belongs in `app/src/main/assets/desktop-web/`.
- If Web UI behavior changes, update `CHANGELOG.md`, `TODO.md`, and the current docs in the same round.
- If API contracts change, document both the Android-side handler and the Web-side caller.
