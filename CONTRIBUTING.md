# Contributing to PaykiTodo

PaykiTodo is primarily a personal Android app, but contributions and local forks are welcome under the MIT License.

## Development environment

- Android Studio
- Android SDK 34
- JDK 17
- Windows PowerShell is the primary maintained command-line environment for this repository.

## Build commands

Debug build:

```powershell
./gradlew.bat :app:assembleDebug
```

Release build:

```powershell
./gradlew.bat :app:assembleRelease
```

Release builds require local signing files. Do not commit them.

## Before committing

Run the relevant checks for your change:

```powershell
git diff --check
./gradlew.bat :app:assembleDebug
```

For desktop web JavaScript changes:

```powershell
node --check app/src/main/assets/desktop-web/app.js
```

For database / parser / behavior changes, run the relevant unit tests if available:

```powershell
./gradlew.bat :app:testDebugUnitTest
```

## Commit message style

Commit messages should be written in Chinese and describe product behavior, not only file names or process notes.

Recommended format:

```text
{模块 + 具体行为变化}

完成内容概要：
- {用户可感知的变化}
- {bug 修复前后差异}
- {如有版本变化，写：版本升级到 X.Y.Z / versionCode N}
- 验证：{执行过的关键命令}
```

Do not put version-bump tails in the subject, such as `并升级到 1.7.9`.

## Versioning

If an APK needs to be installed over a previous build, increase both:

- `paykiVersionName`
- `paykiVersionCode`

For code changes that affect behavior, update relevant docs in the same round:

- `CHANGELOG.md`
- `README.md` if public usage changed
- `docs/current/PROJECT_STATUS.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Database changes

If Room schema changes:

1. Increase the database version.
2. Add a migration.
3. Update schema export files.
4. Update backup / restore and desktop sync JSON paths if the field should be preserved.
5. Document the migration in `CHANGELOG.md`.

## Secret and privacy rules

Do not commit:

- keystores or signing passwords;
- API keys or tokens;
- private Base URLs;
- local backups;
- real crash logs with personal data;
- generated APK / AAB artifacts unless explicitly requested for release handling.

Before pushing, check:

```powershell
git status --short
git check-ignore -v keystore.properties release/PaykiTodo-release.jks
```

## Pull requests

A useful PR should include:

- what changed;
- why it changed;
- how it was tested;
- screenshots or screen recordings for UI changes, with private data removed;
- migration notes for database changes.
