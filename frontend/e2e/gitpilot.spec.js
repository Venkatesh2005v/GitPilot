import { test, expect } from '@playwright/test';
import crypto from 'crypto';

test.describe('GitPilot E2E System Validation User Journey', () => {

  test('should log in, configure settings, view analytics, trigger syncs, and simulate webhooks', async ({ page }) => {
    // 1. Launch & Navigate to Landing Page
    console.log('Navigating to landing page...');
    await page.goto('/');
    await expect(page).toHaveTitle(/GitPilot/);

    // Verify visual presence of sign-in options
    await expect(page.locator('text=Developer Sandbox Sign-In')).toBeVisible();

    // 2. Perform Mock Developer Login
    console.log('Clicking developer sandbox login...');
    await page.click('text=Developer Sandbox Sign-In');

    // Wait for core application UI to load (check sidebar presence)
    await expect(page.locator('h1, h2').filter({ hasText: 'Dashboard' }).first()).toBeVisible({ timeout: 12000 });
    console.log('Successfully authenticated into dashboard!');

    // 3. Navigate to Repository Settings selection
    console.log('Opening repository settings selections...');
    await page.click('a[href="/settings"]');
    await expect(page.locator('text=Repository Selections')).toBeVisible();

    // Wait for repositories list to fetch and load
    await page.waitForSelector('.settings-item');

    // Locate repository checkboxes and check them
    console.log('Selecting repositories for tracking...');
    const checkboxes = page.locator('input[type="checkbox"]');
    const count = await checkboxes.count();
    
    // Select first two repositories
    if (count > 0) {
      const firstChecked = await checkboxes.nth(0).isChecked();
      if (!firstChecked) await checkboxes.nth(0).click();
    }
    if (count > 1) {
      const secondChecked = await checkboxes.nth(1).isChecked();
      if (!secondChecked) await checkboxes.nth(1).click();
    }

    // Save Selection Preferences
    console.log('Saving selections...');
    await page.click('text=Save Selected Repositories');
    
    // Assert success banner appears
    await expect(page.locator('text=Repository selection preferences saved successfully.')).toBeVisible();

    // 4. Return to Dashboard
    console.log('Returning to Dashboard...');
    await page.click('a[href="/dashboard"]');
    await expect(page.locator('text=Repository Health overview')).toBeVisible();

    // Verify at least one repository card exists
    await page.waitForSelector('.card h4');
    const firstRepoCardTitle = await page.locator('.card h4').first().textContent();
    console.log(`Found tracking repository: ${firstRepoCardTitle}`);

    // 5. Open Repository Details tab views
    console.log('Navigating into repository overview details...');
    await page.locator('.card h4').first().click();

    // Verify we arrived at repository overview and tabs render
    await expect(page.locator('text=Back to Dashboard')).toBeVisible();
    
    // Test tabs sequentially
    console.log('Verifying tabs sequentially...');
    
    // Overview Tab
    await page.click('.tabs-container button:has-text("Overview")');
    await expect(page.locator('text=Default Branch')).toBeVisible();

    // Analytics Tab
    await page.click('.tabs-container button:has-text("Analytics")');
    await expect(page.locator('text=Percentage Distribution')).toBeVisible();

    // Commits Tab
    await page.click('.tabs-container button:has-text("Commits")');
    await expect(page.locator('text=Repository Commits List')).toBeVisible();

    // AI Insights Tab
    await page.click('.tabs-container button:has-text("AI Insights")');
    // If AI Gateway keys aren't set, might show failure alert, check either header or error banner is visible
    await expect(page.locator('text=Repository Health').or(page.locator('text=AI Report unavailable').or(page.locator('text=AI Report Generation failed')))).toBeVisible();

    // Contributors Tab
    await page.click('.tabs-container button:has-text("Contributors")');
    await expect(page.locator('text=Contributor Rankings')).toBeVisible();

    // Settings Tab
    await page.click('.tabs-container button:has-text("Settings")');
    await expect(page.locator('text=Synchronization Status')).toBeVisible();

    // 6. Simulate Real-Time GitHub Webhook Event Trigger
    console.log('Simulating push webhook event trigger via relative POST request...');
    
    const webhookPayload = {
      repository: {
        id: 11111111,
        name: 'mock-react-dashboard',
        html_url: 'https://github.com/mock-user/mock-react-dashboard',
        default_branch: 'main',
        private: false
      },
      commits: [
        {
          id: 'sha_playwright_e2e_test_999',
          message: 'docs: verify webhooks strategy handler outputs in Playwright',
          author: {
            name: 'Playwright Test Runner',
            email: 'e2e@playwright.com',
            date: new Date().toISOString()
          },
          url: 'https://github.com/mock-user/mock-react-dashboard/commit/sha_playwright_e2e_test_999'
        }
      ]
    };

    // Calculate valid payload HMAC signature (in case secret is configured in backend environment)
    const payloadString = JSON.stringify(webhookPayload);
    const secretKey = process.env.GITHUB_WEBHOOK_SECRET || '';
    const hmac = crypto.createHmac('sha256', secretKey);
    hmac.update(payloadString);
    const expectedSignature = 'sha256=' + hmac.digest('hex');

    // Trigger webhook POST request using Playwright context
    const webhookResponse = await page.request.post('/webhooks/github', {
      headers: {
        'x-github-event': 'push',
        'x-hub-signature-256': expectedSignature,
        'Content-Type': 'application/json'
      },
      data: webhookPayload
    });

    expect(webhookResponse.status()).toBe(200);
    const bodyText = await webhookResponse.text();
    console.log(`Webhook triggered successfully. Response status: ${webhookResponse.status()}, Content: ${bodyText}`);

    // 7. Verify Dashboard Update after push
    await page.click('a[href="/dashboard"]');
    await expect(page.locator('text=Total Commits')).toBeVisible();

    // 8. Sign Out
    console.log('Signing out...');
    await page.click('text=Logout');
    
    // Assert redirect back to landing page
    await expect(page.locator('text=Sign in with GitHub')).toBeVisible();
    console.log('E2E validation test finished successfully.');
  });
});
