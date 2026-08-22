# Asset Exception Triage

数字资产出入金异常队列分流系统。系统逐单输出：

- 结构化处置决定（`Disposition`）；
- 人可以直接理解的原因说明；
- 可供运维和合规追溯的审计记录。

最终资金处置由确定性的领域规则产生。LLM Agent 仅作为受控调查 Demo，默认使用离线 `stub`，不需要 API Key，也不能直接决定或执行资金动作。

## 环境要求

- JDK 21
- Maven 3.9+

## 构建

```powershell
mvn clean package
```

确认 `mvn -version` 显示的运行时为 Java 21；Maven 会优先使用 `JAVA_HOME`。

## 一条命令跑完整队列

先完成构建，然后在项目根目录执行：

```powershell
java "-Dspring.profiles.active=cli" -jar target\asset-0.0.1-SNAPSHOT.jar triage
```

PowerShell 中必须给整个 `-Dspring.profiles.active=cli` 参数加引号，否则可能被解析成错误的主类名。

输入文件：`materials/orders.jsonl`

输出文件：

- `build/decisions.jsonl`：决定、原因码和人类可读说明；
- `build/audit.jsonl`：事实、规则结果、策略版本和 Agent 轨迹。

## 一条命令跑评测

```powershell
java "-Dspring.profiles.active=cli" -jar target\asset-0.0.1-SNAPSHOT.jar evaluate --materials materials --golden evaluation\golden-cases.json --report build\evaluation-report.json
```

评测将实际结果与黄金用例比较，重点检查：

```text
failed = 0
unsafeAutoCompletions = 0
```

完整报告写入 `build/evaluation-report.json`。命令在评测失败时以非零状态退出。

## Web 测试

直接运行 `AssetApplication`，或执行：

```powershell
java -jar target\asset-0.0.1-SNAPSHOT.jar
```

然后调用与 CLI 相同的文件驱动流程：

```powershell
curl.exe -X POST http://localhost:8080/api/v1/test/triage
curl.exe -X POST http://localhost:8080/api/v1/test/evaluate
```

## 安全边界

- 默认模式为 `DECISION_ONLY`，不会连接真实银行、钱包或区块链执行资金动作；
- 事实缺失、数据冲突、工具不可用、超时或预算耗尽时一律失败关闭；
- 只有明确的 `AUTO_COMPLETE` 且审计成功后，订单才具备资金执行资格；
- 模型只能从服务端白名单中建议调查工具，最终决定仍由版本化规则生成。

## 进一步阅读

- [关键设计决策](doc/DECISIONS.md)
- [需求分析](doc/INTERVIEW_REQUIREMENTS_ANALYSIS.md)
- [接口定义](doc/INTERFACE_DEFINITIONS.md)
- [核心流程](doc/CORE_PROCESS_FLOW.md)
- [领域模型 UML](doc/DOMAIN_MODEL_UML.md)
- [代码走读指南](doc/CODE_WALKTHROUGH_GUIDE.md)
- [LLM Demo 走读手册](doc/LLM_DEMO_CODE_WALKTHROUGH.md)
