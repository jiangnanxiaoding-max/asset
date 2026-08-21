package com.jason.yang.asset.application.input;

/** Converts an untrusted queue line into a validated domain order. */
public interface OrderParser {
    OrderParseResult parse(RawOrderEnvelope envelope);
}
