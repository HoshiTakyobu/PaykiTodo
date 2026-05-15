# PaykiTodo 规划台 AI 识别设计

状态：设计落档，尚未接入真实网络调用。

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

## 推荐流程

```text
用户写自然规划
  -> 本地规则解析
  -> 如果用户选择 AI 识别，则把原文发送给选中的 AI Provider
  -> AI 返回 JSON 候选
  -> App 校验字段、时间、提醒、类型
  -> 进入统一识别预览页
  -> 用户确认导入
```

## Provider 配置字段

每个服务商至少需要：

- `name`：本地显示名。
- `base_url`：OpenAI 兼容接口地址，例如 `https://example.com/v1`。
- `api_key`：用户自己的 Key。
- `model`：模型名，例如 `deepseek-v4-flash`、`qwen3.6`。
- `enabled`：是否启用。

示例见：`docs/templates/planning_ai_providers.example.json`。

## AI 输出格式要求

AI 应只输出 JSON，不输出解释文本。建议结构：

```json
{
  "items": [
    {
      "type": "event",
      "title": "事件1",
      "start": "2026-05-15T10:00:00",
      "end": "2026-05-15T12:00:00",
      "location": "",
      "reminders": ["PT5M"],
      "source": "10:00-12:00 事件1"
    },
    {
      "type": "todo",
      "title": "任务M",
      "deadline": "2026-05-15T15:00:00",
      "reminders": ["PT5M"],
      "source": "任务M ddl 15:00"
    }
  ]
}
```

## 风险控制

- 网络失败：直接提示失败，不影响本地规则识别。
- JSON 解析失败：提示“AI 返回格式无法识别”，不导入。
- 时间缺失：进入预览页标红，要求用户补齐。
- 晚于 DDL / 开始时间的提醒：沿用现有非法提醒校验。
- 多 Provider：用户手动选择默认 Provider；失败时可以切换，不自动轮询所有 Key。

## 后续实现拆分

1. 设置页增加“AI 识别服务商”管理入口。
2. 支持从本地 JSON 模板导入 Provider 配置。
3. 规划台识别弹窗增加“本地识别 / AI 识别”入口。
4. 接入 OpenAI-compatible Chat Completions 调用。
5. AI 返回 JSON 转为现有 `PlanningImportCandidate`。
6. 进入现有预览、编辑、导入闭环。

