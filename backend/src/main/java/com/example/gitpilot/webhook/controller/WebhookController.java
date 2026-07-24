package com.example.gitpilot.webhook.controller;

import com.example.gitpilot.webhook.handler.GithubWebhookHandler;
import com.example.gitpilot.webhook.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Webhooks", description = "Endpoints for receiving third-party event notifications")
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;
    private final List<GithubWebhookHandler> webhookHandlers;

    public WebhookController(WebhookService webhookService, List<GithubWebhookHandler> webhookHandlers) {
        this.webhookService = webhookService;
        this.webhookHandlers = webhookHandlers;
    }

    @Operation(summary = "GitHub Webhook Receiver", description = "Listens for various events directly from GitHub to trigger real-time synchronization")
    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-hub-signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "x-github-event", defaultValue = "push") String eventType
    ) {
        log.info("Received GitHub Webhook. Event Type: {}", eventType);

        // 1. Verify signature
        if (!webhookService.verifySignature(payload, signatureHeader)) {
            log.error("GitHub webhook signature verification failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 2. Delegate to appropriate strategy handler
        try {
            for (GithubWebhookHandler handler : webhookHandlers) {
                if (handler.supports(eventType)) {
                    handler.handle(payload);
                    return ResponseEntity.ok("Event '" + eventType + "' processed successfully");
                }
            }

            // No matching handler found
            log.info("No matching handler found for GitHub webhook event: {}", eventType);
            return ResponseEntity.ok("Event '" + eventType + "' ignored (no handler configured)");
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error("Bad webhook payload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid webhook payload: " + e.getMessage());
        } catch (Exception e) {
            log.error("Internal processing error during webhook handler: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed: " + e.getMessage());
        }
    }
}
