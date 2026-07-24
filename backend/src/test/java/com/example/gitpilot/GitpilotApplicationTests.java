package com.example.gitpilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"GEMINI_API_KEY=mock-key",
		"GROQ_API_KEY=mock-key",
		"OPENROUTER_API_KEY=mock-key",
		"GITHUB_CLIENT_ID=mock-client-id",
		"GITHUB_CLIENT_SECRET=mock-client-secret",
		"GITHUB_WEBHOOK_SECRET=mock-webhook-secret"
})
class GitpilotApplicationTests {

	@Test
	void contextLoads() {
	}

}
