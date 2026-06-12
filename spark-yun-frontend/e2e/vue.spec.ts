import { test, expect } from '@playwright/test'

test('loads the application shell', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle('至轻云')
    await expect(page.locator('#app')).toHaveCount(1)
})
