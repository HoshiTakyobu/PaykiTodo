# Project Intent

## What PaykiTodo Is

PaykiTodo is an Android productivity app centered on strong reminders, local control, and timeline-aware planning.

It is not intended to be a cloud-first SaaS product. The current product philosophy is:

- make reminders actually reach the user at the needed time
- combine tasks and calendar planning in one coherent experience
- keep user data local, observable, exportable, and recoverable
- iterate toward a personal productivity tool that feels practical rather than generic

## Core User Problem

The project exists to solve a concrete pain point: ordinary to-do apps and calendar reminders are often too weak, too easy to ignore, or too fragmented.

The target experience is:

- tasks and schedule items live in one system
- reminders can escalate beyond ordinary notification banners
- the user can still control and inspect their own data locally
- after the first account login, the app remains usable with its local Room copy even when the cloud backend is unavailable; the cloud service is a continuity layer, never the sole local data store

The current Android build uses a normal PaykiTodo account as an access gate. A user can register or log in from the app, and can export or import a local `.paykibak` migration package before login so that Debug-to-Release signature migration does not depend on the cloud account path.

## Product Direction

Current long-term direction includes:

1. Strong reminder reliability across foreground, background, lock-screen, and wake-back scenarios
2. Better daily planning UX across tasks, day view, and calendar timeline
3. Better local backup / restore / diagnostics / explainability
4. Lightweight desktop-side assistance through LAN browser control first, followed by an optional full Windows client with a local copy of the same account data
5. Optional account-backed multi-device sync that preserves local-first operation and remains usable when the server expires

## Computer Client Boundary

正式 Windows EXE 与手机的“连接电脑”功能不是同一个服务：

- 正式 Windows EXE 是独立客户端，拥有自己的本地数据副本，通过账号登录服务器与手机同步，数据流为“手机本地数据 ↔ 服务器 ↔ 电脑本地数据”。它不依赖手机的局域网 Web 服务，也不连接手机 `18765`。
- “连接电脑”是保留的局域网兼容入口。电脑浏览器通过手机 `18765` 直接操作手机数据库，手机完成本地写入后再同步服务器。这条路径没有电脑本地数据副本。

任何后续设计都不得把手机 `18765` 当成 Windows EXE 的云同步接口，也不得用当前 WebView2 兼容壳冒充正式桌面客户端。

## Non-Goals For Now

These are not current primary goals:

- account-backed multi-device cloud sync as an unimplemented future phase
- team collaboration
- complex account and administrator systems before the local / LAN product is stable
- web-first productization
- feature breadth for its own sake

## Source Of Truth Rule

This file is the stable description of why the project exists.
Versioned design docs under `docs/` may still be useful historically, but this file is the current high-level baseline.
