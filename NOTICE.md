# Open Source Notice

PaykiTodo is distributed under the MIT License. See [`LICENSE`](LICENSE).

This project also depends on third-party open-source components, including but not limited to:

- Android Gradle Plugin
- AndroidX Core, Activity, Lifecycle, Room, DocumentFile
- Jetpack Compose and Material / Material 3
- Kotlin and kotlinx.coroutines
- JUnit
- org.json
- Sarasa Gothic UI SC / 更纱黑体 UI SC 1.0.42, SIL Open Font License 1.1, by Renzhi Li and upstream contributors. Unmodified Regular and Semibold fonts are bundled for interface text in Android/native Windows/Desktop Web/Wiki. Source: https://github.com/be5invis/Sarasa-Gothic/releases/tag/v1.0.42 ; full license: `app/src/main/assets/fonts/OFL-Sarasa.txt` (also distributed beside the Windows EXE). The existing user-selected `payki_greeting.ttf` is used for greetings only.
- SheetJS `xlsx` browser bundle, Apache License 2.0, vendored for Desktop Web local Excel parsing at `app/src/main/assets/desktop-web/vendor/xlsx.full.min.js`; license text is kept at `app/src/main/assets/desktop-web/vendor/xlsx.LICENSE.txt`
- Marked 18.1.3, MIT License, by MarkedJS and Christopher Jeffrey, bundled for offline Markdown reports and HTML help at `app/src/main/assets/content-viewer/vendor/marked.js`; license text: `app/src/main/assets/content-viewer/vendor/marked.LICENSE`. Source: https://github.com/markedjs/marked/tree/v18.1.3
- DOMPurify 3.4.17, available under Apache License 2.0 or Mozilla Public License 2.0, by Cure53 and contributors, bundled to sanitize rendered reports at `app/src/main/assets/content-viewer/vendor/purify.js`; license text: `app/src/main/assets/content-viewer/vendor/dompurify.LICENSE`. Source: https://github.com/cure53/DOMPurify/tree/3.4.17 . The bundle also retains the MIT notice for its regenerator-runtime helper.

Those dependencies remain governed by their own upstream licenses. Before redistributing modified builds, verify the exact dependency tree and license metadata from Gradle.

Release signing keys, local API keys, private Base URLs, backups, logs, and generated APK/AAB artifacts are not part of the open-source distribution and must not be committed to the repository.
