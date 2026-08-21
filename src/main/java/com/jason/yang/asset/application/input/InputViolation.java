package com.jason.yang.asset.application.input;

/** Sanitized input validation failure safe to place in decision output. */
public final class InputViolation {
    private final String field;
    private final String code;
    private final String message;

    public InputViolation(String field, String code, String message) {

        this.field = field;
        this.code = code;
        this.message = message;
    }

    public String field() {
        return field;
    }

    public String getField() {
        return field;
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String message() {
        return message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InputViolation)) return false;
        InputViolation that = (InputViolation) other;
        return java.util.Objects.equals(field, that.field)
                && java.util.Objects.equals(code, that.code)
                && java.util.Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(field, code, message);
    }

    @Override
    public String toString() {
        return "InputViolation{" + "field=" + field + ", code=" + code + ", message=" + message + "}";
    }


}
