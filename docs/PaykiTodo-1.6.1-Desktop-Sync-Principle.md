# PaykiTodo 1.6.1 电脑同步原理说明

## 一句话解释

PaykiTodo 当前的“电脑同步”并不是传统意义上的云同步，也不是单独再做了一个 Windows 客户端。

它的实现原理是：

- 手机端在本地启动一个轻量 HTTP 服务
- 这个服务把一个网页控制台直接提供给电脑浏览器访问
- 电脑在同一局域网下打开手机的局域网地址
- 电脑通过网页调用手机暴露出来的接口
- 手机收到请求后，直接改自己本地的 Room 数据库

所以从体验上看起来像是“电脑连上了手机，并且直接在改手机里的待办和日程”。

## 为什么会觉得“很神奇”

因为通常大家接触到的多端同步，都是这种路子：

1. 电脑端 App
2. 手机端 App
3. 云服务器
4. 两端分别和云通信

而 PaykiTodo 现在走的是更轻的一种：

1. 手机端自己当小服务器
2. 电脑浏览器直接连手机
3. 不经过公网云
4. 不需要单独安装桌面程序

也就是说，这更像是“手机开了一个只有局域网里能访问的小型管理后台”。

## 当前实现结构

### 1. 手机端服务层

核心代码在：

- [DesktopSyncService.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/sync/DesktopSyncService.kt)
- [DesktopSyncCoordinator.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt)
- [DesktopSyncServer.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/sync/DesktopSyncServer.kt)

作用分工：

- `DesktopSyncService`
  - 启动一个前台服务
  - 保证“电脑同步”开启时，Android 不容易把这套服务直接收掉
  - 显示“电脑端已连接/电脑同步已运行”的通知

- `DesktopSyncCoordinator`
  - 管理整个桌面同步模块的生命周期
  - 负责判断是否应该启动服务
  - 负责处理网页请求，并把请求转成真实的数据操作

- `DesktopSyncServer`
  - 是一个非常轻量的 HTTP 服务器
  - 监听固定端口 `18765`
  - 接收电脑浏览器发来的请求

### 2. 网页控制台层

核心代码在：

- [DesktopSyncWebAssets.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt)

它做了三件事：

- 返回 HTML 页面
- 返回 CSS 样式
- 返回前端 JS 逻辑

所以电脑上看到的那个“PaykiTodo Desktop Sync”页面，本质上不是电脑本地文件，而是手机端把网页内容直接发给浏览器。

### 3. 数据层

核心数据仍然是手机本地数据库：

- [TodoRepository.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/data/TodoRepository.kt)
- [AppDatabase.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/data/AppDatabase.kt)

也就是说：

- 电脑并没有维护自己的副本数据库
- 网页端并没有一份独立的数据文件
- 所有操作最后都落到手机这一份 Room 数据库上

这就是为什么你在电脑上新增一个日程，手机上会直接出现。

## 电脑和手机到底是怎么通信的

### 第一步：手机拿到局域网地址

同步协调器会枚举手机当前可用的 IPv4 地址。

例如：

- `192.168.114.235`

只要手机和电脑在同一个 Wi‑Fi 里，电脑通常就能访问这个地址。

### 第二步：手机监听端口

手机端 HTTP 服务监听：

- `18765`

所以完整地址就变成：

- `http://192.168.114.235:18765`

### 第三步：电脑浏览器直接访问

电脑浏览器打开这个地址之后：

- 请求先到手机端 `DesktopSyncServer`
- 手机返回 HTML/CSS/JS
- 浏览器渲染出网页控制台

### 第四步：网页调用手机接口

例如网页里点“创建日程”时，会发一个 HTTP 请求：

- `POST /api/events`

手机端收到后会：

1. 解析请求 JSON
2. 组装成 `CalendarEventDraft`
3. 调用 `TodoRepository.createCalendarEventFromDraft(...)`
4. 写入 Room
5. 如果有有效提醒，则重新调度提醒
6. 返回结果给网页

## 为什么不需要云服务器

因为当前设计目标是：

- 优先满足“电脑在旁边时，我懒得拿手机，也能快速录入”
- 不急着上真正的远程云同步
- 先把手机和电脑在同一局域网内的协同做顺

这个场景下，手机自己当服务端就够了。

优点：

- 实现轻
- 不需要买服务器
- 不需要账号系统
- 数据不必先上传云端
- 电脑端直接浏览器可用

缺点：

- 手机和电脑通常需要在同一局域网
- 手机必须开机、联网，并且这个同步服务处于运行状态
- 目前还不是“人在外面也能远程改家里手机数据”的方案

## 安全性是怎么做的

当前不是完全裸奔的 HTTP。

手机端设置页里会生成一个访问密钥：

- `desktopSyncToken`

网页请求时，需要带上：

- `X-Payki-Token`

或者等价鉴权头。

手机端会校验：

- 请求里的 token 是否和本机设置保存的 token 一致

不一致就拒绝。

这意味着：

- 别人即使知道了你手机 IP 和端口
- 如果没有密钥，也不能正常操作你的待办和日程

当然，这仍然是“局域网轻鉴权”方案，不是企业级安全方案，所以只建议在你信任的网络里开启。

## 为什么要做前台服务

Android 对后台常驻进程限制很重。

如果只是普通后台线程，系统可能很快就把它杀掉。那样电脑端网页刚连上，手机服务就没了。

所以这里做了：

- `DesktopSyncService` 前台服务
- 通知栏显示“电脑同步已运行”

目的是：

- 提高这套局域网服务的存活率
- 减少系统后台回收导致的“网页突然连不上”

## 为什么手机重启后还能恢复

核心入口在：

- [BootReceiver.kt](/G:/Workspace/Project/PaykiTodo/app/src/main/java/com/example/todoalarm/alarm/BootReceiver.kt)

系统开机、时区变化、时间变化时，应用会收到广播。

在这一步里，除了恢复提醒外，还会检查：

- 用户是否开启了电脑同步

如果开启，就重新启动 `DesktopSyncService`。

这就是为什么重启后理论上它还能恢复。

## 为什么网页端操作后，手机端提醒也会跟着生效

因为网页端并不是绕过业务层直接改数据库。

当前流程是：

1. 网页请求进来
2. `DesktopSyncCoordinator` 调用现有数据逻辑
3. 成功写入后，再走现有提醒调度逻辑
4. 必要时重新调 `AlarmManager`

也就是说，电脑端只是新增了一个“输入入口”，不是新增了一条完全独立的数据链。

这样做的好处是：

- 手机端和电脑端看到的是同一份数据
- 提醒逻辑仍然只维护一套
- 备份逻辑仍然只维护一套

## 当前已经实现到什么程度

当前电脑同步网页控制台已经支持：

- 查看待办
- 查看日程
- 新增普通待办
- 新增普通日程
- 配置提醒方式
- 配置提醒提前时间点
- 配置循环规则与循环截止日期
- 完成待办
- 取消待办
- 删除日程

当前还不算完整的地方：

- 还没有做成完整的“桌面版全功能管理台”
- 复杂编辑体验仍然偏轻量
- 分组选择、颜色选择、已有事项编辑等还可以继续补
- 还不是公网云同步

## 这套方案和未来真云同步的关系

当前这套局域网网页控制台，并不和未来的真云同步冲突。

以后如果要上真同步，通常会是：

1. 本地数据库继续保留
2. 电脑网页端/桌面端继续保留
3. 再增加一层远程同步后端

也就是说，现在这套方案更像是：

- 先把“近距离多端协作”做顺
- 再考虑“远距离跨网同步”

## 总结

你现在看到的“电脑能直接改手机里的待办和日程”，本质上是：

- 手机开了一个局域网 HTTP 小服务
- 电脑浏览器直接访问手机
- 网页控制台发请求给手机
- 手机收到请求后，直接修改本地数据库并重排提醒

所以它不是魔法，而是一种很实用的“手机自己当服务端”的轻量多端方案。
