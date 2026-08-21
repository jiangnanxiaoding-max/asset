package com.jason.yang.asset.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for the exception-triage lifecycle.
 *
 * <p>It guarantees that a decision can only be made after investigation starts,
 * an audit can only be attached to a decided case, and funds movement is never
 * eligible before the decision has been durably audited.</p>
 */
public final class TriageCase {
    private final OrderId id;
    private final Order order;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private Status status;
    private TriageDecision decision;
    private AuditId auditId;

    private TriageCase(Order order) {
        this.order = Objects.requireNonNull(order);
        this.id = order.identity();
        this.status = Status.RECEIVED;
    }

    public static TriageCase open(Order order) {
        return new TriageCase(order);
    }

    /** Starts fact investigation. A completed or already-started case cannot restart. */
    public void beginInvestigation() {
        requireStatus(Status.RECEIVED, "investigation can only start from RECEIVED");
        status = Status.INVESTIGATING;
    }

    /** Records the single policy decision and emits domain events for downstream work. */
    public void recordDecision(TriageDecision decision) {
        requireStatus(Status.INVESTIGATING, "decision requires an active investigation");
        Objects.requireNonNull(decision, "decision");
        if (!id.equals(decision.orderId())) {
            throw new IllegalArgumentException("decision belongs to a different order");
        }
        this.decision = decision;
        this.status = Status.DECIDED;
        domainEvents.add(new OrderTriaged(id, decision.disposition(), decision.evaluatedAt()));
        if (decision.disposition() == Disposition.FREEZE_COMPLIANCE) {
            domainEvents.add(new ComplianceFreezeRequired(
                    id,
                    decision.reasonCodes(),
                    decision.evaluatedAt()
            ));
        }
    }

    /** Attaches the immutable audit reference; this is required before execution eligibility. */
    public void markAudited(String auditId) {
        requireStatus(Status.DECIDED, "audit can only be attached after a decision");
        if (auditId == null || auditId.trim().isEmpty()) {
            throw new IllegalArgumentException("auditId must not be blank");
        }
        this.auditId = new AuditId(auditId);
        this.status = Status.AUDITED;
    }

    public boolean fundsMovementEligible() {
        return status == Status.AUDITED
                && decision != null
                && decision.disposition() == Disposition.AUTO_COMPLETE
                && decision.fundsMovementAllowed();
    }

    public OrderId id() {
        return id;
    }

    public Order order() {
        return order;
    }

    public Status status() {
        return status;
    }

    public TriageDecision decision() {
        return decision;
    }

    public AuditId auditId() {
        return auditId;
    }

    public List<DomainEvent> domainEvents() {
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(domainEvents));
    }

    private void requireStatus(Status expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message + "; current status=" + status);
        }
    }

    public enum Status {
        RECEIVED,
        INVESTIGATING,
        DECIDED,
        AUDITED
    }
}
