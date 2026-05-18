package com.example.petlife.service.slack;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SlackRequestVerifierTest {

    @Test
    void validSignatureShouldPass() throws Exception {
        SlackRequestVerifier verifier = new SlackRequestVerifier();
        ReflectionTestUtils.setField(verifier, "signingSecret", "secret123");

        String body = "{\"type\":\"event_callback\"}";
        String ts = String.valueOf(System.currentTimeMillis() / 1000L);
        String sig = "v0=" + hmac("secret123", "v0:" + ts + ":" + body);

        assertTrue(verifier.isValid(ts, sig, body));
    }

    @Test
    void invalidSignatureShouldFail() {
        SlackRequestVerifier verifier = new SlackRequestVerifier();
        ReflectionTestUtils.setField(verifier, "signingSecret", "secret123");

        String body = "{\"type\":\"event_callback\"}";
        String ts = String.valueOf(System.currentTimeMillis() / 1000L);

        assertFalse(verifier.isValid(ts, "v0=bad", body));
    }

    private String hmac(String secret, String base) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
