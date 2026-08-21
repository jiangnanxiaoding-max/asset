package com.jason.yang.asset.domain;

public final class CounterpartyInfo {
    private final VaspStatus vaspStatus;
    private final String vaspName;
    private final boolean originatorComplete;
    private final boolean beneficiaryComplete;

    public VaspStatus vaspStatus() {
        return vaspStatus;
    }

    public VaspStatus getVaspStatus() {
        return vaspStatus;
    }

    public String vaspName() {
        return vaspName;
    }

    public String getVaspName() {
        return vaspName;
    }

    public boolean originatorComplete() {
        return originatorComplete;
    }

    public boolean getOriginatorComplete() {
        return originatorComplete;
    }

    public boolean beneficiaryComplete() {
        return beneficiaryComplete;
    }

    public boolean getBeneficiaryComplete() {
        return beneficiaryComplete;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CounterpartyInfo)) return false;
        CounterpartyInfo that = (CounterpartyInfo) other;
        return java.util.Objects.equals(vaspStatus, that.vaspStatus)
                && java.util.Objects.equals(vaspName, that.vaspName)
                && originatorComplete == that.originatorComplete
                && beneficiaryComplete == that.beneficiaryComplete;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(vaspStatus, vaspName, originatorComplete, beneficiaryComplete);
    }

    @Override
    public String toString() {
        return "CounterpartyInfo{" + "vaspStatus=" + vaspStatus + ", vaspName=" + vaspName + ", originatorComplete=" + originatorComplete + ", beneficiaryComplete=" + beneficiaryComplete + "}";
    }


    public CounterpartyInfo(VaspStatus vaspStatus, String vaspName, boolean originatorComplete, boolean beneficiaryComplete) {
        vaspStatus = vaspStatus == null ? VaspStatus.UNKNOWN : vaspStatus;
        vaspName = vaspName == null ? "" : vaspName;
    

        this.vaspStatus = vaspStatus;

        this.vaspName = vaspName;

        this.originatorComplete = originatorComplete;

        this.beneficiaryComplete = beneficiaryComplete;

    }

    public static CounterpartyInfo unknown() {
        return new CounterpartyInfo(VaspStatus.UNKNOWN, "", false, false);
    }

    public static CounterpartyInfo directCustomer() {
        return new CounterpartyInfo(VaspStatus.NOT_VASP, "", false, false);
    }

    public enum VaspStatus {
        VASP,
        NOT_VASP,
        UNKNOWN
    }
}
