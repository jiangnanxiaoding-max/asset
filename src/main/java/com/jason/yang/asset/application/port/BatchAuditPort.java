package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.input.InputViolation;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.ReasonCode;

import java.util.List;

/** Audits lines that cannot enter the normal domain pipeline. */
public interface BatchAuditPort {
    String appendRejectedLine(
            String runId,
            long sourcePosition,
            String payloadSha256,
            String orderId,
            Disposition disposition,
            List<ReasonCode> reasonCodes,
            List<InputViolation> violations
    );
}
