# 📌 PaykiTodo

PaykiTodo 是一个面向 Android 14 的本地单机待办提醒应用原型。当前版本已经完成从创建待办、设置提醒到触发提醒页的基本链路，可在 Android Studio 中直接打开并运行。

## 🌱 学习背景

- 当前项目处于持续学习与迭代阶段。
- 项目维护者目前仍在学习 Android 原生开发，很多设计与实现会以“先跑通、再重构”为原则推进。
- 因此，仓库中会保留一些阶段性方案与后续重构计划。

## ✨ 当前能力

- 首页展示今日待办与其他未完成待办
- 新建待办时设置标题、备注、截止日期与提醒时间
- 到点后通过通知触发提醒，并进入提醒页
- 提醒方式支持铃声、震动、语音播报的组合
- 提醒页当前提供固定的延后提醒选项
- 开机后自动恢复未来提醒

## 🗂️ 目录结构

- `app/`：Android 应用模块
- `gradle/`：Gradle Wrapper 配置
- `TODO.md`：下一阶段待完成事项

## 🚀 本地运行

1. 使用 Android Studio 打开 `G:\Workspace\Project\PaykiTodo`
2. 在 `Gradle JDK` 中选择 Android Studio 自带的 `jbr_21` 或其他内置 JDK
3. 确认 Android SDK 34 已安装，并准备好模拟器或真机
4. 运行 `app` 模块

## 🧰 技术栈

- Kotlin
- Jetpack Compose
- Room
- AlarmManager
- BroadcastReceiver

## ⚠️ 当前限制

- Android 14 下，全屏提醒是否直接拉起会受到系统状态与权限限制影响
- 已完成待办尚未保留为可浏览的历史记录
- 权限入口尚未迁移到独立设置页
- DDL 目前仍为日期级别，不含时分

详细的后续工作见 `TODO.md`。
