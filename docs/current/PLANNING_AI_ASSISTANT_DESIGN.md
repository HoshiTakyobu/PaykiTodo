# PaykiTodo 规划台 AI 识别设计

状态：`1.7.25` 已接入手机端和桌面 Web 规划台的真实网络调用；当前 `1.10.2` 继续沿用这条 AI 优先 / 本地规则回退链路，并保留 `1.8.5` 补充的单个 Provider“获取模型”“测试连接”和 Base URL endpoint 兼容修复。设置页保存多个 OpenAI-compatible Provider，规划台“识别”会在启用 AI 且配置完整时优先调用 AI，失败时回退本地规则；编辑、换行、自动保存和桌面导入不会隐式触发 AI。`1.9.9` 起进一步要求 AI 分组必须显式标记，且桌面端导入可以直接接收 AI 候选 ID；`1.9.11` 将 Provider 列表收口为摘要卡 + 更多菜单，避免窄屏配置页拥挤；`1.9.12` 进一步让完整 AI 源在添加、编辑、启用/停用、排序、删除后尽量立即保存，并在存在未保存或不完整配置时直接在页面内提示；`1.9.14` 将本地规则识别和 AI 失败回退解析放到后台线程，避免长规划文档阻塞 Compose 主线程；`1.9.15` 进一步让普通看板 / 任务页面不再携带完整规划文档列表，完整规划文档只在进入规划台时订阅；`1.9.16` 将规划公告候选状态持久化并建立索引，避免每日看板 / 小组件 / 桌面轻量看板为了公告扫描完整 Markdown 正文；`1.9.17` 将桌面 Web 普通数据刷新拆成轻量看板、完整待办列表和可见日程范围，规划台导入后也只补齐当前页面需要的数据；`1.9.18` 进一步让桌面待办管理列表分页 / 搜索；`1.9.19` 修复电脑同步服务读取 UTF-8 长请求体的稳定性风险，降低电脑端规划台保存中文长文档时影响 AI 预览 / 导入闭环的概率；`1.9.20` 未改变规划台 AI 的触发语义，仍保持显式触发、预览确认、本地规则兜底；`1.9.21` 到 `1.9.23` 也未改变 AI 链路，只分别修正手机日历页标题本地化、Android 小组件刷新 / 浅色可读性、地点 `@` 展示和电脑同步启动地址问题；`1.10.1` 要求 AI 输出把日程地点放入 `location` 字段、把循环规则放入 `recurrence` 字段；`1.10.2` 未改变 AI 识别语义，继续保持显式触发、预览确认、本地规则兜底。

## 目标

规划台的一阶段本地解析器适合稳定格式，例如：

```text
10:00-12:00 事件1
12:00-13:00 事件2
任务M ddl 15:00
```

但用户真实输入可能很随意，甚至混合自然语言、口语、缩写和不完整日期。AI 识别的目标是把这些文本转换为“候选待办 / 候选日程”，降低用户记忆语法的负担。

## 基本原则

1. 本地规则解析永远保留，作为离线兜底。
2. AI 识别只作为可选增强，不应成为规划台可用性的前置条件。
3. AI 不直接创建、修改或删除正式待办 / 日程。
4. AI 输出必须进入识别预览页，由用户确认后再导入。
5. API Key、Base URL、模型名不得硬编码进源码，也不得提交到 Git。
6. 多服务商并存：DeepSeek、Qwen、兼容 OpenAI Chat Completions 的第三方中转都应按同一配置结构处理。
7. 多 Provider 按设置页列表顺序作为 fallback 优先级，后续真实 AI 调用应先尝试第一个启用源，遇到认证失效、限流、服务端错误或网络错误再尝试下一个。

## 推荐流程

```text
用户写自然规划
  -> 如果启用 AI 且存在完整 Provider，则按设置页顺序调用 AI
  -> AI 不可用 / 返回异常 / 配置不完整时回到本地规则解析
  -> AI 返回 JSON 候选
  -> App 校验字段、时间、提醒、类型
  -> 进入统一识别预览页
  -> 用户确认导入
```

## Provider 配置字段

每个服务商至少需要：

- `name`：本地显示名。
- `baseUrl`：OpenAI 兼容接口地址，可以填写服务根地址、`https://example.com/v1`、完整 `https://example.com/v1/chat/completions`，或完整 `https://example.com/v1/models`。
- `apiKey`：用户自己的 Key。
- `model`：模型名，例如 `deepseek-v4-flash`、`qwen3.6`。
- `enabled`：是否启用。
- `id`：本机唯一标识，App 内部生成，用于编辑和保留本地 Key。

示例见：`docs/templates/planning_ai_providers.example.json`。

## AI 输出格式要求

AI 应只输出 JSON，不输出解释文本。建议结构：

```json
{
  "items": [
    {
      "type": "event",
      "lineNumber": 1,
      "sourceLine": "10:00-12:00 事件1",
      "title": "事件1",
      "notes": "",
      "groupName": "",
      "startAt": "2026-05-15T10:00:00",
      "endAt": "2026-05-15T12:00:00",
      "reminderOffsetsMinutes": [5],
      "createLinkedTodo": true,
      "message": "AI 识别结果，建议确认"
    },
    {
      "type": "todo",
      "lineNumber": 2,
      "sourceLine": "任务M ddl 15:00",
      "title": "任务M",
      "notes": "",
      "groupName": "",
      "dueAt": "2026-05-15T15:00:00",
      "reminderOffsetsMinutes": [5],
      "createLinkedTodo": false,
      "message": "AI 识别结果，建议确认"
    }
  ]
}
```

## 风险控制

- 网络失败：按启用 Provider 顺序尝试下一个；全部失败后自动回到本地规则识别，并在预览页显示回退提示。
- Chat endpoint 兼容：完整 `/chat/completions` URL 直接使用；完整 `/models` URL 会转换为同级 `/chat/completions`；`/v1` Base URL 追加 `/chat/completions`；服务根地址先尝试 `/v1/chat/completions`，再尝试 `/chat/completions`。
- Model endpoint 兼容：完整 `/models` URL 直接使用；完整 `/chat/completions` URL 会转换为同级 `/models`；`/v1` Base URL 追加 `/models`；服务根地址先尝试 `/v1/models`，再尝试 `/models`。
- 非 JSON 响应：如果中转或服务返回 HTML / 网页内容，提示检查 Base URL 是否应填写到 `/v1` 或完整 endpoint，不再显示原始 `<!doctype` JSON 转换错误。
- 可重试失败：401 / 403 / 429 / 500 / 502 / 503 / 504、连接超时、DNS 解析失败。
- 不可重试失败：400 请求格式错误、用户主动取消。
- JSON 解析失败：回到本地规则识别，不导入 AI 结果。
- 时间缺失：进入预览页标红，要求用户补齐。
- 晚于 DDL / 开始时间的提醒：沿用现有非法提醒校验。
- 多 Provider：列表顺序就是调用优先级，用户可以在设置页通过上移 / 下移调整。
- 分组保护：`groupName` 只在原文显式包含 `#group`、`分组：`、`项目：` 或 `课程：` 等标记时保留。AI 不能从普通标题中擅自截取分组；例如 `16:05-18:00 入党表格填写` 的标题应是 `入党表格填写`，分组为空。
- 桌面导入保护：桌面 Web 的导入请求可能携带 AI 候选 ID（例如 `ai-0`），后端必须直接按候选 JSON 转换并导入，不能只接受本地规则解析器生成的 `line-*` ID。

## 已实现拆分

1. 设置页具备多 Provider 管理入口。
2. `PlanningAiCaller.callWithFallback` 已接入规划台识别流程。
3. 规划台识别按钮现在是异步执行，网络请求期间显示 `识别中`。
4. `PlanningAiRecognizer` 负责内置 prompt、解析 AI JSON、转换为现有 `PlanningParsedCandidate`。
5. AI 和本地规则共用现有预览、编辑、导入闭环。
6. 手机端和桌面 Web 规划台现已共用 `PlanningRecognitionService` 识别入口，桌面预览摘要也会显示共享的回退消息。
7. `1.8.2` 起，Provider 测试连接使用 10s connect / 20s read timeout，并对 root Base URL、`/v1` Base URL 和完整 endpoint 做 endpoint 候选处理。
8. `1.8.5` 起，Provider 编辑弹窗支持只填 Base URL / API Key 后拉取 `/models`，成功后用下拉选择模型，失败时仍可手动填写模型名。
9. `1.9.9` 起，AI `groupName` 显式-only，避免把普通标题误拆成分组；桌面端导入可直接接收 AI 候选 ID，避免“识别 1 条、导入 0 条”。
10. `1.10.1` 起，AI 候选可以携带 `location`、`allDay`、`countdownEnabled` 和 `recurrence`；桌面和手机预览都应允许确认这些字段，导入时必须写入正式日程/待办字段，而不是把地点放进备注。
11. 可选后续：支持从本地 JSON 模板导入 Provider 配置。
