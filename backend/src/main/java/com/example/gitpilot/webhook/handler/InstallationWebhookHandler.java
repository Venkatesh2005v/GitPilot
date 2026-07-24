package com.example.gitpilot.webhook.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstallationWebhookHandler implements GithubWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(InstallationWebhookHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "installation".equalsIgnoreCase(eventType) || "integration_installation".equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(String payload) throws Exception {
        log.info("Received GitHub Integration/Installation lifecycle event. Handler registered for future multi-tenant setups.");
    }
}
