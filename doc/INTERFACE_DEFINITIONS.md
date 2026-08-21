# 出入金订单异常分诊 Agent——接口定义

## 1. 文档范围

本文定义系统实现所需的入口接口、应用接口、Agent 工具接口、规则接口、审计接口、执行接口和离线 CLI 契约。

接口以 Java 强类型端口为主，HTTP、JSON 文件、数据库和第三方 SDK 都作为适配器放在接口之外。本题默认使用文件型或内存型实现，后续接入真实服务时不修改领域规则。

本文依赖：[需求分析](./INTERVIEW_REQUIREMENTS_ANALYSIS.md)。

## 2. 设计原则

1. **判断与执行分离**：Agent 输出决策，资金执行网关独立复核。
2. **确定性规则拥有决定权**：LLM 不能直接返回可执行授权。
3. **接口强类型**：金额、资产、网络、订单类型和处置不能使用含义不明的字符串传递。
4. **失败关闭**：查询失败、事实缺失或事实冲突不能退化成“通过”。
5. **无隐式 null**：查询结果明确区分找到、未找到、暂时失败和冲突。
6. **幂等优先**：所有可能产生外部副作用的接口必须接收幂等键。
7. **可审计**：每次查询、规则判断和副作用都有稳定 ID 和证据引用。
8. **默认离线**：所有外部端口都有文件、内存或 no-op 实现。

## 3. 分层与包结构

```text
com.jason.yang.asset
├── domain
│   ├── order          # 三类订单和值对象
│   ├── decision       # 处置、原因、规则结果
│   ├── policy         # 策略快照和规则接口
│   └── audit          # 审计模型
├── application
│   ├── port.in        # 批处理、单笔分诊、评测入口
│   ├── port.out       # Agent 所需工具和外部系统端口
│   └── service        # Agent 编排、规则聚合、解释生成
├── adapter
│   ├── in.cli         # triage/evaluate 命令
│   ├── out.file       # 题目 JSON/JSONL 文件适配器
│   ├── out.memory     # 幂等、审计和执行 stub
│   └── out.llm        # 可选 LLM provider
└── config             # Spring 装配和运行配置
```

依赖方向必须保持：`adapter -> application -> domain`。领域层不得依赖 Spring、Jackson、文件路径或 LLM SDK。

## 4. 公共值对象

以下代码表示契约，不是最终实现的完整源码。

```java
public record OrderId(String value) {}
public record CustomerId(String value) {}
public record DecisionId(UUID value) {}
public record AuditId(UUID value) {}
public record CaseId(String value) {}
public record PolicyVersion(String value) {}
public record EvidenceRef(String source, String version, String key) {}

public enum OrderType {
    ON_RAMP, OFF_RAMP, WITHDRAWAL
}

public record AssetCode(String value) {}
public record NetworkCode(String value) {}
public record CurrencyCode(String value) {}

public record FiatMoney(
    BigDecimal amount,
    CurrencyCode currency
) {}

public record CryptoMoney(
    BigDecimal amount,
    AssetCode asset,
    NetworkCode network
) {}

public record RatePair(
    AssetCode base,
    CurrencyCode quote
) {}
```

约束：

- `BigDecimal` 从 JSON 文本直接构造，不经过 `double`。
- 金额必须大于零。
- 金额比较前必须使用资产/币种精度和明确舍入模式。
- 所有时间使用 `Instant`，不得依赖服务器本地时区。

## 5. 标准工具返回模型

所有查询型外部端口使用统一的失败语义：

```java
public sealed interface LookupResult<T>
    permits Found, NotFound, TemporarilyUnavailable, DataConflict {
}

public record Found<T>(T value, EvidenceRef evidence)
    implements LookupResult<T> {}

public record NotFound<T>(String code, String message)
    implements LookupResult<T> {}

public record TemporarilyUnavailable<T>(
    String code,
    String message,
    boolean retryable
) implements LookupResult<T> {}

public record DataConflict<T>(
    String code,
    String message,
    List<EvidenceRef> conflictingEvidence
) implements LookupResult<T> {}
```

语义约束：

| 结果 | 含义 | Agent 默认行为 |
| --- | --- | --- |
| `Found` | 获得可信事实 | 继续判断 |
| `NotFound` | 权威源明确无此数据 | 挂起、人工或运维，不能按通过处理 |
| `TemporarilyUnavailable` | 超时、连接失败、限流等 | 在预算内重试，之后挂起 |
| `DataConflict` | 多个可信事实互相矛盾 | 停止自动化并人工复核 |

参数非法属于程序错误或输入校验错误，不应伪装成 `NotFound`。

## 6. 订单输入模型

### 6.1 原始信封

```java
public record RawOrderEnvelope(
    String sourceName,
    long sourcePosition,
    String rawPayload,
    String payloadSha256,
    Instant receivedAt
) {}
```

### 6.2 统一订单接口

```java
public sealed interface Order
    permits OnRampOrder, OffRampOrder, WithdrawalOrder {

    OrderId orderId();
    CustomerId customerId();
    OrderType type();
    AssetCode asset();
    NetworkCode network();
    String customerNote();
}
```

### 6.3 入金订单

```java
public record OnRampOrder(
    OrderId orderId,
    CustomerId customerId,
    AssetCode asset,
    NetworkCode network,
    FiatMoney fiatAmount,
    CryptoMoney quotedCryptoAmount,
    Instant quoteExpiresAt,
    FiatReceiptReference fiatReceipt,
    String destinationAddress,
    CounterpartyInfo counterparty,
    String customerNote
) implements Order {}
```

题目中的 `fiat_status` 只能被文件 stub 转换为 `FiatReceiptReference`，生产实现必须查询支付网关，不能直接信任订单字符串。

### 6.4 出金订单

```java
public record OffRampOrder(
    OrderId orderId,
    CustomerId customerId,
    AssetCode asset,
    NetworkCode network,
    CryptoMoney quotedCryptoAmount,
    Instant quoteExpiresAt,
    DepositReference deposit,
    BankPayoutInstruction payout,
    CounterpartyInfo counterparty,
    String customerNote
) implements Order {}

public record DepositReference(
    String txHash,
    String transferIndex,
    String fromAddress,
    NetworkCode observedNetwork
) {}

public record BankPayoutInstruction(
    String bankAccountName,
    FiatMoney amount
) {}
```

### 6.5 提币订单

```java
public record WithdrawalOrder(
    OrderId orderId,
    CustomerId customerId,
    AssetCode asset,
    NetworkCode network,
    CryptoMoney amount,
    String destinationAddress,
    CounterpartyInfo counterparty,
    String customerNote
) implements Order {}

public record CounterpartyInfo(
    VaspStatus vaspStatus,
    String vaspName,
    OriginatorInfo originator,
    BeneficiaryInfo beneficiary
) {}

public enum VaspStatus {
    VASP, NOT_VASP, UNKNOWN
}
```

`VaspStatus.UNKNOWN` 与 `NOT_VASP` 必须区分，缺失字段不能默认成非 VASP。

## 7. 入口接口

### 7.1 单笔分诊入口

```java
public interface TriageOrderUseCase {
    TriageResult triage(TriageCommand command);
}

public record TriageCommand(
    RawOrderEnvelope envelope,
    RunContext runContext
) {}

public record RunContext(
    String runId,
    Instant evaluationTime,
    PolicyVersion policyVersion,
    ToolBudget toolBudget,
    ExecutionMode executionMode
) {}

public enum ExecutionMode {
    DECISION_ONLY,
    SIMULATED_EXECUTION,
    LIVE_EXECUTION
}
```

本题默认且推荐 `DECISION_ONLY`。`LIVE_EXECUTION` 不在笔试实现范围内。

### 7.2 批处理入口

```java
public interface ProcessOrderBatchUseCase {
    BatchResult process(BatchCommand command);
}

public record BatchCommand(
    Path ordersFile,
    Path outputFile,
    Path auditFile,
    RunContext runContext,
    int maxConcurrency
) {}

public record BatchResult(
    String runId,
    long total,
    long autoComplete,
    long held,
    long frozen,
    long rejected,
    long invalid,
    long failed,
    Duration elapsed
) {}
```

批处理中单行失败必须转成该行的 `INVALID_INPUT` 或内部错误结果，不能终止后续订单。

### 7.3 评测入口

```java
public interface EvaluateTriageUseCase {
    EvaluationReport evaluate(EvaluationCommand command);
}

public record EvaluationCommand(
    Path materialsDirectory,
    Path goldenCases,
    long randomSeed,
    int repetitions
) {}

public record EvaluationReport(
    int cases,
    int passed,
    int failed,
    int unsafeAutoCompletions,
    List<EvaluationFailure> failures
) {}
```

验收硬指标：`unsafeAutoCompletions == 0`。

## 8. 解析与校验接口

```java
public interface OrderParser {
    OrderParseResult parse(RawOrderEnvelope envelope);
}

public sealed interface OrderParseResult
    permits ParsedOrder, InvalidOrder {}

public record ParsedOrder(
    Order order,
    List<NormalizationEvent> normalizationEvents
) implements OrderParseResult {}

public record InvalidOrder(
    OrderId orderIdIfAvailable,
    List<InputViolation> violations
) implements OrderParseResult {}

public record InputViolation(
    String field,
    String code,
    String message
) {}
```

推荐输入错误码：

- `MALFORMED_JSON`
- `MISSING_REQUIRED_FIELD`
- `UNKNOWN_ORDER_TYPE`
- `INVALID_DECIMAL`
- `NON_POSITIVE_AMOUNT`
- `INVALID_TIMESTAMP`
- `INVALID_ADDRESS_FORMAT`
- `FIELD_NOT_ALLOWED_FOR_ORDER_TYPE`
- `CONFLICTING_FIELDS`

## 9. Agent 编排接口

### 9.1 分诊 Agent

```java
public interface TriageAgent {
    AgentOutcome investigate(Order order, AgentRunContext context);
}

public record AgentRunContext(
    String runId,
    Instant evaluationTime,
    PolicySnapshot policy,
    ToolBudget budget,
    ToolCallRecorder recorder
) {}

public record ToolBudget(
    int maxToolCalls,
    int maxAttemptsPerTool,
    Duration perToolTimeout,
    Duration totalTimeout
) {}
```

Agent 必须是有界状态机，不能让模型自由循环调用工具。

### 9.2 调查事实

```java
public record InvestigationFacts(
    Order order,
    LookupResult<CustomerProfile> customer,
    LookupResult<AssetNetworkPolicy> assetPolicy,
    LookupResult<AddressRiskAssessment> addressRisk,
    LookupResult<FundingEvidence> funding,
    LookupResult<ReferenceRate> referenceRate,
    LookupResult<TravelRuleAssessment> travelRule,
    LookupResult<DuplicateAssessment> duplicateAssessment,
    List<ToolCallRecord> toolCalls
) {}
```

不是所有字段对每类订单都适用；不适用应使用显式 `NotApplicable` 事实或由事实集合按类型建模，不能使用含义不明的 null。

## 10. 权威数据查询接口

### 10.1 客户查询

```java
public interface CustomerProfilePort {
    LookupResult<CustomerProfile> findCustomer(CustomerId customerId);
}

public record CustomerProfile(
    CustomerId customerId,
    String legalName,
    int kycTier,
    BigDecimal monthlyLimitUsd,
    BigDecimal monthlyUsedUsd,
    String verifiedBankName,
    CustomerStatus status
) {}

public enum CustomerStatus {
    ACTIVE, REVIEW_HOLD, SUSPENDED, CLOSED, UNKNOWN
}
```

文件 stub 没有 `monthlyUsedUsd`，应明确标记为未知；不能默认为零并声称完成了真实累计限额检查。

### 10.2 资产与网络策略查询

```java
public interface AssetPolicyPort {
    LookupResult<AssetNetworkPolicy> findPolicy(
        AssetCode asset,
        NetworkCode network
    );
}

public record AssetNetworkPolicy(
    AssetCode asset,
    NetworkCode network,
    BigDecimal minimumAmount,
    int confirmationsRequired,
    int amountScale,
    RoundingMode roundingMode
) {}
```

题目材料未提供 `amountScale` 和 `roundingMode`，实现必须在 `DECISIONS.md` 中说明默认值。

### 10.3 地址风险筛查

```java
public interface AddressRiskPort {
    LookupResult<AddressRiskAssessment> screen(AddressRiskQuery query);
}

public record AddressRiskQuery(
    NetworkCode network,
    String address,
    AddressDirection direction,
    OrderId orderId
) {}

public enum AddressDirection {
    INCOMING_SOURCE, OUTGOING_DESTINATION
}

public record AddressRiskAssessment(
    int riskScore,
    RiskCategory category,
    Instant assessedAt,
    String providerReference
) {}

public enum RiskCategory {
    CLEAN, SANCTIONED, MIXER, DARKNET, UNKNOWN, OTHER
}
```

### 10.4 参考汇率

```java
public interface ReferenceRatePort {
    LookupResult<ReferenceRate> getRate(
        RatePair pair,
        Instant asOf
    );
}

public record ReferenceRate(
    RatePair pair,
    BigDecimal rate,
    Instant observedAt,
    String source
) {}
```

文件材料没有汇率时间，stub 使用策略基准时间并在审计中标记为题目快照。

### 10.5 法币到账

```java
public interface FiatReceiptPort {
    LookupResult<FiatReceipt> getReceipt(
        OrderId orderId,
        FiatReceiptReference reference
    );
}

public record FiatReceipt(
    String receiptId,
    FiatMoney receivedAmount,
    FundingStatus status,
    Instant confirmedAt
) {}

public enum FundingStatus {
    PENDING, CONFIRMED, REVERSED, FAILED, UNKNOWN
}
```

### 10.6 链上到账

```java
public interface BlockchainDepositPort {
    LookupResult<ChainDeposit> getDeposit(DepositReference reference);
}

public record ChainDeposit(
    FundsEventKey eventKey,
    String fromAddress,
    CryptoMoney observedAmount,
    int confirmations,
    ChainDepositStatus status,
    Instant observedAt
) {}

public enum ChainDepositStatus {
    SEEN, CONFIRMED, REORGED, FAILED, UNKNOWN
}

public record FundsEventKey(
    NetworkCode network,
    String txHash,
    String transferIndex
) {}
```

### 10.7 钱包余额与预留

```java
public interface WalletFundsPort {
    LookupResult<WalletFundsSnapshot> getAvailableFunds(
        CustomerId customerId,
        AssetCode asset,
        NetworkCode network
    );

    MutationResult<FundsReservation> reserve(
        ReserveFundsCommand command
    );

    MutationResult<Void> releaseReservation(
        ReleaseReservationCommand command
    );
}

public record ReserveFundsCommand(
    OrderId orderId,
    CryptoMoney amount,
    String idempotencyKey
) {}
```

本题没有钱包余额材料，因此提供 `UnavailableWalletFundsAdapter`，任何提币都不能因该 stub 自动放行。

### 10.8 Travel Rule

```java
public interface TravelRulePort {
    LookupResult<TravelRuleAssessment> assess(
        TravelRuleQuery query
    );
}

public record TravelRuleQuery(
    OrderId orderId,
    FiatMoney usdEquivalent,
    CounterpartyInfo counterparty
) {}

public record TravelRuleAssessment(
    boolean required,
    boolean originatorComplete,
    boolean beneficiaryComplete,
    List<String> missingFields
) {}
```

本地实现可直接按字段和 `1000 USD` 阈值判断，不需要 LLM。

## 11. 策略与规则接口

### 11.1 策略快照

```java
public interface PolicyProvider {
    PolicySnapshot currentPolicy();
}

public record PolicySnapshot(
    PolicyVersion version,
    Instant effectiveAt,
    BigDecimal quoteSlippageTolerance,
    BigDecimal travelRuleThresholdUsd,
    List<TriageRule> rules
) {}
```

### 11.2 单条规则

```java
public interface TriageRule {
    RuleId id();
    Set<OrderType> applicableOrderTypes();
    RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy);
}

public record RuleId(String value) {}

public record RuleResult(
    RuleId ruleId,
    RuleOutcome outcome,
    RuleSeverity severity,
    ReasonCode reasonCode,
    String messageTemplate,
    Map<String, String> parameters,
    List<EvidenceRef> evidence
) {}

public enum RuleOutcome {
    PASS, FAIL, UNKNOWN, NOT_APPLICABLE
}

public enum RuleSeverity {
    INFO, HOLD, OPS, REJECT, FREEZE
}
```

规则只做纯计算：不能写数据库、调用外部服务或执行资金动作。

### 11.3 规则引擎和决策聚合

```java
public interface RuleEngine {
    List<RuleResult> evaluateAll(
        InvestigationFacts facts,
        PolicySnapshot policy
    );
}

public interface DecisionAggregator {
    TriageDecision aggregate(
        Order order,
        List<RuleResult> ruleResults,
        DecisionContext context
    );
}
```

`evaluateAll` 应尽量评估所有有事实支持的规则，不能使用普通的 first-match 逻辑漏掉更严重风险。

## 12. 决策输出接口

```java
public enum Disposition {
    AUTO_COMPLETE,
    HOLD,
    FREEZE_COMPLIANCE,
    REJECT_ESCALATE,
    REQUOTE,
    REFUND_REVIEW,
    OPS_RECOVERY,
    DUPLICATE_NOOP,
    MANUAL_REVIEW,
    INVALID_INPUT
}

public record ReasonCode(String value) {}

public record TriageDecision(
    String contractVersion,
    DecisionId decisionId,
    OrderId orderId,
    Disposition disposition,
    List<ReasonCode> reasonCodes,
    boolean fundsMovementAllowed,
    List<NextAction> nextActions,
    PolicyVersion policyVersion,
    Instant evaluatedAt
) {}

public enum NextAction {
    EXECUTE_PAYOUT,
    EXECUTE_CRYPTO_TRANSFER,
    WAIT_FOR_CONFIRMATIONS,
    OPEN_COMPLIANCE_CASE,
    OPEN_OPERATIONS_CASE,
    REQUEST_TRAVEL_RULE_INFO,
    REQUEST_REQUOTE_ACCEPTANCE,
    REVIEW_REFUND,
    NO_ACTION
}

public record TriageResult(
    TriageDecision decision,
    String humanExplanation,
    AuditId auditId,
    SideEffectSummary sideEffects
) {}
```

强制约束：只有 `AUTO_COMPLETE` 可以令 `fundsMovementAllowed=true`，但这仍不是执行授权本身。

### 12.1 JSONL 输出示例

```json
{
  "contract_version": "1.0",
  "decision_id": "d8b3...",
  "order_id": "O-003",
  "disposition": "FREEZE_COMPLIANCE",
  "reason_codes": ["ADDRESS_SANCTIONED", "ADDRESS_HIGH_RISK"],
  "funds_movement_allowed": false,
  "next_actions": ["OPEN_COMPLIANCE_CASE"],
  "human_explanation": "来源地址命中 sanctioned，风险分为 99，订单已冻结并上报合规。",
  "policy_version": "policy-2026-07-28",
  "evaluated_at": "2026-07-28T12:00:00Z",
  "audit_id": "a41f..."
}
```

## 13. 幂等与处理状态接口

订单处理幂等和资金事件幂等必须分开。

```java
public interface OrderProcessingPort {
    ProcessingClaim claimOrder(OrderClaimCommand command);
    void completeOrder(OrderCompletion command);
    void failOrder(OrderFailure command);
}

public record OrderClaimCommand(
    OrderId orderId,
    String payloadSha256,
    String runId
) {}

public enum ClaimStatus {
    ACQUIRED,
    ALREADY_COMPLETED_SAME_PAYLOAD,
    ALREADY_RUNNING,
    PAYLOAD_CONFLICT
}

public interface FundsEventRegistryPort {
    LookupResult<FundsEventRegistration> find(FundsEventKey key);
    FundsEventClaim claim(FundsEventClaimCommand command);
    void markCredited(FundsEventCreditCommand command);
}

public enum FundsEventClaimStatus {
    ACQUIRED, ALREADY_CREDITED, IN_PROGRESS, CONFLICT
}
```

`claim` 和状态写入必须由存储层保证原子性。不能用“先查再写”的两个独立操作实现生产幂等。

## 14. 审计接口

```java
public interface DecisionAuditPort {
    AuditAppendResult append(DecisionAuditRecord record);
}

public record DecisionAuditRecord(
    String auditSchemaVersion,
    DecisionId decisionId,
    OrderId orderId,
    String runId,
    String payloadSha256,
    PolicyVersion policyVersion,
    Instant evaluatedAt,
    List<FactAuditEntry> facts,
    List<ToolCallRecord> toolCalls,
    List<RuleResult> ruleResults,
    TriageDecision decision,
    String explanationGenerator,
    List<SideEffectRecord> sideEffects
) {}

public record ToolCallRecord(
    String callId,
    String toolName,
    String sanitizedInputHash,
    String resultType,
    EvidenceRef evidence,
    Duration latency,
    int attempt
) {}
```

审计写入失败时：

- 决策可以作为失败结果返回。
- 不得继续执行任何资金动作。
- 应产生 `AUDIT_PERSISTENCE_FAILED` 告警或隔离记录。

本题使用 append-only JSONL 审计适配器；生产实现可替换为不可变审计存储。

## 15. 工单与人工处理接口

```java
public interface CaseManagementPort {
    MutationResult<CaseReference> openCase(OpenCaseCommand command);
}

public record OpenCaseCommand(
    String idempotencyKey,
    CaseType caseType,
    OrderId orderId,
    DecisionId decisionId,
    List<ReasonCode> reasonCodes,
    List<EvidenceRef> evidence,
    CasePriority priority
) {}

public enum CaseType {
    COMPLIANCE, OPERATIONS, MANUAL_REVIEW, REFUND_REVIEW
}

public enum CasePriority {
    NORMAL, HIGH, CRITICAL
}
```

同一决定重复执行 `openCase` 必须返回同一个 case，而不是创建多个工单。

## 16. 资金执行接口

```java
public interface FundsExecutionGateway {
    ExecutionResult execute(AuthorizedExecution command);
}

public record AuthorizedExecution(
    String idempotencyKey,
    Order order,
    TriageDecision decision,
    AuditId auditId,
    FundingAuthorization fundingAuthorization,
    PolicyVersion policyVersion
) {}

public sealed interface ExecutionResult
    permits ExecutionSucceeded, ExecutionPending,
            ExecutionRejected, ExecutionStatusUnknown {}
```

网关执行前必须独立验证：

1. `disposition == AUTO_COMPLETE`。
2. `fundsMovementAllowed == true`。
3. 审计记录已持久化。
4. 资金来源仍已确认或预留。
5. 地址风险和客户状态没有被更新为更高风险状态。
6. 幂等键此前没有成功执行。
7. 实际释放价值不超过授权价值。

本题只提供 `NoOpFundsExecutionGateway` 或 `RecordingFundsExecutionGateway`，不得接真实资金系统。

## 17. 副作用返回模型

```java
public sealed interface MutationResult<T>
    permits MutationSucceeded, MutationAlreadyApplied,
            MutationRejected, MutationTemporarilyUnavailable,
            MutationStatusUnknown {}
```

`MutationStatusUnknown` 是重要状态：发生超时后不能直接重试资金动作，必须先按幂等键查询远端结果。

## 18. 决策解释和可选 LLM 接口

### 18.1 业务解释接口

```java
public interface DecisionExplanationService {
    String explain(ExplanationRequest request);
}

public record ExplanationRequest(
    TriageDecision decision,
    List<RuleResult> ruleResults,
    Locale locale
) {}
```

默认实现 `TemplateDecisionExplanationService` 使用原因码和模板离线生成解释。

### 18.2 LLM Provider 接口

```java
public interface LanguageModelPort {
    LlmResult generate(LlmRequest request);
}

public record LlmRequest(
    String templateVersion,
    Map<String, Object> sanitizedStructuredFacts,
    int maxOutputTokens,
    Duration timeout
) {}
```

限制：

- 不把原始 `customer_note` 拼接成系统指令。
- LLM 只能润色已经确定的结构化决定。
- LLM 返回的 disposition、动作或覆盖建议一律忽略。
- LLM 失败时回退模板解释，不能影响主决策。
- 默认 provider 为 `StubLanguageModelAdapter`，真实 provider 通过环境变量启用。

## 19. 时钟和 ID 接口

```java
public interface EvaluationClock {
    Instant now();
}

public interface IdentifierGenerator {
    DecisionId nextDecisionId();
    AuditId nextAuditId();
}
```

测试和题目运行使用固定时钟 `2026-07-28T12:00:00Z`；生产实现使用系统时钟。禁止在规则内部直接调用 `Instant.now()`。

## 20. CLI 契约

### 20.1 处理队列

```text
java -jar target/asset-*.jar triage \
  --materials <directory> \
  --orders <orders.jsonl> \
  --output <decisions.jsonl> \
  --audit <audit.jsonl> \
  --clock 2026-07-28T12:00:00Z \
  --mode decision-only
```

默认值：

- `--materials`：题目材料目录。
- `--orders`：`<materials>/orders.jsonl`。
- `--clock`：题目模式使用策略指定时间。
- `--mode`：`decision-only`。

### 20.2 运行评测

```text
java -jar target/asset-*.jar evaluate \
  --materials <directory> \
  --golden <golden-cases.json> \
  --report <evaluation-report.json>
```

### 20.3 退出码

| 退出码 | 含义 |
| --- | --- |
| `0` | 命令成功；队列中可以有业务上的 HOLD/FREEZE |
| `2` | CLI 参数或配置错误 |
| `3` | 材料文件不可读或格式整体无效 |
| `4` | 批处理发生未恢复的系统错误 |
| `5` | 评测失败或发现不安全自动放行 |

业务处置不是进程失败，不能因为存在冻结订单就返回非零退出码。

## 21. 文件适配器映射

| 文件 | 接口实现 |
| --- | --- |
| `customers.json` | `FileCustomerProfileAdapter` |
| `assets.json` | `FileAssetPolicyAdapter` |
| `address_risk.json` | `FileAddressRiskAdapter` |
| `reference_rates.json` | `FileReferenceRateAdapter` |
| `orders.jsonl` 中 deposit 字段 | `EmbeddedDepositStubAdapter` |
| `orders.jsonl` 中 fiat_status | `EmbeddedFiatReceiptStubAdapter` |
| 无对应材料 | `UnavailableWalletFundsAdapter` |
| 本地 JSONL | `JsonLinesDecisionAuditAdapter` |
| 内存或本地状态文件 | `LocalIdempotencyAdapter` |
| 不执行真实资金动作 | `RecordingFundsExecutionGateway` |

嵌入订单的到账字段在题目中作为 stub 事实使用，但生产适配器必须改为独立查询，避免信任不可信订单载荷。

## 22. 建议的最小实现清单

### 第一阶段必须实现

- `ProcessOrderBatchUseCase`
- `TriageOrderUseCase`
- `OrderParser`
- `TriageAgent`
- `CustomerProfilePort`
- `AssetPolicyPort`
- `AddressRiskPort`
- `ReferenceRatePort`
- `FiatReceiptPort`
- `BlockchainDepositPort`
- `PolicyProvider`
- `TriageRule` / `RuleEngine`
- `DecisionAggregator`
- `OrderProcessingPort`
- `FundsEventRegistryPort`
- `DecisionAuditPort`
- `DecisionExplanationService`
- `EvaluateTriageUseCase`

### 用 stub 明确占位

- `WalletFundsPort`
- `CaseManagementPort`
- `FundsExecutionGateway`
- `LanguageModelPort`

### 本题不需要实现

- 对外 REST Controller。
- 真实银行付款接口。
- 真实链上转账接口。
- 真实 KYC、制裁或 Travel Rule Provider。
- 真实 LLM 调用。

## 23. 关键接口调用顺序

```text
CLI / Batch
  -> OrderParser
  -> OrderProcessingPort.claimOrder
  -> PolicyProvider.currentPolicy
  -> TriageAgent
       -> CustomerProfilePort
       -> AssetPolicyPort
       -> AddressRiskPort
       -> FiatReceiptPort / BlockchainDepositPort / WalletFundsPort
       -> ReferenceRatePort（仅报价过期或需 USD 等值时）
       -> TravelRulePort（条件适用时）
       -> FundsEventRegistryPort（有链上到账时）
       -> RuleEngine.evaluateAll
       -> DecisionAggregator
  -> DecisionExplanationService
  -> DecisionAuditPort.append
  -> CaseManagementPort（非资金副作用，按决定执行）
  -> FundsExecutionGateway（仅 AUTO_COMPLETE 且模式允许）
  -> OrderProcessingPort.completeOrder
  -> JSONL output
```

## 24. 尚需在 DECISIONS.md 确认的接口选择

1. 本题是否完全不模拟资金执行，还是使用 recording gateway 展示调用记录。
2. 幂等状态使用内存、SQLite 还是本地文件；内存最简单但不能演示跨进程恢复。
3. 规则是纯 Java 类，还是部分阈值外置为配置；铁律不建议由可随意修改的文本配置控制。
4. 对缺失的月累计额度采用 `unknown` 挂起，还是按题面只比较单笔上限。
5. `withdrawal` 是否全部因余额事实缺失而挂起，还是题目上下文允许假设上游已预留；必须显式说明。
6. 地址格式是否做链级严格校验；样本地址是 stub，不符合真实 BTC/ETH 地址格式。
7. 输出解释是否只用模板；当前题目不需要真实 LLM，模板更安全且完全可重复。
8. 是否将工单创建纳入本题输出，还是只输出 `next_actions`。

## 25. 接口验收标准

- 核心规则可以只使用领域对象和端口接口进行测试。
- 将文件适配器替换为 mock 后，不需要启动 Spring 即可测试 Agent。
- 任何外部查询失败都不能产生 `AUTO_COMPLETE`。
- 任何资金副作用都有幂等键、审计 ID 和明确的未知状态处理。
- 相同事实快照和策略版本产生相同结构化决定。
- 关闭 LLM 或没有 API Key 时，全部队列和评测仍能运行。
- 新增一条规则只需新增 `TriageRule` 并注册，不修改 Agent 主流程。
