# PaykiTodo

[![Latest release](https://img.shields.io/github/v/release/HoshiTakyobu/PaykiTodo?label=release)](https://github.com/HoshiTakyobu/PaykiTodo/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android-green.svg)](app)
[![CI](https://github.com/HoshiTakyobu/PaykiTodo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/HoshiTakyobu/PaykiTodo/actions/workflows/android-ci.yml)

PaykiTodo 是一个以 **强提醒、本地可控、日程与待办一体化** 为核心的 Android 个人任务管理应用。

它面向这样的使用场景：

- 不希望重要 DDL 被普通通知淹没；
- 需要把待办、日程、倒数日和规划草稿放在同一套本地数据里；
- 希望在手机上使用，同时能通过同局域网电脑网页端做较高效的编辑；
- 希望数据尽量留在本机，并能自行备份、恢复和导出。

> 当前项目仍是个人维护型应用。提醒可靠性会受到 Android 版本、厂商 ROM、电池策略、通知权限、精确闹钟权限等因素影响；不要把它当作医疗、安全生产或法律合规场景的唯一提醒来源。

## 当前版本

| 项目 | 内容 |
| --- | --- |
| 当前源码版本 | `v1.14.0` |
| 最新 GitHub Release | `v1.13.11` |
| Android 包名 | `com.paykitodo.app` |
| 最低系统 | Android 8.0 / API 26 |
| 目标系统 | Android 14 / API 34 |
| 开源协议 | MIT License |
| 最新 Release 页面 | <https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11> |
| 最新 Release APK 下载 | <https://github.com/HoshiTakyobu/PaykiTodo/releases/download/v1.13.11/PaykiTodo-1.13.11-release.apk> |

## 功能概览

### 1. 每日看板

- 聚合显示今日待办、今日日程、明日日程和关键倒数目标。
- 已结束日程会从今日日程计数中排除，正在进行的日程会突出显示。
- 无 DDL 的活动待办默认作为今日待办处理，适合“今天想做但还没定具体时间”的事项。
- 支持从看板进入待办、日历、规划台等具体页面。

### 2. 待办与 DDL

- 支持标题、备注、多分组、DDL、提醒方式、多提醒时间、循环任务和倒数日标记。
- 待办可选择全屏提醒或通知栏提醒。
- 待办详情预览中“取消待办”和“删除”保持分离：取消会进入历史记录，删除是硬删除；循环待办取消时会先选择“当前 / 当前及之后 / 全部”范围。
- 待办详情和编辑器负责取消、修改 DDL、循环范围等复杂处理；全屏提醒页只保留完成和固定延后。
- 支持批量添加待办，以及自然日期/时间输入，例如 `16:30`、`明天 16:30`、`5.28 23:59`。

### 3. 日历与日程

- 支持普通日程、全天日程、循环日程、地点、备注、颜色、提醒和倒数日标记。
- 支持单日、三日、月视图和列表等日历浏览方式。
- 时间轴可点击空白区域快速创建日程。
- 持续多天的日程、进行中日程和普通日程使用不同视觉处理。
- 日程详情预览支持“取消日程”和“删除”：取消会进入历史记录，删除是硬删除；循环日程取消/删除前会先选择作用范围。

### 4. 强提醒能力

- 使用 `AlarmManager` 做提醒调度。
- 支持通知栏提醒、全屏提醒、锁屏唤醒、响铃、震动和工作模式。
- 提供提醒链路诊断与短延迟全屏测试入口，会显示通知、精确闹钟、全屏权限、电池优化和辅助功能兜底状态。
- 支持内置提示音与系统通知提示音选择。
- 全屏待办提醒页保持极简，只提供“我已完成”和“延后 10 分钟”；取消待办、修改 DDL 等复杂操作回到待办详情/编辑器处理。
- 支持重要 DDL 的闹钟模式：提醒可持续响铃/振动，未处理后降级为通知并进行有限重试。

### 5. 规划台

规划台是 PaykiTodo 的“上游备忘录”，用于先自由写计划，再把明确事项识别为正式待办或日程。

- 支持多篇规划文档。
- 手机端和电脑端都以大文本备忘录为主入口，不再要求学习井号标签、大纲节点或 Markdown 语法。
- 支持自然 DDL、轻量日程、地点、无 DDL 待办和缩进子任务识别。
- 支持从分享、拍照、语音和图片识别入口快速捕获内容。
- 识别结果先进入预览页，用户勾选和修正后才会导入正式待办或日程。
- 已导入内容通过内部映射记录去重，不再往用户原文里追加可见井号标记。

### 6. 可选 AI 识别

- 支持配置多个 OpenAI-compatible AI 源。
- AI Key 和 Base URL 只保存在本机设置中，备份 JSON 不导出这些敏感字段。
- 规划台识别会优先调用启用的 AI 源；失败、超时或未配置时自动回退到本地规则解析器。
- AI 识别结果进入预览页，用户确认后才会导入。
- 电脑端 Web 规划台与手机端共用同一套 AI 优先 / 本地兜底链路。

### 7. 电脑同步 / 局域网网页端

- 手机端可开启同局域网网页控制台。
- 电脑浏览器访问手机提供的本地地址后，可以查看每日看板、编辑待办、编辑日程、使用规划台。
- 网页端操作直接写入手机本机 Room 数据库。
- 同步功能没有云账号体系，不经过项目作者服务器。

### 8. Android 桌面小组件

- 今日看板 Widget：显示公告、问候、今日待办、今日日程和明日日程摘要。
- 倒数日 Widget：独立展示最近倒数目标，按分钟刷新。
- 小组件支持浅色 / 深色模式，并使用独立的暖白 / 深灰背景和卡片视觉。
- 点击待办区域进入待办，点击日程区域进入日历或对应详情。

### 9. 数据、备份与排障

- 本地 Room 数据库。
- JSON 备份与恢复。
- 崩溃日志查看与复制。
- 内置 Wiki / 使用说明。
- 数据健康检查与安全清理。

## 安装说明

### 从 GitHub Release 安装

1. 打开最新 Release：<https://github.com/HoshiTakyobu/PaykiTodo/releases/latest>
2. 下载 `PaykiTodo-*-release.apk`。
3. 在 Android 手机上允许当前浏览器或文件管理器“安装未知来源应用”。
4. 安装 APK。

注意：

- 如果手机上已经安装的是 debug 签名版本，可能无法直接覆盖安装 release 签名版本。
- debug 与 release 签名不同。要从 debug 切到 release，通常需要先在应用内导出备份，再卸载 debug 版，然后安装 release 版并导入备份。
- 卸载应用会删除本地数据；操作前先备份。

## 本地构建

### 环境要求

- Android Studio
- Android SDK 34
- JDK 17（推荐使用 Android Studio 自带 JBR）

### Debug 构建

```powershell
./gradlew.bat :app:assembleDebug
```

输出位置：

```text
app/build/outputs/apk/debug/
```

### Release 构建

Release 签名材料不属于仓库内容，必须只保存在本地：

```text
keystore.properties
release/PaykiTodo-release.jks
```

这两个路径已经在 `.gitignore` 中忽略。不要把真实密码、API Key、Token、私有 Base URL 或 keystore 提交到 Git。

可参考安全模板：

```text
keystore.properties.example
docs/templates/
```

构建命令：

```powershell
./gradlew.bat :app:assembleRelease
```

## 隐私与数据边界

PaykiTodo 当前是本地优先应用：

- 不要求账号登录。
- 默认不上传待办、日程、规划文档或备份数据到项目作者服务器。
- 电脑同步只在用户主动开启后，通过局域网访问手机端服务。
- AI 功能是可选项；只有用户配置并启用 AI 源时，规划台识别 / AI 报告相关文本才会发送到对应第三方模型服务。
- API Key、Base URL 等敏感配置只应保存在本机，不应进入 Git、截图、日志或公开 Issue。

详见：[`PRIVACY.md`](PRIVACY.md)。

## 开源许可

PaykiTodo 以 MIT License 开源。你可以在遵守许可证条款的前提下使用、复制、修改和分发本项目源码。

详见：

- [`LICENSE`](LICENSE)
- [`NOTICE.md`](NOTICE.md)

第三方依赖仍遵循其各自许可证。

## 贡献与安全

- 贡献说明：[`CONTRIBUTING.md`](CONTRIBUTING.md)
- 安全说明：[`SECURITY.md`](SECURITY.md)
- 支持说明：[`SUPPORT.md`](SUPPORT.md)
- 行为规范：[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)
- 版本记录：[`CHANGELOG.md`](CHANGELOG.md)

提交 Issue 或 Pull Request 时，请不要包含：

- API Key / Token
- keystore 或签名密码
- 私有 Base URL
- 个人日程、课程、电话号码、地址等隐私数据
- 未脱敏的日志和截图

## 文档入口

| 文件 | 用途 |
| --- | --- |
| `README.md` | GitHub 首页与公开项目说明 |
| `CHANGELOG.md` | 面向用户的版本变化记录 |
| `LICENSE` | 开源许可证 |
| `NOTICE.md` | 开源与第三方依赖声明 |
| `PRIVACY.md` | 隐私与数据处理说明 |
| `SECURITY.md` | 安全报告与敏感信息处理说明 |
| `SUPPORT.md` | 提问、报错和支持边界 |
| `CODE_OF_CONDUCT.md` | 社区讨论行为规范 |
| `CONTRIBUTING.md` | 本地开发、提交和发布约束 |
| `docs/current/` | 当前维护状态、设计意图和会话交接文档 |
| `docs/archive/` | 历史文档归档 |

## 当前维护状态

PaykiTodo 仍处于快速迭代阶段。功能覆盖已经较广，但 UI 细节、厂商 ROM 提醒稳定性、桌面端功能一致性和文档仍在持续打磨。

如果你只是想安装使用，请优先下载最新 GitHub Release。
如果你要参与开发，请先阅读 `AGENTS.md`、`CONTRIBUTING.md` 和 `docs/current/` 下的当前状态文档；内部维护 backlog 位于 `docs/current/PROJECT_BACKLOG.md`。
