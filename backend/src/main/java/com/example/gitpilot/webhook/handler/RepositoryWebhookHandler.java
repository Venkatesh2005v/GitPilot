package com.example.gitpilot.webhook.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RepositoryWebhookHandler implements GithubWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(RepositoryWebhookHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "repository".equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(String payload) throws Exception {
        log.info("Received GitHub repository lifecycle event. Handler registered for future database expansion.");
    }
}
