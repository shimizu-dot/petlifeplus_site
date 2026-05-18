package com.example.petlife.service.slack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SlackRequestVerifier {

    private static final long ALLOWED_SKEW_SECONDS = 60L * 5L;

    @Value("${slack.signing-secret:}")
    private String signingSecret;

    public boolean isValid(String timestampHeader, String signatureHeader, String rawBody) {
        if (signingSecret == null || signingSecret.isBlank()) {
            return false;
        }
        if (timestampHeader == null || signatureHeader == null || rawBody == null) {
            return false;
        }

        long now = System.currentTimeMillis() / 1000L;
        long ts;
        try {
            ts = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(now - ts) > ALLOWED_SKEW_SECONDS) {
            return false;
        }

        String base = "v0:" + timestampHeader + ":" + rawBody;
        String expected = "v0=" + hmacSha256Hex(signingSecret, base);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed to compute hmac", e);
        }
    }
}
