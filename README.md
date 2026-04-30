# PaykiTodo

> 🌄 一个仍在持续打磨中的 Android 本地待办提醒应用。  
> 面向“真的需要被提醒到”的使用场景，而不是只停留在普通通知层。

## ✨ 项目定位

PaykiTodo 是一个以 **本地单机** 为前提的 Android 待办应用，当前重心是：

- 把待办、DDL、提醒节奏放进同一条使用链路
- 在前台、后台、锁屏等不同场景下尽量把提醒做得更强
- 保持界面足够清爽，同时让重要信息一眼可见
- 让项目本身也能成为 Android 学习材料的一部分

当前仓库仍处在持续学习与持续迭代阶段。  
项目维护者目前仍在学习 Android 原生开发，因此这里会保留一部分“先跑通，再精修”的工程痕迹。

## 🧩 当前版本

- 当前仓库版本：1.4.9
- 平台目标：Android 14 / API 34
- 应用包名：com.paykitodo.app
- 数据形态：纯本地存储，不依赖云端账号体系

## 🚀 已实现能力

### 待办管理

- 新增、编辑、删除待办
- 支持标题、备注、DDL、提醒时间、任务分组
- 支持不设置 DDL 的普通待办
- 支持循环任务、取消任务、恢复任务
- 首页区分“已错过”“今日待办”和“计划中”
- 已完成事项会进入历史记录而不是直接消失

### 日历与日程

- 提供三日时间轴日历视图、全天栏和当前时间红线
- 支持普通日程、全天日程、跨天日程、循环日程
- 支持地点、备注、颜色、提醒提前量、提醒方式
- 支持文本语法批量导入课程表/日程，并带预览与帮助

### 提醒能力

- 提醒方式支持响铃、震动
- 日程支持“通知栏提醒 / 全屏界面提醒”两种送达方式
- 到点后进入完整提醒页，可直接完成、确认或延后
- 默认延后时长可在设置中调整
- 重启设备后会自动恢复未来提醒
- 已加入辅助功能提醒兜底链路，用于加强提醒页拉起
- 支持系统铃声或内置提醒音

### 数据与备份

- 支持 JSON 手动导出、导入
- 支持指定目录自动备份
- 支持导入前自动备份当前数据快照

### 界面体验

- 支持浅色 / 深色 / 跟随系统三种显示模式
- 主界面包含问候语、语录刷新、抽屉导航
- 抽屉菜单支持切换到我的任务、日历、历史记录、分组管理、设置、关于
- 启动页、主界面、提醒页都做了单独的视觉设计
- 已提供关于页、崩溃日志查看、权限设置聚合入口

### 工程结构

- 使用 Room 保存本地任务数据
- 使用 AlarmManager 调度提醒
- 使用 Jetpack Compose 构建界面
- 关键权限入口已统一整理到设置页

## 📱 手机上安装与使用

调试包默认输出在：

- pp/build/outputs/apk/debug/

首次在真机上使用，建议完成以下检查：

1. 安装 APK 并打开应用
2. 在设置页完成通知权限、精确闹钟、全屏提醒、免打扰穿透、忽略电池优化、辅助功能提醒等授权
3. 新建一个 1 分钟后的测试待办，分别测试前台、后台、锁屏场景

## 🛠️ 本地开发

### 环境要求

- Android Studio
- Android SDK 34
- Android Studio 自带 jbr / JDK 21

### 运行方式

1. 用 Android Studio 打开项目根目录 Project/PaykiTodo
2. 确认 Gradle JDK 指向 Android Studio 自带的 jbr
3. 确认已安装 Android 34 平台与需要的模拟器镜像
4. 运行 pp 模块

### 命令行构建

在项目根目录执行：

`powershell
./gradlew.bat assembleDebug
`

如果本机默认 JAVA_HOME 指向了过新的 JDK，建议切回 Android Studio 自带的 jbr 再构建。

## 🔐 权限说明

为了尽可能接近强提醒场景，当前项目会涉及这些系统能力：

- 通知权限
- 精确闹钟
- 全屏提醒
- 免打扰穿透
- 忽略电池优化
- 辅助功能提醒

说明：

- 不同厂商系统对后台拉起、锁屏展示、免打扰策略的限制并不一致
- 同一套代码在不同 ROM 上的提醒表现可能会有差异
- 当前项目已经针对这类差异做了多轮兜底，但仍然会继续迭代

## 🗂️ 仓库结构

- pp/：Android 应用主模块
- gradle/：Gradle Wrapper 与构建配置
- docs/：需求、功能清单、路线文档
- README.md：项目总览
- TODO.md：当前待完成与下一阶段计划
- CHANGELOG.md：版本更新记录
- LOCAL_STUDY_PLAN.md：围绕当前项目的学习计划

## 🧠 学习说明

这个仓库不是“学完 Android 再来做项目”的结果，而是“边做边学”的过程记录。

如果你也是零基础或刚入门，可以直接结合以下文件一起看：

- MainActivity.kt
- DashboardScreen.kt
- DashboardChrome.kt
- TodoViewModel.kt
- ReminderActivity.kt
- ReminderAccessibilityService.kt

配套学习说明见：

- [LOCAL_STUDY_PLAN.md](/G:/Workspace/Project/PaykiTodo/LOCAL_STUDY_PLAN.md)

## 🛣️ 接下来要继续做的事

- 继续优化提醒在不同 ROM 下的稳定性
- 继续降低默认延后时长滚轮的交互卡顿感
- 继续打磨提醒页与设置页体验
- 逐步整理代码结构，减少早期迭代留下的历史负担

详细待办见：

- [TODO.md](/G:/Workspace/Project/PaykiTodo/TODO.md)

完整能力清单与后续方向见：

- [PaykiTodo-1.4.9-Implemented-Features.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.4.9-Implemented-Features.md)
- [PaykiTodo-1.4.9-Future-Roadmap-Prediction.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.4.9-Future-Roadmap-Prediction.md)

版本记录见：

- [CHANGELOG.md](/G:/Workspace/Project/PaykiTodo/CHANGELOG.md)

## © Copyright

© Copyright Hoshi Takyobu, 2026-2026
