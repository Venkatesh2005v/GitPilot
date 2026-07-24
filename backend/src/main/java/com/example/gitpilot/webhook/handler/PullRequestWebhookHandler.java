package com.example.gitpilot.webhook.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PullRequestWebhookHandler implements GithubWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(PullRequestWebhookHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "pull_request".equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(String payload) throws Exception {
        log.info("Received GitHub Pull Request event. Handler registered for future pull requests analysis features.");
    }
}
