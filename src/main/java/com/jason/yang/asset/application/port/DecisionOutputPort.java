package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.batch.TriageOutputRecord;

import java.nio.file.Path;

public interface DecisionOutputPort {
    void initialize(Path outputFile);

    void append(Path outputFile, TriageOutputRecord record);
}
