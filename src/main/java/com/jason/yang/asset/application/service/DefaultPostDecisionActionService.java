package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.model.ExecutionMode;
import com.jason.yang.asset.application.model.SideEffectSummary;
import com.jason.yang.asset.application.port.CaseManagementPort;
import com.jason.yang.asset.application.port.FundsExecutionGateway;
import com.jason.yang.asset.application.port.PostDecisionActionPort;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.TriageCase;
import com.jason.yang.asset.domain.TriageDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Coordinates only post-audit side effects; it cannot change the domain decision. */
public final class DefaultPostDecisionActionService implements PostDecisionActionPort {
    private static final Logger log = LoggerFactory.getLogger(DefaultPostDecisionActionService.class);
    private final ExecutionMode mode;
    private final CaseManagementPort caseManagementPort;
    private final FundsExecutionGateway fundsExecutionGateway;

    public DefaultPostDecisionActionService(
            ExecutionMode mode,
            CaseManagementPort caseManagementPort,
            FundsExecutionGateway fundsExecutionGateway
    ) {
        this.mode = Objects.requireNonNull(mode);
        this.caseManagementPort = Objects.requireNonNull(caseManagementPort);
        this.fundsExecutionGateway = Objects.requireNonNull(fundsExecutionGateway);
    }

    @Override
    public SideEffectSummary handle(TriageCase triageCase) {
        if (triageCase.status() != TriageCase.Status.AUDITED) {
            throw new IllegalStateException("Post-decision actions require an audited aggregate");
        }
        List<String> records = new ArrayList<String>();
        TriageDecision decision = triageCase.decision();
        String key = decision.decisionId().value();

        String caseType = caseType(decision.disposition(), decision.reasonCodes());
        if (caseType != null) {
            String caseId = caseManagementPort.openCase("case:" + key, caseType, decision);
            records.add("case:" + caseId);
            log.info("post-decision case opened orderId={} caseType={} caseId={}",
                    decision.orderId().value(), caseType, caseId);
        }

        if (triageCase.fundsMovementEligible() && mode == ExecutionMode.SIMULATED_EXECUTION) {
            FundsExecutionGateway.ExecutionRecord execution = fundsExecutionGateway.execute("funds:" + key, triageCase);
            records.add("execution:" + execution.status() + ":" + execution.externalReference());
            log.info("simulated funds execution recorded orderId={} status={}",
                    decision.orderId().value(), execution.status());
        }
        return new SideEffectSummary(records);
    }

    private String caseType(Disposition disposition, java.util.List<ReasonCode> reasons) {
        switch (disposition) {
            case FREEZE_COMPLIANCE:
            case REJECT_ESCALATE: return "COMPLIANCE";
            case OPS_RECOVERY: return "OPERATIONS";
            case REFUND_REVIEW: return "REFUND_REVIEW";
            case MANUAL_REVIEW:
            case REQUOTE: return "MANUAL_REVIEW";
            case HOLD: return reasons.stream().anyMatch(this::complianceReason)
                    ? "COMPLIANCE" : "MANUAL_REVIEW";
            default: return null;
        }
    }

    private boolean complianceReason(ReasonCode reason) {
        switch (reason) {
            case ADDRESS_SANCTIONED:
            case ADDRESS_HIGH_RISK:
            case ADDRESS_MEDIUM_RISK:
            case BANK_NAME_MISMATCH:
            case TRAVEL_RULE_INFO_MISSING:
            case CUSTOMER_NOT_ACTIVE: return true;
            default: return false;
        }
    }
}
