# Web 测试接口

## 1. 定位

Web Controller 不接收单笔订单，也没有创建另一套业务流程。它只把现有 CLI 的两个文件驱动命令暴露成 HTTP 测试入口：

| Web 接口 | 对应 CLI 命令 | 默认输入 |
| --- | --- | --- |
| `POST /api/v1/test/triage` | `triage` | `materials/orders.jsonl` |
| `POST /api/v1/test/evaluate` | `evaluate` | `evaluation/golden-cases.json` |

两种入口共用 `ProcessOrderBatchUseCase`、`EvaluateTriageUseCase`、领域规则和基础设施 Adapter，结果应当一致。Web 接口仅用于本地测试。

## 2. 启动

默认是 Web 模式，可以在 IDEA 直接运行 `com.jason.yang.AssetApplication`，也可以在项目根目录执行：

```powershell
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

IDEA 的 Working directory 必须是项目根目录，否则相对文件路径无法解析。

## 3. 完整队列分诊

```http
POST /api/v1/test/triage
X-Request-Id: local-triage-001
```

不需要请求体。接口读取 `materials/orders.jsonl` 的全部订单并执行完整批处理。

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/test/triage `
  -Headers @{ "X-Request-Id" = "local-triage-001" }
```

结果写入 `build/decisions.jsonl` 和 `build/audit.jsonl`。响应返回总数、失败数、决定分布以及两个结果文件的绝对路径。

## 4. Golden Evaluation

```http
POST /api/v1/test/evaluate
X-Request-Id: local-evaluate-001
```

不需要请求体。接口读取 `materials/orders.jsonl` 和 `evaluation/golden-cases.json`。

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/test/evaluate `
  -Headers @{ "X-Request-Id" = "local-evaluate-001" }
```

报告写入 `build/evaluation-report.json`。预期关键指标：

```json
{
  "cases": 14,
  "passed": 14,
  "failed": 0,
  "unsafeAutoCompletions": 0,
  "successful": true
}
```

## 5. 默认文件配置

```yaml
asset:
  materials: materials
  golden-cases: evaluation/golden-cases.json
  evaluation-report: build/evaluation-report.json
```

分诊队列的固定文件路径、评估时刻和并发度由 `DefaultRunTriageQueueService` 统一初始化。Web 与 CLI 都直接调用同一个无参 `RunTriageQueueUseCase.run()`；应用服务内部串行执行，并复用相同配置对应的离线 Runtime。

## 6. CLI 仍然可用

启用 `cli` profile 后，原有 `triage` 和 `evaluate` 命令保持不变。CLI 模式不启动 Web Server；默认 Web 模式不运行 CLI Runner。
