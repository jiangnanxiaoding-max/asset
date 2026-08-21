package com.jason.yang.asset.adapter.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jason.yang.asset.application.input.InputViolation;
import com.jason.yang.asset.application.input.OrderParseResult;
import com.jason.yang.asset.application.input.OrderParser;
import com.jason.yang.asset.application.input.RawOrderEnvelope;
import com.jason.yang.asset.domain.BankPayout;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.DepositReference;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.WithdrawalOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Strict JSON parser for the heterogeneous order queue. */
public final class JacksonOrderParser implements OrderParser {
    private final ObjectMapper objectMapper;

    public JacksonOrderParser() {
        this(JsonMapper.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build());
    }

    public JacksonOrderParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OrderParseResult parse(RawOrderEnvelope envelope) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(envelope.rawPayload());
        } catch (JsonProcessingException exception) {
            return invalid(Optional.empty(), "", "MALFORMED_JSON", "Malformed JSON object");
        }
        if (root == null || !root.isObject()) {
            return invalid(Optional.empty(), "", "MALFORMED_JSON", "Queue line must be a JSON object");
        }

        Optional<String> possibleOrderId = optionalText(root, "order_id");
        List<InputViolation> violations = new ArrayList<>();
        String orderId = requiredText(root, "order_id", violations);
        String type = requiredText(root, "type", violations).toLowerCase(Locale.ROOT);
        String customerId = requiredText(root, "customer_id", violations);
        String asset = requiredText(root, "asset", violations).toUpperCase(Locale.ROOT);
        String network = requiredText(root, "network", violations).toUpperCase(Locale.ROOT);
        String note = optionalText(root, "customer_note").orElse("");

        if (!violations.isEmpty()) {
            return new OrderParseResult.Invalid(possibleOrderId, violations);
        }

        try {
            Order order;
            if ("on_ramp".equals(type)) {
                order = parseOnRamp(root, orderId, customerId, asset, network, note, violations);
            } else if ("off_ramp".equals(type)) {
                order = parseOffRamp(root, orderId, customerId, asset, network, note, violations);
            } else if ("withdrawal".equals(type)) {
                order = parseWithdrawal(root, orderId, customerId, asset, network, note, violations);
            } else {
                violations.add(new InputViolation("type", "UNKNOWN_ORDER_TYPE", "Unsupported order type"));
                order = null;
            }
            if (!violations.isEmpty() || order == null) {
                return new OrderParseResult.Invalid(possibleOrderId, violations);
            }
            return new OrderParseResult.Parsed(order);
        } catch (IllegalArgumentException exception) {
            violations.add(new InputViolation("order", "INVALID_ORDER", safeMessage(exception)));
            return new OrderParseResult.Invalid(possibleOrderId, violations);
        }
    }

    private Order parseOnRamp(
            JsonNode root, String orderId, String customerId, String asset, String network,
            String note, List<InputViolation> violations
    ) {
        BigDecimal fiat = positiveDecimal(root, "fiat_amount_usd", violations);
        BigDecimal crypto = positiveDecimal(root, "quoted_crypto_amount", violations);
        Instant expiresAt = instant(root, "quote_expires_at", violations);
        String address = requiredText(root, "destination_address", violations);
        String status = requiredText(root, "fiat_status", violations).toLowerCase(Locale.ROOT);
        FundingEvidence.Status fundingStatus;
        if ("received".equals(status) || "confirmed".equals(status)) {
            fundingStatus = FundingEvidence.Status.CONFIRMED;
        } else if ("pending".equals(status)) {
            fundingStatus = FundingEvidence.Status.PENDING;
        } else if ("reversed".equals(status)) {
            fundingStatus = FundingEvidence.Status.REVERSED;
        } else if ("failed".equals(status)) {
            fundingStatus = FundingEvidence.Status.FAILED;
        } else {
            fundingStatus = FundingEvidence.Status.UNKNOWN;
        }
        requireNoViolations(violations);
        return new OnRampOrder(orderId, customerId, asset, network, fiat, crypto, expiresAt,
                fundingStatus, address, counterparty(root.path("counterparty")), note);
    }

    private Order parseOffRamp(
            JsonNode root, String orderId, String customerId, String asset, String network,
            String note, List<InputViolation> violations
    ) {
        BigDecimal quoted = positiveDecimal(root, "quoted_crypto_amount", violations);
        Instant expiresAt = instant(root, "quote_expires_at", violations);
        JsonNode depositNode = requiredObject(root, "deposit", violations);
        JsonNode payoutNode = requiredObject(root, "payout", violations);

        String txHash = requiredText(depositNode, "tx_hash", "deposit.tx_hash", violations);
        String fromAddress = requiredText(depositNode, "from_address", "deposit.from_address", violations);
        String observedNetwork = optionalText(depositNode, "network").orElse(network).toUpperCase(Locale.ROOT);
        String transferIndex = optionalText(depositNode, "transfer_index").orElse("0");
        BigDecimal observedAmount = positiveDecimal(depositNode, "observed_amount", "deposit.observed_amount", violations);
        int confirmations = nonNegativeInteger(depositNode, "confirmations", "deposit.confirmations", violations);

        String bankName = requiredText(payoutNode, "bank_account_name", "payout.bank_account_name", violations);
        String currency = requiredText(payoutNode, "currency", "payout.currency", violations).toUpperCase(Locale.ROOT);
        BigDecimal payoutAmount = positiveDecimal(payoutNode, "amount", "payout.amount", violations);
        requireNoViolations(violations);

        DepositReference deposit = new DepositReference(txHash, transferIndex, fromAddress,
                observedNetwork, observedAmount, confirmations, FundingEvidence.Status.CONFIRMED);
        return new OffRampOrder(orderId, customerId, asset, network, quoted, expiresAt,
                deposit, new BankPayout(bankName, currency, payoutAmount),
                counterparty(root.path("counterparty")), note);
    }

    private Order parseWithdrawal(
            JsonNode root, String orderId, String customerId, String asset, String network,
            String note, List<InputViolation> violations
    ) {
        BigDecimal amount = positiveDecimal(root, "amount", violations);
        String address = requiredText(root, "destination_address", violations);
        requireNoViolations(violations);
        return new WithdrawalOrder(orderId, customerId, asset, network, amount, address,
                counterparty(root.path("counterparty")), note);
    }

    private CounterpartyInfo counterparty(JsonNode node) {
        if (node == null || !node.isObject()) {
            // Exercise contract: omitted counterparty means a direct customer transfer.
            // An object with missing is_vasp remains UNKNOWN and fails closed.
            return CounterpartyInfo.directCustomer();
        }
        CounterpartyInfo.VaspStatus status = node.has("is_vasp") && node.get("is_vasp").isBoolean()
                ? node.get("is_vasp").booleanValue()
                    ? CounterpartyInfo.VaspStatus.VASP
                    : CounterpartyInfo.VaspStatus.NOT_VASP
                : CounterpartyInfo.VaspStatus.UNKNOWN;
        String vaspName = optionalText(node, "vasp_name").orElse("");
        boolean originatorComplete = presentStructuredInfo(node.get("originator_info"));
        boolean beneficiaryComplete = presentStructuredInfo(node.get("beneficiary_info"));
        return new CounterpartyInfo(status, vaspName, originatorComplete, beneficiaryComplete);
    }

    private boolean presentStructuredInfo(JsonNode node) {
        return node != null && !node.isNull()
                && ((node.isObject() && !node.isEmpty()) || (node.isTextual() && !node.asText().trim().isEmpty()));
    }

    private JsonNode requiredObject(JsonNode root, String field, List<InputViolation> violations) {
        JsonNode node = root.get(field);
        if (node == null || !node.isObject()) {
            violations.add(new InputViolation(field, "MISSING_REQUIRED_FIELD", "Required object is missing"));
            return objectMapper.createObjectNode();
        }
        return node;
    }

    private String requiredText(JsonNode root, String field, List<InputViolation> violations) {
        return requiredText(root, field, field, violations);
    }

    private String requiredText(JsonNode root, String field, String path, List<InputViolation> violations) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().trim().isEmpty()) {
            violations.add(new InputViolation(path, "MISSING_REQUIRED_FIELD", "Required text is missing"));
            return "";
        }
        return node.asText().trim();
    }

    private Optional<String> optionalText(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node != null && node.isTextual() && !node.asText().trim().isEmpty()
                ? Optional.of(node.asText().trim()) : Optional.<String>empty();
    }

    private BigDecimal positiveDecimal(JsonNode root, String field, List<InputViolation> violations) {
        return positiveDecimal(root, field, field, violations);
    }

    private BigDecimal positiveDecimal(
            JsonNode root, String field, String path, List<InputViolation> violations
    ) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            violations.add(new InputViolation(path, "INVALID_DECIMAL", "A decimal number is required"));
            return BigDecimal.ZERO;
        }
        BigDecimal value = node.decimalValue();
        if (value.signum() <= 0) {
            violations.add(new InputViolation(path, "NON_POSITIVE_AMOUNT", "Amount must be positive"));
        }
        if (value.precision() > 38) {
            violations.add(new InputViolation(path, "INVALID_DECIMAL", "Amount precision exceeds 38 digits"));
        }
        return value;
    }

    private int nonNegativeInteger(
            JsonNode root, String field, String path, List<InputViolation> violations
    ) {
        JsonNode node = root.get(field);
        if (node == null || !node.canConvertToInt() || node.intValue() < 0) {
            violations.add(new InputViolation(path, "INVALID_INTEGER", "A non-negative integer is required"));
            return 0;
        }
        return node.intValue();
    }

    private Instant instant(JsonNode root, String field, List<InputViolation> violations) {
        String value = requiredText(root, field, violations);
        if (value.trim().isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            violations.add(new InputViolation(field, "INVALID_TIMESTAMP", "Timestamp must use ISO-8601 UTC format"));
            return Instant.EPOCH;
        }
    }

    private void requireNoViolations(List<InputViolation> violations) {
        if (!violations.isEmpty()) {
            throw new InvalidFieldsException();
        }
    }

    private OrderParseResult.Invalid invalid(
            Optional<String> orderId, String field, String code, String message
    ) {
        return new OrderParseResult.Invalid(orderId,
                java.util.Collections.singletonList(new InputViolation(field, code, message)));
    }

    private String safeMessage(IllegalArgumentException exception) {
        return exception instanceof InvalidFieldsException
                ? "One or more required fields are invalid"
                : Optional.ofNullable(exception.getMessage()).orElse("Invalid order");
    }

    private static final class InvalidFieldsException extends IllegalArgumentException {
    }
}
