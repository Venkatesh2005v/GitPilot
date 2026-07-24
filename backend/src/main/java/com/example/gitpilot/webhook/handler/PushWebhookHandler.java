package com.example.gitpilot.webhook.handler;

import com.example.gitpilot.webhook.service.WebhookService;
import org.springframework.stereotype.Component;

@Component
public class PushWebhookHandler implements GithubWebhookHandler {

    private final WebhookService webhookService;

    public PushWebhookHandler(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public boolean supports(String eventType) {
        return "push".equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(String payload) throws Exception {
        webhookService.processPushWebhook(payload);
    }
}
