package com.jason.yang.asset.application.evaluation;

import java.nio.file.Path;

public final class EvaluationCommand {
    private final Path materialsDirectory;
    private final Path goldenCases;
    private final Path reportFile;

    public EvaluationCommand(Path materialsDirectory, Path goldenCases, Path reportFile) {

        this.materialsDirectory = materialsDirectory;
        this.goldenCases = goldenCases;
        this.reportFile = reportFile;
    }

    public Path materialsDirectory() {
        return materialsDirectory;
    }

    public Path getMaterialsDirectory() {
        return materialsDirectory;
    }

    public Path goldenCases() {
        return goldenCases;
    }

    public Path getGoldenCases() {
        return goldenCases;
    }

    public Path reportFile() {
        return reportFile;
    }

    public Path getReportFile() {
        return reportFile;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvaluationCommand)) return false;
        EvaluationCommand that = (EvaluationCommand) other;
        return java.util.Objects.equals(materialsDirectory, that.materialsDirectory)
                && java.util.Objects.equals(goldenCases, that.goldenCases)
                && java.util.Objects.equals(reportFile, that.reportFile);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(materialsDirectory, goldenCases, reportFile);
    }

    @Override
    public String toString() {
        return "EvaluationCommand{" + "materialsDirectory=" + materialsDirectory + ", goldenCases=" + goldenCases + ", reportFile=" + reportFile + "}";
    }


}
