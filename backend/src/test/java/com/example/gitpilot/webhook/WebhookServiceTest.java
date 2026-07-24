package com.example.gitpilot.webhook;

import com.example.gitpilot.webhook.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(null, null, null, null);
    }


    @Test
    void testVerifySignatureNoSecret() {
        ReflectionTestUtils.setField(webhookService, "secret", "");
        assertTrue(webhookService.verifySignature("payload", "any-sig"));
    }

    @Test
    void testVerifySignatureWithSecretMatching() {
        ReflectionTestUtils.setField(webhookService, "secret", "my-secret");
        
        // Calculated signature for payload "hello" and secret "my-secret"
        // HMAC-SHA256("my-secret", "hello") = 7c1ce32402750db1149385ac20603beeaca8909906d1e81c08f4f5c7db8fbe94
        String signatureHeader = "sha256=7c1ce32402750db1149385ac20603beeaca8909906d1e81c08f4f5c7db8fbe94";
        
        assertTrue(webhookService.verifySignature("hello", signatureHeader));
    }


    @Test
    void testVerifySignatureWithSecretMismatch() {
        ReflectionTestUtils.setField(webhookService, "secret", "my-secret");
        String signatureHeader = "sha256=mismatched-sig-hash-value-here";
        
        assertFalse(webhookService.verifySignature("hello", signatureHeader));
    }
}
