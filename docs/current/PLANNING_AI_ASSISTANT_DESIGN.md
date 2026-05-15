# PaykiTodo 规划台 AI 识别设计

状态：`1.7.25` 已接入手机端和桌面 Web 规划台的真实网络调用；当前 `1.8.0` 继续沿用这条 AI 优先 / 本地规则回退链路。设置页保存多个 OpenAI-compatible Provider，规划台“识别”会在启用 AI 且配置完整时优先调用 AI，失败时回退本地规则。

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
- `baseUrl`：OpenAI 兼容接口地址，例如 `https://example.com/v1`。
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
- 可重试失败：401 / 403 / 429 / 500 / 502 / 503 / 504、连接超时、DNS 解析失败。
- 不可重试失败：400 请求格式错误、用户主动取消。
- JSON 解析失败：回到本地规则识别，不导入 AI 结果。
- 时间缺失：进入预览页标红，要求用户补齐。
- 晚于 DDL / 开始时间的提醒：沿用现有非法提醒校验。
- 多 Provider：列表顺序就是调用优先级，用户可以在设置页通过上移 / 下移调整。

## 已实现拆分

1. 设置页具备多 Provider 管理入口。
2. `PlanningAiCaller.callWithFallback` 已接入规划台识别流程。
3. 规划台识别按钮现在是异步执行，网络请求期间显示 `识别中`。
4. `PlanningAiRecognizer` 负责内置 prompt、解析 AI JSON、转换为现有 `PlanningParsedCandidate`。
5. AI 和本地规则共用现有预览、编辑、导入闭环。
6. 手机端和桌面 Web 规划台现已共用 `PlanningRecognitionService` 识别入口，桌面预览摘要也会显示共享的回退消息。
7. 可选后续：支持从本地 JSON 模板导入 Provider 配置。
