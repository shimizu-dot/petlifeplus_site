package com.example.petlife.service.line;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class LineRequestVerifier {

    @Value("${line.channel-secret:}")
    private String channelSecret;

    public boolean isValid(String signatureHeader, String rawBody) {
        if (channelSecret == null || channelSecret.isBlank()) {
            return false;
        }
        if (signatureHeader == null || rawBody == null) {
            return false;
        }

        String expected = computeSignature(channelSecret, rawBody);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String computeSignature(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("failed to compute LINE signature", e);
        }
    }
}
