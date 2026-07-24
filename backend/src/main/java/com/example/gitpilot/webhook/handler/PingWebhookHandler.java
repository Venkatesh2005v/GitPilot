package com.example.gitpilot.webhook.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PingWebhookHandler implements GithubWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(PingWebhookHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "ping".equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(String payload) throws Exception {
        log.info("Processing GitHub Connection Ping webhook. Connection verified.");
    }
}
