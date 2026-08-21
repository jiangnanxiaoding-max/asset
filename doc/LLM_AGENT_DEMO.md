# LLM Agent 调查实现说明

## 1. 定位

LLM 只负责在权威数据查询出现 `UNAVAILABLE` 或 `CONFLICT` 时，选择允许调用的补查工具。它不生成最终决定，也不能直接触发资金操作。

完整链路：

```text
DefaultTriageOrderService
  -> GuardedInvestigationService
       -> DefaultInvestigationService       先执行确定性查询
       -> LlmAgentInvestigationAdapter       仅补查未解决事实
            -> InvestigationPlan             计算缺失事实、前置条件和可用工具
            -> LlmAgentClient                stub / replay / http
            -> PortBackedAgentToolbox        调用现有权威查询 Port
  -> DefaultRuleEngine                       确定性规则
  -> DefaultDecisionAggregator               确定性决策
  -> JsonLinesDecisionAuditAdapter            保存事实、规则与 Agent 轨迹
```

因此，即使模型输出错误、超时或不可用，系统也只会保留原有事实并进入 HOLD/人工处理，不会因为模型文字直接放款。

## 2. Provider 模式

默认不需要 API Key：

```text
ASSET_LLM_PROVIDER=stub
```

支持三种模式：

| 模式 | 用途 | 必要配置 |
|---|---|---|
| `stub` | 默认离线运行；按可用工具顺序返回调用 | 无 |
| `replay` | 使用 JSONL 录制结果做回放测试 | `ASSET_LLM_REPLAY_FILE` |
| `http` | 调用真实的受控 LLM Gateway | `ASSET_LLM_ENDPOINT`、`ASSET_LLM_API_KEY`、`ASSET_LLM_MODEL` |

真实 Provider 必须显式设置 `ASSET_LLM_PROVIDER=http`。缺少 endpoint、key 或 model 时，应用在创建运行时阶段直接失败，避免误以为已经启用真实模型。

Replay 文件每行是一轮模型响应，例如：

```json
{"type":"tool_call","tool_name":"funding","tokens_used":42}
{"type":"finish","tokens_used":12}
```

## 3. 模型看到什么

每轮请求只包含脱敏且受限的信息：订单 ID、订单类型、资产、网络、策略版本、缺失事实、当前允许的工具定义、已完成工具、工具结果类型摘要和剩余预算。

每个工具定义包含：

- 工具名称和用途说明；
- 它能产生的事实类型；
- 适用的订单类型；
- 必须先取得的前置事实。

工具不接受模型生成的 customerId、地址、金额等业务参数；这些参数由服务端从当前订单绑定。模型只能返回：

```json
{"type":"tool_call","tool":"funding"}
```

或：

```json
{"type":"finish"}
```

提前 `finish`、调用非白名单工具、调用不适用于当前订单的工具、前置条件未满足或重复调用，都会停止本次 Agent，并保持 fail-closed。

## 4. 停止条件和成本保护

单订单 `AgentExecutionPolicy.demoDefaults()`：

| 限制 | 默认值 |
|---|---:|
| 最大 Agent 轮数 | 8 |
| 最大工具调用数 | 7 |
| 最大 Token 数 | 8000 |
| 总超时 | 30 秒 |
| 最大重试数 | 2 |
| 基础退避 | 200 毫秒 |

进程级 `AgentBatchBudget.demoDefaults()`：

| 限制 | 默认值 |
|---|---:|
| 模型总调用预算 | 2000 |
| Token 总预算 | 2,000,000 |
| 并发 Agent 会话 | 4 |
| 连续失败熔断阈值 | 5 |

另外，模型客户端外层有硬超时和有界线程池。仅 retryable 错误允许指数退避加随机抖动后重试；非重试错误、预算耗尽、并发满、熔断、超时都会立即停止。

## 5. 审计记录

发生 Agent 补查时，同一条决策 JSONL 会增加 `agent` 字段：

```json
{
  "agent": {
    "provider": "stub",
    "model": "offline-stub-v1",
    "prompt_version": "triage-tools-v1",
    "iterations": 2,
    "model_calls": 1,
    "tool_calls": 1,
    "retry_count": 0,
    "tokens_used": 0,
    "elapsed_millis": 3,
    "stop_reason": "LLM_NO_ELIGIBLE_TOOLS",
    "tools": [
      {"tool":"funding","result":"UNAVAILABLE","elapsed_millis":0}
    ]
  }
}
```

审计只记录工具名称、结果类别和运行指标，不保存原始订单内容、客户备注、Prompt 或模型自由文本。

## 6. 策略与 Agent 的边界

业务政策和资金安全规则仍位于 domain policy，由 `DefaultRuleEngine` 执行；修改一条规则不需要修改 Prompt。Agent 运行机制位于 infrastructure，替换模型只需实现 `LlmAgentClient` 或选择不同 Provider。

允许自动完成的条件仍由确定性规则统一决定。任何未解决事实、证据冲突、模型故障或预算停止都会让规则产生非自动结果，从结构上保证 LLM 不能绕过人工决策边界。
