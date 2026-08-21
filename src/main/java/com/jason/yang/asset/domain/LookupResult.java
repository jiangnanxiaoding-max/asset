package com.jason.yang.asset.domain;

import java.util.Objects;
import java.util.Optional;

public interface LookupResult<T> {

    default Optional<T> value() {
        if (this instanceof Found) {
            @SuppressWarnings("unchecked")
            Found<T> found = (Found<T>) this;
            return Optional.of(found.data());
        }
        return Optional.empty();
    }

    default boolean isFound() {
        return this instanceof Found<?>;
    }

    static <T> LookupResult<T> found(T data, String evidenceRef) {
        return new Found<>(data, evidenceRef);
    }

    static <T> LookupResult<T> notFound(String code) {
        return new NotFound<>(code);
    }

    static <T> LookupResult<T> unavailable(String code, boolean retryable) {
        return new Unavailable<>(code, retryable);
    }

    static <T> LookupResult<T> conflict(String code) {
        return new Conflict<>(code);
    }

    static <T> LookupResult<T> notApplicable() {
        return new NotApplicable<>();
    }static final class Found<T> implements LookupResult<T> {
    private final T data;
    private final String evidenceRef;

    public T data() {
        return data;
    }

    public T getData() {
        return data;
    }

    public String evidenceRef() {
        return evidenceRef;
    }

    public String getEvidenceRef() {
        return evidenceRef;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Found)) return false;
        Found that = (Found) other;
        return java.util.Objects.equals(data, that.data)
                && java.util.Objects.equals(evidenceRef, that.evidenceRef);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(data, evidenceRef);
    }

    @Override
    public String toString() {
        return "Found{" + "data=" + data + ", evidenceRef=" + evidenceRef + "}";
    }


        public Found(T data, String evidenceRef) {
            Objects.requireNonNull(data);
            evidenceRef = evidenceRef == null ? "" : evidenceRef;
        

            this.data = data;

            this.evidenceRef = evidenceRef;

        }
    }static final class NotFound<T> implements LookupResult<T> {
    private final String code;

    public NotFound(String code) {

        this.code = code;
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NotFound)) return false;
        NotFound that = (NotFound) other;
        return java.util.Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code);
    }

    @Override
    public String toString() {
        return "NotFound{" + "code=" + code + "}";
    }

}static final class Unavailable<T> implements LookupResult<T> {
    private final String code;
    private final boolean retryable;

    public Unavailable(String code, boolean retryable) {

        this.code = code;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean getRetryable() {
        return retryable;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Unavailable)) return false;
        Unavailable that = (Unavailable) other;
        return java.util.Objects.equals(code, that.code)
                && retryable == that.retryable;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code, retryable);
    }

    @Override
    public String toString() {
        return "Unavailable{" + "code=" + code + ", retryable=" + retryable + "}";
    }

}static final class Conflict<T> implements LookupResult<T> {
    private final String code;

    public Conflict(String code) {

        this.code = code;
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Conflict)) return false;
        Conflict that = (Conflict) other;
        return java.util.Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Conflict{" + "code=" + code + "}";
    }

}static final class NotApplicable<T> implements LookupResult<T> {

    public NotApplicable() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NotApplicable)) return false;
        NotApplicable that = (NotApplicable) other;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash();
    }

    @Override
    public String toString() {
        return "NotApplicable{" + "}";
    }

}
}
