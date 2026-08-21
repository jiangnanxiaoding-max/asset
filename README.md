# Asset Exception Triage Agent

一个面向数字资产出入金异常队列的离线分流系统，支持 `on_ramp`、`off_ramp` 和 `withdrawal` 三类订单。

系统为每张订单输出结构化决定、人能理解的理由和可追溯审计记录。最终资金决定由确定性领域规则产生；Agent 只在权威查询出现未解决事实时选择受限工具进行补查，默认使用离线 Stub，不需要付费 API Key。

## 环境要求

- JDK 21
- Maven 3.9+
- 无需数据库、外部服务或 LLM API Key

确认环境：

```powershell
java -version
mvn -version
```

## 构建与测试

```powershell
mvn clean package
```

该命令会编译项目、执行全部自动化测试，并生成：

```text
target/asset-0.0.1-SNAPSHOT.jar
```

## 一条命令跑完整队列

在项目根目录执行：

```powershell
java -Dspring.profiles.active=cli `
  -jar target/asset-0.0.1-SNAPSHOT.jar `
  triage
```

输出文件：

- `build/decisions.jsonl`：每张订单一条结构化决定、原因码和人类可读说明。
- `build/audit.jsonl`：事实摘要、规则结果和 Agent 运行轨迹等追加式审计记录。

`-Dspring.profiles.active=cli` 不能省略，而且必须放在 `-jar` 前。省略时应用会按 Web 服务启动并监听 8080 端口，不会执行 CLI 命令后退出。

## 一条命令跑评测

```powershell
java -Dspring.profiles.active=cli `
  -jar target/asset-0.0.1-SNAPSHOT.jar `
  evaluate `
  --materials materials `
  --golden evaluation/golden-cases.json `
  --report build/evaluation-report.json
```

评测会重新运行完整队列，并将实际的 `disposition` 和必要 `reason_codes` 与黄金用例比较。重点安全指标是：

```text
failed = 0
unsafeAutoCompletions = 0
```

完整报告写入：

```text
build/evaluation-report.json
```

评测不通过时 CLI 会以失败结束，并提示检查报告文件。

## Web 测试接口

直接在 IDEA 中运行 `AssetApplication`，或者执行：

```powershell
java -jar target/asset-0.0.1-SNAPSHOT.jar
```

默认监听：

```text
http://localhost:8080
```

处理配置好的完整订单文件：

```powershell
curl.exe -X POST http://localhost:8080/api/v1/test/triage
```

运行黄金用例评测：

```powershell
curl.exe -X POST http://localhost:8080/api/v1/test/evaluate
```

这两个接口是 CLI 流程的测试适配器，仍然读取项目配置的文件，不接收任意订单请求体。

## Agent Provider

默认模式：

```text
ASSET_LLM_PROVIDER=stub
```

支持以下 Provider：

| Provider | 用途 | 必要配置 |
|---|---|---|
| `stub` | 默认离线、确定性运行 | 无 |
| `replay` | 回放 JSONL 模型响应 | `ASSET_LLM_REPLAY_FILE` |
| `http` | 调用受控的真实 LLM Gateway | `ASSET_LLM_ENDPOINT`、`ASSET_LLM_API_KEY`、`ASSET_LLM_MODEL` |

PowerShell 中启用真实 HTTP Provider 的示例：

```powershell
$env:ASSET_LLM_PROVIDER = "http"
$env:ASSET_LLM_ENDPOINT = "https://your-gateway.example.com/agent"
$env:ASSET_LLM_API_KEY = "your-key"
$env:ASSET_LLM_MODEL = "your-model"
```

真实 Provider 的必要配置缺失时，运行时会直接拒绝启动。无论使用哪种 Provider，模型都不能直接生成最终决定或执行资金动作。

## 核心处理流程

```text
Web / CLI
  -> RunTriageQueueUseCase（共享应用入口）
  -> JSONL 订单队列
  -> 输入解析与校验
  -> 订单幂等 claim
  -> 确定性事实调查
  -> 未解决事实的受控 Agent 补查
  -> 确定性领域规则
  -> 决策聚合
  -> 审计落盘
  -> 输出决定 / 创建人工工单
```

当事实冲突、工具不可用、Agent 超时、重试耗尽或预算耗尽时，系统采用 fail-closed 行为，产生 `HOLD` 或人工处理结果，不会自动移动资金。

## 结果集

系统可能输出：

- `AUTO_COMPLETE`
- `HOLD`
- `MANUAL_REVIEW`
- `FREEZE_COMPLIANCE`
- `REJECT_ESCALATE`
- `REQUOTE`
- `OPS_RECOVERY`
- `DUPLICATE_NOOP`
- `INVALID_INPUT`

默认执行模式是 `DECISION_ONLY`，即使结果为 `AUTO_COMPLETE`，演示环境也不会连接真实银行、钱包或区块链 Provider。

## 代码结构

```text
adapter/          CLI、Web 和 JSON 输入输出适配器
application/      共享队列入口、单笔分流、批处理、评测编排和端口定义
domain/           聚合根、值对象、领域规则、决策服务和领域事件
infrastructure/   文件数据源、Agent Provider、审计、幂等和外部端口实现
```

领域层不依赖 Spring、Jackson 或持久化框架。策略规则和 Agent 运行机制相互分离：修改领域规则不需要修改 Prompt，更换模型不需要修改领域决策代码。

## 进一步阅读

- [关键设计决策](doc/DECISIONS.md)
- [需求分析](doc/INTERVIEW_REQUIREMENTS_ANALYSIS.md)
- [接口定义](doc/INTERFACE_DEFINITIONS.md)
- [核心流程](doc/CORE_PROCESS_FLOW.md)
- [LLM Agent 实现说明](doc/LLM_AGENT_DEMO.md)
- [Web API](doc/WEB_API.md)

## 常见问题

### 启动后为什么一直占用 8080 端口？

你启动的是默认 Web 模式。需要执行队列并在完成后退出时，请加入：

```text
-Dspring.profiles.active=cli
```

### 为什么提现订单可能进入 HOLD？

离线材料没有真实钱包余额和原子资金预留记录。系统不会相信订单自身声明的余额，因此必要资金事实不可用时会安全挂起。

### 为什么没有调用真实大模型？

默认 Provider 是 `stub`，用于无 Key 离线运行。只有显式设置 `ASSET_LLM_PROVIDER=http` 并提供完整配置后，才会调用真实 Gateway。
