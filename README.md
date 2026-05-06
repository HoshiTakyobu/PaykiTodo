# PaykiTodo

> 一个以“强提醒”和“本地可控”为核心的 Android 待办与日历应用。  
> 面向真的需要被提醒到、需要时间轴规划、需要本地数据掌控权的使用场景。

## 项目定位

PaykiTodo 当前仍然以 **Android 本地单机应用** 为主，但已经开始补齐更完整的个人生产力链路：

- 待办、DDL、全屏提醒、锁屏提醒
- 日历、课程表式时间轴、批量导入
- 循环任务、循环日程、分组与模板
- JSON 备份恢复
- 手机与电脑同局域网下的轻量同步控制台

它不是依赖账号体系的云平台，也不是只停留在普通通知层的轻提醒工具。当前更强调：

- 真正到点时尽量把提醒送到用户面前
- 尽量适配后台、锁屏、解锁等复杂场景
- 保持数据本地可导出、可恢复、可观察
- 让日历与待办形成同生态体验

## 当前版本

- 当前仓库版本：`1.6.0`
- `versionCode = 72`
- 平台目标：Android 14 / API 34
- 应用包名：`com.paykitodo.app`
- 数据形态：本地 Room 数据库 + SharedPreferences + 本地缓存

## 已实现能力

### 待办系统

- 新增、编辑、删除待办
- 支持标题、备注、分组、DDL、提醒时间
- 支持不设置 DDL 的普通待办
- 支持待办完成、取消、恢复
- 支持循环任务与循环范围编辑
- 首页按 `已错过 / 今日待办 / 计划中` 分区显示
- 支持分组筛选，不破坏首页三段式布局

### 日历系统

- 默认三日时间轴视图
- 补充单日、周、月、列表视图
- 支持普通日程、全天日程、跨天日程、循环日程
- 支持地点、备注、颜色、提醒方式、提醒提前量
- 支持时间轴点按快速创建日程
- 支持批量导入文本语法
- 支持周模板保存、套用和学期级生成

### 提醒能力

- `AlarmManager` 精确调度
- 通知栏提醒与全屏提醒双路径
- 锁屏、后台、解锁后回提醒页等场景的多轮兜底
- 辅助功能提醒链路
- 前台服务 + 活跃提醒会话维持
- 提醒链路诊断与短延迟测试入口
- 提醒音支持内置音与系统通知提示音

### 电脑同步

- 手机端内置局域网网页控制台
- 在 `设置 -> 电脑同步` 中可开启
- 手机与电脑同一局域网时，电脑浏览器可直接连接手机
- 当前网页端支持：
  - 查看待办和日程
  - 创建普通待办
  - 创建普通日程
  - 完成待办
  - 取消待办
  - 删除日程
- 操作直接写入手机端同一份 Room 数据库

### 数据与备份

- 手动导出 JSON
- 手动导入 JSON
- 指定目录自动备份
- 导入前自动备份当前快照

### 设置与排障

- 目录式设置页
- 权限聚合入口
- 提醒链路诊断记录
- 崩溃日志查看与复制
- 内置 Wiki 使用说明
- 关于页与版本信息

## 电脑同步怎么用

这是 1.6.0 新增的重点能力。

### 使用前提

- 手机已安装并运行 PaykiTodo 1.6.0
- 手机和电脑连接到同一个局域网，通常就是同一个 Wi‑Fi

### 开启步骤

1. 在手机中打开 `设置 -> 电脑同步`
2. 开启 `启用电脑同步`
3. 记下页面显示的 `访问密钥`
4. 记下页面显示的地址，例如 `http://192.168.1.23:18765`
5. 在电脑浏览器打开这个地址
6. 输入访问密钥，点击连接手机

### 说明

- 这不是独立 Windows 客户端，而是手机端提供的局域网网页控制台
- 优点是不需要再安装一个桌面程序
- 当前优先保证“同局域网直连”和“直接改手机真实数据”
- 这还不是公网云同步

## 手机上安装与测试

当前调试包输出在：

- [PaykiTodo-1.6.0-debug.apk](/G:/Workspace/Project/PaykiTodo/app/build/outputs/apk/debug/PaykiTodo-1.6.0-debug.apk)

首次安装后建议至少完成以下检查：

1. 打开 `设置 -> 提醒权限`，检查通知、精确闹钟、全屏提醒、免打扰穿透、忽略电池优化、辅助功能提醒。
2. 打开 `设置 -> 提醒链路诊断`，跑一次短延迟测试。
3. 新建一条带提醒的待办，分别测试前台、后台、锁屏场景。
4. 打开 `设置 -> 电脑同步`，测试电脑浏览器是否能连上手机。

## 本地开发

### 环境要求

- Android Studio
- Android SDK 34
- Android Studio 自带 `jbr`

### 运行方式

1. 用 Android Studio 打开项目根目录 `Project/PaykiTodo`
2. 确认 Gradle JDK 指向 Android Studio 自带的 `jbr`
3. 运行 `app` 模块

### 命令行构建

在项目根目录执行：

```powershell
./gradlew.bat assembleDebug
```

这个项目默认约定优先使用 Android Studio 自带的 `jbr`，不要切回系统里乱七八糟的 Java。

## 权限说明

为了尽量接近强提醒场景，当前项目会涉及：

- 通知权限
- 精确闹钟
- 全屏提醒
- 免打扰穿透
- 忽略电池优化
- 辅助功能提醒
- 前台服务
- 网络权限（用于局域网电脑同步控制台）

说明：不同厂商 ROM 对后台和锁屏行为限制不同，因此提醒表现仍会有机型差异，项目会继续迭代兼容。

## 仓库结构

- `app/`：Android 应用主模块
- `app/src/main/java/com/example/todoalarm/ui/`：界面层
- `app/src/main/java/com/example/todoalarm/data/`：数据层
- `app/src/main/java/com/example/todoalarm/alarm/`：提醒链路
- `app/src/main/java/com/example/todoalarm/sync/`：电脑同步控制台
- `app/src/main/assets/wiki/`：内置说明文档
- `docs/`：设计、需求、测试文档

## 相关文档

- [CHANGELOG.md](/G:/Workspace/Project/PaykiTodo/CHANGELOG.md)
- [TODO.md](/G:/Workspace/Project/PaykiTodo/TODO.md)
- [PaykiTodo-1.5.0-Architecture-Design.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.5.0-Architecture-Design.md)
- [PaykiTodo-1.5.0-Test-Plan.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.5.0-Test-Plan.md)
- [LOCAL_STUDY_PLAN.md](/G:/Workspace/Project/PaykiTodo/LOCAL_STUDY_PLAN.md)

## 版权

© Copyright Hoshi Takyobu, 2026-2026
