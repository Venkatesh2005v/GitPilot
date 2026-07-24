# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: gitpilot.spec.js >> GitPilot E2E System Validation User Journey >> should log in, configure settings, view analytics, trigger syncs, and simulate webhooks
- Location: e2e\gitpilot.spec.js:6:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('text=Contribution Percentage Distribution')
Expected: visible
Timeout: 8000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 8000ms
  - waiting for locator('text=Contribution Percentage Distribution')

```

```yaml
- complementary:
  - text: GP GitPilot
  - button "Collapse sidebar":
    - img
  - navigation:
    - link "Dashboard":
      - /url: /dashboard
      - img
      - text: Dashboard
    - link "Analytics":
      - /url: /analytics
      - img
      - text: Analytics
    - link "AI Insights":
      - /url: /insights
      - img
      - text: AI Insights
    - link "Settings":
      - /url: /settings
      - img
      - text: Settings
  - img "Avatar"
  - text: GitPilot Mock User @gitpilot-mock-user
  - button "Log out":
    - img
- banner:
  - heading "Overall Analytics" [level=2]
  - img
  - combobox:
    - option "-- Quick Repo Jump --" [selected]
    - option "mock-spring-api"
    - option "mock-react-dashboard"
  - link "Configure webhooks and repositories":
    - /url: /settings
    - img
- heading "Overall Analytics" [level=1]
- paragraph: Analyze commit frequencies and contribution metrics across selected codebases.
- heading "Commits by Repository" [level=3]:
  - img
  - text: Commits by Repository
- img: mock-spring-api mock-react-dashboard 0 1 2 3 4
- heading "Commit Volume Split" [level=3]:
  - img
  - text: Commit Volume Split
- img
- heading "Repository Activity Statistics" [level=3]
- table:
  - rowgroup:
    - row "Repository Name Total Commits Contributors Health Score Last Synced At Sync Status":
      - columnheader "Repository Name"
      - columnheader "Total Commits"
      - columnheader "Contributors"
      - columnheader "Health Score"
      - columnheader "Last Synced At"
      - columnheader "Sync Status"
  - rowgroup:
    - row "mock-spring-api 0 0 - Never Pending":
      - cell "mock-spring-api":
        - link "mock-spring-api":
          - /url: /repositories/1
      - cell "0"
      - cell "0"
      - cell "-"
      - cell "Never"
      - cell "Pending"
    - row "mock-react-dashboard 0 0 - Never Pending":
      - cell "mock-react-dashboard":
        - link "mock-react-dashboard":
          - /url: /repositories/2
      - cell "0"
      - cell "0"
      - cell "-"
      - cell "Never"
      - cell "Pending"
```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test';
  2   | import crypto from 'crypto';
  3   | 
  4   | test.describe('GitPilot E2E System Validation User Journey', () => {
  5   | 
  6   |   test('should log in, configure settings, view analytics, trigger syncs, and simulate webhooks', async ({ page }) => {
  7   |     // 1. Launch & Navigate to Landing Page
  8   |     console.log('Navigating to landing page...');
  9   |     await page.goto('/');
  10  |     await expect(page).toHaveTitle(/GitPilot/);
  11  | 
  12  |     // Verify visual presence of sign-in options
  13  |     await expect(page.locator('text=Developer Sandbox Sign-In')).toBeVisible();
  14  | 
  15  |     // 2. Perform Mock Developer Login
  16  |     console.log('Clicking developer sandbox login...');
  17  |     await page.click('text=Developer Sandbox Sign-In');
  18  | 
  19  |     // Wait for core application UI to load (check sidebar presence)
  20  |     await expect(page.locator('h1, h2').filter({ hasText: 'Dashboard' }).first()).toBeVisible({ timeout: 12000 });
  21  |     console.log('Successfully authenticated into dashboard!');
  22  | 
  23  |     // 3. Navigate to Repository Settings selection
  24  |     console.log('Opening repository settings selections...');
  25  |     await page.click('a[href="/settings"]');
  26  |     await expect(page.locator('text=Repository Selections')).toBeVisible();
  27  | 
  28  |     // Wait for repositories list to fetch and load
  29  |     await page.waitForSelector('.settings-item');
  30  | 
  31  |     // Locate repository checkboxes and check them
  32  |     console.log('Selecting repositories for tracking...');
  33  |     const checkboxes = page.locator('input[type="checkbox"]');
  34  |     const count = await checkboxes.count();
  35  |     
  36  |     // Select first two repositories
  37  |     if (count > 0) {
  38  |       const firstChecked = await checkboxes.nth(0).isChecked();
  39  |       if (!firstChecked) await checkboxes.nth(0).click();
  40  |     }
  41  |     if (count > 1) {
  42  |       const secondChecked = await checkboxes.nth(1).isChecked();
  43  |       if (!secondChecked) await checkboxes.nth(1).click();
  44  |     }
  45  | 
  46  |     // Save Selection Preferences
  47  |     console.log('Saving selections...');
  48  |     await page.click('text=Save Selected Repositories');
  49  |     
  50  |     // Assert success banner appears
  51  |     await expect(page.locator('text=Repository selection preferences saved successfully.')).toBeVisible();
  52  | 
  53  |     // 4. Return to Dashboard
  54  |     console.log('Returning to Dashboard...');
  55  |     await page.click('a[href="/dashboard"]');
  56  |     await expect(page.locator('text=Repository Health overview')).toBeVisible();
  57  | 
  58  |     // Verify at least one repository card exists
  59  |     await page.waitForSelector('.card h4');
  60  |     const firstRepoCardTitle = await page.locator('.card h4').first().textContent();
  61  |     console.log(`Found tracking repository: ${firstRepoCardTitle}`);
  62  | 
  63  |     // 5. Open Repository Details tab views
  64  |     console.log('Navigating into repository overview details...');
  65  |     await page.locator('.card h4').first().click();
  66  | 
  67  |     // Verify we arrived at repository overview and tabs render
  68  |     await expect(page.locator('text=Back to Dashboard')).toBeVisible();
  69  |     
  70  |     // Test tabs sequentially
  71  |     console.log('Verifying tabs sequentially...');
  72  |     
  73  |     // Overview Tab
  74  |     await page.click('text=Overview');
  75  |     await expect(page.locator('text=Default Branch')).toBeVisible();
  76  | 
  77  |     // Analytics Tab
  78  |     await page.click('text=Analytics');
> 79  |     await expect(page.locator('text=Contribution Percentage Distribution')).toBeVisible();
      |                                                                             ^ Error: expect(locator).toBeVisible() failed
  80  | 
  81  |     // Commits Tab
  82  |     await page.click('text=Commits');
  83  |     await expect(page.locator('text=Repository Commits List')).toBeVisible();
  84  | 
  85  |     // AI Insights Tab
  86  |     await page.click('text=AI Insights');
  87  |     // If AI Gateway keys aren't set, might show failure alert, check either header or error banner is visible
  88  |     await expect(page.locator('text=Repository Health').or(page.locator('text=AI Report unavailable'))).toBeVisible();
  89  | 
  90  |     // Contributors Tab
  91  |     await page.click('text=Contributors');
  92  |     await expect(page.locator('text=Contributor Rankings')).toBeVisible();
  93  | 
  94  |     // Settings Tab
  95  |     await page.click('text=Settings');
  96  |     await expect(page.locator('text=Synchronization Status')).toBeVisible();
  97  | 
  98  |     // 6. Simulate Real-Time GitHub Webhook Event Trigger
  99  |     console.log('Simulating push webhook event trigger via relative POST request...');
  100 |     
  101 |     const webhookPayload = {
  102 |       repository: {
  103 |         id: 11111111,
  104 |         name: 'mock-react-dashboard',
  105 |         html_url: 'https://github.com/mock-user/mock-react-dashboard',
  106 |         default_branch: 'main',
  107 |         private: false
  108 |       },
  109 |       commits: [
  110 |         {
  111 |           id: 'sha_playwright_e2e_test_999',
  112 |           message: 'docs: verify webhooks strategy handler outputs in Playwright',
  113 |           author: {
  114 |             name: 'Playwright Test Runner',
  115 |             email: 'e2e@playwright.com',
  116 |             date: new Date().toISOString()
  117 |           },
  118 |           url: 'https://github.com/mock-user/mock-react-dashboard/commit/sha_playwright_e2e_test_999'
  119 |         }
  120 |       ]
  121 |     };
  122 | 
  123 |     // Calculate valid payload HMAC signature (in case secret is configured in backend environment)
  124 |     const payloadString = JSON.stringify(webhookPayload);
  125 |     const secretKey = process.env.GITHUB_WEBHOOK_SECRET || '';
  126 |     const hmac = crypto.createHmac('sha256', secretKey);
  127 |     hmac.update(payloadString);
  128 |     const expectedSignature = 'sha256=' + hmac.digest('hex');
  129 | 
  130 |     // Trigger webhook POST request using Playwright context
  131 |     const webhookResponse = await page.request.post('/webhooks/github', {
  132 |       headers: {
  133 |         'x-github-event': 'push',
  134 |         'x-hub-signature-256': expectedSignature,
  135 |         'Content-Type': 'application/json'
  136 |       },
  137 |       data: webhookPayload
  138 |     });
  139 | 
  140 |     expect(webhookResponse.status()).toBe(200);
  141 |     const bodyText = await webhookResponse.text();
  142 |     console.log(`Webhook triggered successfully. Response status: ${webhookResponse.status()}, Content: ${bodyText}`);
  143 | 
  144 |     // 7. Verify Dashboard Update after push
  145 |     await page.click('a[href="/dashboard"]');
  146 |     await expect(page.locator('text=Total Commits')).toBeVisible();
  147 | 
  148 |     // 8. Sign Out
  149 |     console.log('Signing out...');
  150 |     await page.click('text=Logout');
  151 |     
  152 |     // Assert redirect back to landing page
  153 |     await expect(page.locator('text=Sign in with GitHub')).toBeVisible();
  154 |     console.log('E2E validation test finished successfully.');
  155 |   });
  156 | });
  157 | 
```