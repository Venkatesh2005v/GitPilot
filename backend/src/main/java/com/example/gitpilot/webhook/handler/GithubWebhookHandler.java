package com.example.gitpilot.webhook.handler;

public interface GithubWebhookHandler {
    /**
     * Checks if this handler supports the given GitHub event type (e.g. "push", "ping").
     *
     * @param eventType the event type header received
     * @return true if supported, false otherwise
     */
    boolean supports(String eventType);

    /**
     * Processes the webhook payload.
     *
     * @param payload the raw JSON request body
     * @throws Exception if parsing or processing fails
     */
    void handle(String payload) throws Exception;
}
