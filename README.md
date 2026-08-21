# Asset Exception Triage Agent

一个可离线运行、确定性决策、默认拒绝自动放行的出入金异常分诊系统。实现覆盖 `on_ramp`、`off_ramp` 和 `withdrawal` 三类订单，输出结构化决定、解释和追加式审计记录。

## 环境

- JDK 21
- Maven 3.9+
- 无需 API Key、数据库或外部服务

## 构建与测试

```powershell
mvn clean test
mvn package
```

## 跑完整队列

```powershell
java -jar target/asset-0.0.1-SNAPSHOT.jar triage `
  --materials materials `
  --orders materials/orders.jsonl `
  --output build/decisions.jsonl `
  --audit build/audit.jsonl `
  --clock 2026-07-28T12:00:00Z `
  --max-concurrency 1
```

输出：

- `build/decisions.jsonl`：每笔订单一条结构化决定。
- `build/audit.jsonl`：追加式审计记录，不包含原始客户备注。

## 跑 golden evaluation

```powershell
java -jar target/asset-0.0.1-SNAPSHOT.jar evaluate `
  --materials materials `
  --golden evaluation/golden-cases.json `
  --report build/evaluation-report.json
```

验收硬指标是 `unsafeAutoCompletions = 0`。

## DDD 分层

```text
adapter/          CLI、JSON 输入边界
application/      单笔分诊、批处理、评测编排和端口
domain/           聚合根、值对象、领域策略、领域服务和领域事件
infrastructure/   文件数据源、审计、幂等、工单及模拟执行适配器
```

领域层不依赖 Spring、Jackson 或持久化框架。规则全部为纯计算，外部查询通过端口反转依赖。只有审计完成后的 `AUTO_COMPLETE` 聚合才具备资金执行资格；默认运行模式仍是 `DECISION_ONLY`，不会真实动钱。

详细设计和取舍见 [doc/DECISIONS.md](doc/DECISIONS.md)，接口及流程见 [doc/INTERFACE_DEFINITIONS.md](doc/INTERFACE_DEFINITIONS.md) 与 [doc/CORE_PROCESS_FLOW.md](doc/CORE_PROCESS_FLOW.md)。
