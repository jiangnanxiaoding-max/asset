package com.jason.yang.asset.adapter.input;

import com.jason.yang.asset.application.input.OrderParseResult;
import com.jason.yang.asset.application.input.RawOrderEnvelope;
import com.jason.yang.asset.domain.OffRampOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonOrderParserTest {
    private final JacksonOrderParser parser = new JacksonOrderParser();

    @Test
    void parsesOffRampAndPreservesEmbeddedDepositEvidence() {
        String json = "{\"order_id\":\"O-1\",\"type\":\"off_ramp\",\"customer_id\":\"c1\",\"asset\":\"USDT\"," +
                "\"network\":\"TRC20\",\"quoted_crypto_amount\":500,\"quote_expires_at\":\"2026-07-28T12:05:00Z\"," +
                "\"deposit\":{\"tx_hash\":\"0xa1\",\"from_address\":\"0x1\",\"confirmations\":25,\"observed_amount\":500}," +
                "\"payout\":{\"bank_account_name\":\"Alice\",\"currency\":\"USD\",\"amount\":495}}";

        OrderParseResult result = parser.parse(envelope(json));

        assertTrue(result instanceof OrderParseResult.Parsed);
        OrderParseResult.Parsed parsed = (OrderParseResult.Parsed) result;
        assertTrue(parsed.order() instanceof OffRampOrder);
        OffRampOrder order = (OffRampOrder) parsed.order();
        assertEquals(25, order.deposit().confirmations());
        assertEquals("TRC20", order.deposit().observedNetwork());
    }

    @Test
    void duplicateJsonFieldIsRejected() {
        String json = "{\"order_id\":\"O-1\",\"order_id\":\"O-2\",\"type\":\"withdrawal\",\"customer_id\":\"c1\"," +
                "\"asset\":\"BTC\",\"network\":\"BTC\",\"amount\":1,\"destination_address\":\"0x1\"}";

        OrderParseResult parseResult = parser.parse(envelope(json));
        assertTrue(parseResult instanceof OrderParseResult.Invalid);
        OrderParseResult.Invalid result = (OrderParseResult.Invalid) parseResult;

        assertTrue(result.violations().stream().anyMatch(v -> v.code().equals("MALFORMED_JSON")));
    }

    @Test
    void nonPositiveMoneyIsRejected() {
        String json = "{\"order_id\":\"O-1\",\"type\":\"withdrawal\",\"customer_id\":\"c1\"," +
                "\"asset\":\"BTC\",\"network\":\"BTC\",\"amount\":0,\"destination_address\":\"0x1\"}";

        OrderParseResult parseResult = parser.parse(envelope(json));
        assertTrue(parseResult instanceof OrderParseResult.Invalid);
        OrderParseResult.Invalid result = (OrderParseResult.Invalid) parseResult;

        assertTrue(result.violations().stream().anyMatch(v -> v.code().equals("NON_POSITIVE_AMOUNT")));
    }

    private RawOrderEnvelope envelope(String json) {
        return new RawOrderEnvelope("test", 1, json, "hash", Instant.parse("2026-07-28T12:00:00Z"));
    }
}
